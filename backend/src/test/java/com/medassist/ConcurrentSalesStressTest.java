package com.medassist;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.RepeatedTest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The scaled-up version of SalesFlowTest's four-thread race: 200 cashiers each sell one unit
 * of a product with 50 in stock across two batches, all at once. Exactly 50 must succeed.
 */
class ConcurrentSalesStressTest extends IntegrationTest {

    private static final int CASHIERS = 200;
    private static final int STOCK = 50;

    @RepeatedTest(5)
    void twoHundredConcurrentSalesOfFiftyUnitsSellExactlyFifty() throws Exception {
        Session owner = registerPharmacy("Stress " + uniq);
        String branch = firstBranchId(owner);
        String supplier = JsonPath.read(json(mvc.perform(jsonPost("/api/v1/suppliers", """
                {"name":"Stress Distributors","phone":"9811111111"}""").with(owner))
                .andExpect(status().isCreated()).andReturn()), "$.id");
        String product = createProduct(owner, "Pan 40", "OTC", "15.00", "5");
        mvc.perform(jsonPost("/api/v1/purchases", """
                        {"branchId":"%s","supplierId":"%s","invoiceNo":"ST-%s","invoiceDate":"%s","lines":[
                          {"productId":"%s","batchNo":"P1","expiryDate":"%s","quantity":30,"freeQuantity":0,"purchasePrice":9.00},
                          {"productId":"%s","batchNo":"P2","expiryDate":"%s","quantity":20,"freeQuantity":0,"purchasePrice":9.00}]}
                        """.formatted(branch, supplier, uniq, LocalDate.now().minusDays(1),
                        product, LocalDate.now().plusYears(1), product, LocalDate.now().plusYears(2))).with(owner))
                .andExpect(status().isCreated());

        String sale = """
                {"branchId":"%s","paymentMode":"CASH","lines":[{"productId":"%s","quantity":1}]}
                """.formatted(branch, product);
        List<Callable<Integer>> calls = new ArrayList<>();
        for (int i = 0; i < CASHIERS; i++) {
            calls.add(() -> mvc.perform(jsonPost("/api/v1/sales", sale).with(owner)).andReturn().getResponse().getStatus());
        }
        ExecutorService pool = Executors.newFixedThreadPool(CASHIERS);
        long start = System.nanoTime();
        List<Integer> statuses = new ArrayList<>();
        for (Future<Integer> f : pool.invokeAll(calls)) {
            statuses.add(f.get());
        }
        long ms = (System.nanoTime() - start) / 1_000_000;
        pool.shutdown();

        long sold = statuses.stream().filter(s -> s == 201).count();
        long refused = statuses.stream().filter(s -> s == 409).count();
        System.out.printf("STRESS sales: %d attempts, %d sold, %d refused, %d other, %d ms%n",
                CASHIERS, sold, refused, CASHIERS - sold - refused, ms);

        assertThat(sold).isEqualTo(STOCK);
        assertThat(refused).isEqualTo(CASHIERS - STOCK);
        mvc.perform(get("/api/v1/inventory/levels").param("branchId", branch).with(owner))
                .andExpect(jsonPath("$.content[0].quantity").value(0));
    }
}
