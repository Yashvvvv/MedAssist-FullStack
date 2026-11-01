package com.medassist;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Numbers on the reports must match what was actually billed and returned. */
class ReportsTest extends IntegrationTest {

    @Test
    void dashboardSalesGstAndH1RegisterAgreeWithTheInvoices() throws Exception {
        Session owner = registerPharmacy("Reports " + uniq);
        String branch = firstBranchId(owner);
        // Business dates are Indian dates; at 23:00 UTC it is already tomorrow in IST.
        String today = LocalDate.now(java.time.ZoneId.of("Asia/Kolkata")).toString();
        String expiry = LocalDate.now().plusYears(1).toString();
        String otc = createProduct(owner, "Telma 40", "OTC", "105.00", "5");
        String h1 = createProduct(owner, "Azithral 500", "H1", "210.00", "5");
        for (String[] p : new String[][]{{otc, "T1", "60"}, {h1, "A1", "120"}}) {
            mvc.perform(jsonPost("/api/v1/inventory/receive", """
                            {"branchId":"%s","productId":"%s","batchNo":"%s","expiryDate":"%s","quantity":50,"purchasePrice":%s}
                            """.formatted(branch, p[0], p[1], expiry, p[2])).with(owner))
                    .andExpect(status().isCreated());
        }

        // Sale 1: 2 x Telma 40 = 210.00 (taxable 200.00, GST 10.00, cost 120.00)
        String sale1 = json(mvc.perform(jsonPost("/api/v1/sales", """
                        {"branchId":"%s","paymentMode":"CASH","lines":[{"productId":"%s","quantity":2}]}
                        """.formatted(branch, otc)).with(owner)).andExpect(status().isCreated()).andReturn());

        // Sale 2: 1 x Azithral (H1) = 210.00 with prescription
        String rx = json(mvc.perform(multipart("/api/v1/prescriptions")
                        .file(new MockMultipartFile("file", "rx.png", "image/png",
                                new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A}))
                        .file(new MockMultipartFile("meta", "", MediaType.APPLICATION_JSON_VALUE,
                                "{\"doctorName\":\"Dr. Rao\",\"doctorRegNo\":\"KMC-77\"}".getBytes()))
                        .with(owner)).andExpect(status().isCreated()).andReturn());
        mvc.perform(jsonPost("/api/v1/sales", """
                        {"branchId":"%s","paymentMode":"UPI","prescriptionId":"%s",
                         "newCustomer":{"name":"Meera Iyer","phone":"9000000001"},
                         "lines":[{"productId":"%s","quantity":1}]}""".formatted(branch, JsonPath.read(rx, "$.id"), h1))
                        .with(owner))
                .andExpect(status().isCreated());

        // Return 1 Telma: refund 105.00 (taxable 100.00, GST 5.00)
        mvc.perform(jsonPost("/api/v1/sales/" + JsonPath.read(sale1, "$.id") + "/returns", """
                        {"reason":"Wrong strength","lines":[{"saleLineId":"%s","quantity":1}]}
                        """.formatted((String) JsonPath.read(sale1, "$.lines[0].id"))).with(owner))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/v1/reports/dashboard").param("branchId", branch).with(owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoicesToday").value(2))
                .andExpect(jsonPath("$.salesToday").value(420.00))
                .andExpect(jsonPath("$.salesMonthToDate").value(315.00))
                .andExpect(jsonPath("$.last7Days", hasSize(7)));

        mvc.perform(get("/api/v1/reports/sales").param("from", today).param("to", today).with(owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoices").value(2))
                .andExpect(jsonPath("$.grossSales").value(420.00))
                .andExpect(jsonPath("$.returns").value(105.00))
                .andExpect(jsonPath("$.netSales").value(315.00))
                // kept: 1 Telma (100.00 - 60) + 1 Azithral (200.00 - 120) = 120.00
                .andExpect(jsonPath("$.grossProfit").value(120.00))
                .andExpect(jsonPath("$.byPaymentMode", hasSize(2)));

        mvc.perform(get("/api/v1/reports/gst").param("from", today).param("to", today).with(owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.byRate", hasSize(1)))
                .andExpect(jsonPath("$.byRate[0].taxable").value(400.00))
                .andExpect(jsonPath("$.byRate[0].returnedTaxable").value(100.00))
                .andExpect(jsonPath("$.totalNetTaxable").value(300.00))
                .andExpect(jsonPath("$.totalNetTax").value(15.00));

        mvc.perform(get("/api/v1/reports/h1-register").param("from", today).param("to", today).with(owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].patientName").value("Meera Iyer"))
                .andExpect(jsonPath("$[0].doctorRegNo").value("KMC-77"))
                .andExpect(jsonPath("$[0].batchNo").value("A1"));

        String csv = mvc.perform(get("/api/v1/reports/h1-register.csv").param("from", today).param("to", today)
                .with(owner)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(csv).startsWith("Date,Invoice,Patient").contains("Meera Iyer").contains("KMC-77");

        // Another pharmacy's reports are empty: native SQL is filtered by the caller's org.
        Session other = registerPharmacy("Other reports " + uniq);
        mvc.perform(get("/api/v1/reports/sales").param("from", today).param("to", today).with(other))
                .andExpect(jsonPath("$.invoices").value(0));
    }

    @Test
    void csvCellsCannotInjectSpreadsheetFormulas() {
        assertThat(com.medassist.reports.ReportController.cell("=HYPERLINK(\"x\")")).startsWith("\"'=");
        assertThat(com.medassist.reports.ReportController.cell("a,b")).isEqualTo("\"a,b\"");
    }
}
