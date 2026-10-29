package com.medassist;

import com.jayway.jsonpath.JsonPath;
import com.medassist.jobs.ScheduledJobs;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Scheduled jobs (called directly), the in-app inbox and the audit log. */
class JobsAndInboxTest extends IntegrationTest {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    @Autowired
    ScheduledJobs jobs;

    @Autowired
    DataSource dataSource;

    private Session owner;
    private String branch;
    private String product;

    private void stockedPharmacy(LocalDate expiry) throws Exception {
        owner = registerPharmacy("Jobs " + uniq);
        branch = firstBranchId(owner);
        product = createProduct(owner, "Thyronorm 50", "OTC", "170.00", "5");
        mvc.perform(jsonPost("/api/v1/inventory/receive", """
                        {"branchId":"%s","productId":"%s","batchNo":"TH1","expiryDate":"%s","quantity":30,"purchasePrice":110}
                        """.formatted(branch, product, expiry)).with(owner))
                .andExpect(status().isCreated());
    }

    @Test
    void refillReminderGoesOutOncePerCycle() throws Exception {
        stockedPharmacy(LocalDate.now().plusYears(1));
        String sale = json(mvc.perform(jsonPost("/api/v1/sales", """
                        {"branchId":"%s","paymentMode":"CASH","newCustomer":{"name":"Kamla Devi","phone":"9811100001"},
                         "lines":[{"productId":"%s","quantity":1,"refillAfterDays":1}]}""".formatted(branch, product))
                        .with(owner))
                .andExpect(status().isCreated()).andReturn());
        UUID customer = UUID.fromString(JsonPath.read(sale, "$.customerId"));
        LocalDate today = LocalDate.now(IST);

        jobs.sendRefillReminders(today);
        jobs.sendRefillReminders(today);   // a second run the same day must not resend

        List<String> sent = JdbcClient.create(dataSource).sql(
                        "select body from notifications where customer_id = :c and category = 'REFILL_DUE'")
                .param("c", customer).query(String.class).list();
        assertThat(sent).hasSize(1);
        assertThat(sent.getFirst()).contains("Kamla Devi").contains("Thyronorm 50");
    }

    @Test
    void expiryDigestReachesTheOwnersInbox() throws Exception {
        stockedPharmacy(LocalDate.now().plusDays(10));
        jobs.sendExpiryAlerts(LocalDate.now(IST));
        String inbox = json(mvc.perform(get("/api/v1/notifications").with(owner)).andExpect(status().isOk()).andReturn());
        List<String> categories = JsonPath.read(inbox, "$.content[*].category");
        assertThat(categories).contains("EXPIRY_ALERT");
    }

    @Test
    void staffAndPatientsGetOrderNotificationsAndCanMarkThemRead() throws Exception {
        stockedPharmacy(LocalDate.now().plusYears(1));
        Session patient = registerConsumer("Farah");
        String order = json(mvc.perform(jsonPost("/api/v1/me/orders", """
                        {"branchId":"%s","fulfilment":"PICKUP","contactPhone":"9811100002",
                         "lines":[{"productId":"%s","quantity":1}]}""".formatted(branch, product)).with(patient))
                .andExpect(status().isCreated()).andReturn());

        List<String> staffInbox = JsonPath.read(json(mvc.perform(get("/api/v1/notifications").with(owner)).andReturn()),
                "$.content[*].category");
        assertThat(staffInbox).contains("NEW_ORDER");

        mvc.perform(post("/api/v1/orders/" + JsonPath.read(order, "$.id") + "/accept").with(owner))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/notifications/unread-count").with(patient))
                .andExpect(jsonPath("$.unread").value(1));
        mvc.perform(get("/api/v1/notifications").with(patient))
                .andExpect(jsonPath("$.content[0].category").value("ORDER_UPDATE"));
        mvc.perform(post("/api/v1/notifications/read-all").with(patient)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/notifications/unread-count").with(patient))
                .andExpect(jsonPath("$.unread").value(0));

        mvc.perform(jsonPost("/api/v1/devices", "{\"token\":\"fcm-" + uniq + "\",\"platform\":\"ANDROID\"}")
                .with(patient)).andExpect(status().isNoContent());
    }

    @Test
    void auditLogRecordsWhatStaffDid() throws Exception {
        stockedPharmacy(LocalDate.now().plusYears(1));
        String audit = json(mvc.perform(get("/api/v1/audit").with(owner)).andExpect(status().isOk()).andReturn());
        List<String> actions = JsonPath.read(audit, "$.content[*].action");
        assertThat(actions).contains("ORG_CREATED", "PRODUCT_CREATED");
        Session other = registerPharmacy("Audit other " + uniq);
        List<String> otherActions = JsonPath.read(json(mvc.perform(get("/api/v1/audit").with(other)).andReturn()),
                "$.content[*].action");
        assertThat(otherActions).doesNotContain("PRODUCT_CREATED");
    }
}
