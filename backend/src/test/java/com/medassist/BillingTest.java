package com.medassist;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Plans, limits (402 with details) and the Razorpay webhook. */
@TestPropertySource(properties = "app.billing.razorpay.webhook-secret=test-webhook-secret")
class BillingTest extends IntegrationTest {

    @Test
    void newPharmacyStartsOnAProTrialAndCanSeeThePriceList() throws Exception {
        Session owner = registerPharmacy("Trial " + uniq);
        mvc.perform(get("/api/v1/org").with(owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscription.status").value("TRIALING"))
                .andExpect(jsonPath("$.subscription.effectivePlan").value("PRO"))
                .andExpect(jsonPath("$.usage.branches").value(1));
        mvc.perform(get("/api/v1/billing/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void freePlanLimitsBranchesWithAnExplained402() throws Exception {
        Session owner = registerPharmacy("Free " + uniq);
        mvc.perform(jsonPost("/api/v1/billing/plan", "{\"plan\":\"FREE\"}").with(owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscription.effectivePlan").value("FREE"));

        mvc.perform(jsonPost("/api/v1/branches", """
                        {"name":"Second","addressLine":"Sector 62","city":"Noida","state":"UP","pincode":"201309"}
                        """).with(owner))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.code").value("PLAN_LIMIT_REACHED"))
                .andExpect(jsonPath("$.details.limit").value("branches"))
                .andExpect(jsonPath("$.details.max").value(1));

        mvc.perform(jsonPost("/api/v1/billing/plan", "{\"plan\":\"PRO\"}").with(owner)).andExpect(status().isOk());
        mvc.perform(jsonPost("/api/v1/branches", """
                        {"name":"Second","addressLine":"Sector 62","city":"Noida","state":"UP","pincode":"201309"}
                        """).with(owner))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("BR2"));
    }

    @Test
    void razorpayWebhookNeedsAValidSignatureAndIsIdempotent() throws Exception {
        Session owner = registerPharmacy("Rzp " + uniq);
        String body = """
                {"event":"subscription.cancelled","payload":{"subscription":{"entity":
                  {"id":"sub_%s","plan_id":"","current_end":0,"notes":{"org_id":"%s"}}}}}
                """.formatted(uniq, owner.orgId());

        mvc.perform(post("/api/v1/billing/webhooks/razorpay").content(body)
                        .header("X-Razorpay-Signature", "forged").header("X-Razorpay-Event-Id", "evt_" + uniq))
                .andExpect(status().isUnauthorized());

        String sig = hmac(body);
        mvc.perform(post("/api/v1/billing/webhooks/razorpay").content(body)
                        .header("X-Razorpay-Signature", sig).header("X-Razorpay-Event-Id", "evt_" + uniq))
                .andExpect(status().isOk()).andExpect(jsonPath("$.applied").value(true));
        mvc.perform(post("/api/v1/billing/webhooks/razorpay").content(body)
                        .header("X-Razorpay-Signature", sig).header("X-Razorpay-Event-Id", "evt_" + uniq))
                .andExpect(status().isOk()).andExpect(jsonPath("$.applied").value(false));

        String org = json(mvc.perform(get("/api/v1/org").with(owner)).andReturn());
        assertThat((String) JsonPath.read(org, "$.subscription.status"))
                .isEqualTo("CANCELED");
    }

    @Test
    void onlyTheOwnerChangesThePlan() throws Exception {
        Session patient = registerConsumer("Nobody");
        mvc.perform(jsonPost("/api/v1/billing/plan", "{\"plan\":\"BUSINESS\"}").with(patient))
                .andExpect(status().isForbidden());
    }

    private static String hmac(String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("test-webhook-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }
}
