package com.medassist;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Purchase -> stock -> sale -> return, end to end over HTTP against Postgres. */
class SalesFlowTest extends IntegrationTest {

    private Session owner;
    private String branch;
    private String supplier;

    private void setUpPharmacy() throws Exception {
        owner = registerPharmacy("Sales " + uniq);
        branch = firstBranchId(owner);
        String json = json(mvc.perform(jsonPost("/api/v1/suppliers", """
                {"name":"Noida Pharma Distributors","phone":"9811111111"}""").with(owner))
                .andExpect(status().isCreated()).andReturn());
        supplier = JsonPath.read(json, "$.id");
    }

    private void purchase(String product, String invoiceNo, String... batches) throws Exception {
        StringBuilder lines = new StringBuilder();
        for (String b : batches) {
            String[] p = b.split(":");   // batchNo:expiry:qty:price
            if (!lines.isEmpty()) {
                lines.append(',');
            }
            lines.append("""
                    {"productId":"%s","batchNo":"%s","expiryDate":"%s","quantity":%s,"freeQuantity":0,"purchasePrice":%s}
                    """.formatted(product, p[0], p[1], p[2], p[3]));
        }
        mvc.perform(jsonPost("/api/v1/purchases", """
                        {"branchId":"%s","supplierId":"%s","invoiceNo":"%s","invoiceDate":"%s","lines":[%s]}
                        """.formatted(branch, supplier, invoiceNo, LocalDate.now().minusDays(1), lines)).with(owner))
                .andExpect(status().isCreated());
    }

    private String saleBody(String product, int qty) {
        return """
                {"branchId":"%s","paymentMode":"UPI","lines":[{"productId":"%s","quantity":%d}]}
                """.formatted(branch, product, qty);
    }

    @Test
    void sellsFirstExpiryFirstWithCorrectGst() throws Exception {
        setUpPharmacy();
        String dolo = createProduct(owner, "Dolo 650", "OTC", "30.00", "5");
        String far = LocalDate.now().plusYears(2).toString();
        String near = LocalDate.now().plusMonths(4).toString();
        purchase(dolo, "INV-1", "FAR01:" + far + ":20:22.00", "NEAR01:" + near + ":10:22.00");

        String json = json(mvc.perform(jsonPost("/api/v1/sales", saleBody(dolo, 15)).with(owner))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoiceNo", matchesPattern("MAIN/\\d{4}/0001")))
                .andExpect(jsonPath("$.total").value(495.00))
                .andExpect(jsonPath("$.subtotal").value(471.43))
                .andExpect(jsonPath("$.taxTotal").value(23.57))
                .andExpect(jsonPath("$.lines", hasSize(2)))
                // FEFO: the 10 units expiring sooner go first, then 5 from the later batch
                .andExpect(jsonPath("$.lines[0].batchNo").value("NEAR01"))
                .andExpect(jsonPath("$.lines[0].quantity").value(10))
                .andExpect(jsonPath("$.lines[1].batchNo").value("FAR01"))
                .andExpect(jsonPath("$.lines[1].quantity").value(5))
                .andReturn());
        assertThat((String) JsonPath.read(json, "$.id")).isNotBlank();

        mvc.perform(get("/api/v1/inventory/levels").param("branchId", branch).with(owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].quantity").value(15));
    }

    @Test
    void idempotencyKeyPreventsDoubleBilling() throws Exception {
        setUpPharmacy();
        String product = createProduct(owner, "Pan 40", "OTC", "155.00", "5");
        purchase(product, "INV-2", "P1:" + LocalDate.now().plusYears(1) + ":10:100.00");
        String key = UUID.randomUUID().toString();

        String first = json(mvc.perform(jsonPost("/api/v1/sales", saleBody(product, 3)).header("Idempotency-Key", key)
                .with(owner)).andExpect(status().isCreated()).andReturn());
        String second = json(mvc.perform(jsonPost("/api/v1/sales", saleBody(product, 3)).header("Idempotency-Key", key)
                .with(owner)).andExpect(status().isCreated()).andReturn());

        assertThat((String) JsonPath.read(second, "$.id")).isEqualTo(JsonPath.read(first, "$.id"));
        mvc.perform(get("/api/v1/inventory/levels").param("branchId", branch).with(owner))
                .andExpect(jsonPath("$.content[0].quantity").value(7));
    }

    @Test
    void refusesToOversell() throws Exception {
        setUpPharmacy();
        String product = createProduct(owner, "Cetzine", "OTC", "20.00", "5");
        purchase(product, "INV-3", "C1:" + LocalDate.now().plusYears(1) + ":2:10.00");

        mvc.perform(jsonPost("/api/v1/sales", saleBody(product, 3)).with(owner))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"))
                .andExpect(jsonPath("$.details.available").value(2));
    }

    @Test
    void concurrentSalesOfTheLastUnitsNeverGoNegative() throws Exception {
        setUpPharmacy();
        String product = createProduct(owner, "Azithral 500", "OTC", "120.00", "5");
        purchase(product, "INV-4", "AZ1:" + LocalDate.now().plusYears(1) + ":5:80.00");

        ExecutorService pool = Executors.newFixedThreadPool(4);
        List<Callable<Integer>> calls = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            calls.add(() -> mvc.perform(jsonPost("/api/v1/sales", saleBody(product, 2)).with(owner))
                    .andReturn().getResponse().getStatus());
        }
        List<Integer> statuses = new ArrayList<>();
        for (Future<Integer> f : pool.invokeAll(calls)) {
            statuses.add(f.get());
        }
        pool.shutdown();

        assertThat(statuses).filteredOn(s -> s == 201).hasSize(2);   // 2 + 2 = 4 of 5 units
        assertThat(statuses).filteredOn(s -> s == 409).hasSize(2);
        mvc.perform(get("/api/v1/inventory/levels").param("branchId", branch).with(owner))
                .andExpect(jsonPath("$.content[0].quantity").value(1));
    }

    @Test
    void scheduleHNeedsAPrescriptionAndACustomer() throws Exception {
        setUpPharmacy();
        String augmentin = createProduct(owner, "Augmentin 625", "H1", "224.00", "5");
        purchase(augmentin, "INV-5", "AG1:" + LocalDate.now().plusYears(1) + ":10:150.00");

        mvc.perform(jsonPost("/api/v1/sales", saleBody(augmentin, 1)).with(owner))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("PRESCRIPTION_REQUIRED"));

        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
        String rx = json(mvc.perform(multipart("/api/v1/prescriptions")
                        .file(new MockMultipartFile("file", "rx.png", "image/png", png))
                        .file(new MockMultipartFile("meta", "", MediaType.APPLICATION_JSON_VALUE,
                                "{\"doctorName\":\"Dr. Mehta\",\"doctorRegNo\":\"DMC-1234\"}".getBytes()))
                        .with(owner))
                .andExpect(status().isCreated()).andReturn());
        String rxId = JsonPath.read(rx, "$.id");

        mvc.perform(jsonPost("/api/v1/sales", """
                        {"branchId":"%s","paymentMode":"CASH","prescriptionId":"%s",
                         "newCustomer":{"name":"Ramesh Kumar","phone":"+91 98100 12345"},
                         "lines":[{"productId":"%s","quantity":2,"refillAfterDays":30}]}
                        """.formatted(branch, rxId, augmentin)).with(owner))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerName").value("Ramesh Kumar"))
                .andExpect(jsonPath("$.lines[0].schedule").value("H1"));

        mvc.perform(get("/api/v1/prescriptions/" + rxId).with(owner))
                .andExpect(jsonPath("$.status").value("DISPENSED"));
        mvc.perform(get("/api/v1/refills/due").param("withinDays", "31").with(owner))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].customerPhone").value("9810012345"));
    }

    @Test
    void returnsRestockTheSameBatchAndIssueACreditNote() throws Exception {
        setUpPharmacy();
        String product = createProduct(owner, "Telma 40", "OTC", "250.00", "5");
        purchase(product, "INV-6", "T1:" + LocalDate.now().plusYears(1) + ":10:180.00");
        String sale = json(mvc.perform(jsonPost("/api/v1/sales", saleBody(product, 4)).with(owner))
                .andExpect(status().isCreated()).andReturn());
        String saleId = JsonPath.read(sale, "$.id");
        String lineId = JsonPath.read(sale, "$.lines[0].id");

        mvc.perform(jsonPost("/api/v1/sales/" + saleId + "/returns", """
                        {"reason":"Doctor changed the prescription","lines":[{"saleLineId":"%s","quantity":1}]}
                        """.formatted(lineId)).with(owner))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.creditNoteNo", matchesPattern("CMAIN/\\d{4}/0001")))
                .andExpect(jsonPath("$.refundTotal").value(250.00));

        mvc.perform(jsonPost("/api/v1/sales/" + saleId + "/returns", """
                        {"reason":"again","lines":[{"saleLineId":"%s","quantity":4}]}
                        """.formatted(lineId)).with(owner))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RETURN_EXCEEDS_SOLD"));

        mvc.perform(get("/api/v1/sales/" + saleId).with(owner))
                .andExpect(jsonPath("$.status").value("PARTIALLY_RETURNED"));
        mvc.perform(get("/api/v1/inventory/levels").param("branchId", branch).with(owner))
                .andExpect(jsonPath("$.content[0].quantity").value(7));
    }

    @Test
    void duplicateSupplierInvoiceIsRejected() throws Exception {
        setUpPharmacy();
        String product = createProduct(owner, "Shelcal 500", "OTC", "120.00", "5");
        purchase(product, "DUP-1", "S1:" + LocalDate.now().plusYears(1) + ":10:80.00");
        mvc.perform(jsonPost("/api/v1/purchases", """
                        {"branchId":"%s","supplierId":"%s","invoiceNo":"DUP-1","invoiceDate":"%s",
                         "lines":[{"productId":"%s","batchNo":"S2","expiryDate":"%s","quantity":1,"purchasePrice":80}]}
                        """.formatted(branch, supplier, LocalDate.now(), product, LocalDate.now().plusYears(1)))
                        .with(owner))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_INVOICE"));
    }
}
