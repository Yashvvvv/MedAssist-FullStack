package com.medassist;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** AI endpoints against the stub AiClient (TestAiConfig): matching, plan gating, quotas, caching. */
class AiTest extends IntegrationTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};

    @Test
    void identifyMatchesThePhotoToTheMedicineMaster() throws Exception {
        Session patient = registerConsumer("Photo");
        mvc.perform(multipart("/api/v1/ai/identify").file(new MockMultipartFile("image", "pack.png", "image/png", PNG))
                        .with(patient))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identification.name").value("Dolo 650"))
                .andExpect(jsonPath("$.catalogMatches[0].name").value("Dolo 650"));
    }

    @Test
    void prescriptionReaderSuggestsBillLinesFromStockOnPro() throws Exception {
        Session owner = registerPharmacy("Rx AI " + uniq);
        String branch = firstBranchId(owner);
        String medicineId = JsonPath.read(json(mvc.perform(get("/api/v1/public/medicines").param("q", "Dolo 650"))
                .andReturn()), "$.content[0].id");
        String product = JsonPath.read(json(mvc.perform(jsonPost("/api/v1/products/import",
                "{\"medicineIds\":[\"" + medicineId + "\"]}").with(owner)).andReturn()), "$.products[0].id");
        mvc.perform(jsonPost("/api/v1/inventory/receive", """
                        {"branchId":"%s","productId":"%s","batchNo":"R1","expiryDate":"%s","quantity":12,"purchasePrice":20}
                        """.formatted(branch, product, LocalDate.now().plusYears(1))).with(owner))
                .andExpect(status().isCreated());
        String rx = JsonPath.read(json(mvc.perform(multipart("/api/v1/prescriptions")
                .file(new MockMultipartFile("file", "rx.png", "image/png", PNG))
                .file(new MockMultipartFile("meta", "", MediaType.APPLICATION_JSON_VALUE, "{}".getBytes()))
                .with(owner)).andExpect(status().isCreated()).andReturn()), "$.id");

        mvc.perform(post("/api/v1/ai/prescriptions/" + rx + "/read").param("branchId", branch).with(owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.extraction.doctorRegNo").value("DMC-1"))
                .andExpect(jsonPath("$.suggestedLines[0].productId").value(product))
                .andExpect(jsonPath("$.suggestedLines[0].inStock").value(12))
                .andExpect(jsonPath("$.disclaimer").isNotEmpty());

        // The reader is a PRO feature: on FREE the same call is refused with an upgrade hint.
        mvc.perform(jsonPost("/api/v1/billing/plan", "{\"plan\":\"FREE\"}").with(owner)).andExpect(status().isOk());
        mvc.perform(post("/api/v1/ai/prescriptions/" + rx + "/read").param("branchId", branch).with(owner))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.code").value("PLAN_FEATURE_UNAVAILABLE"));
    }

    @Test
    void patientsGetTwentyAiCallsADayAndRepeatedQuestionsAreFree() throws Exception {
        Session patient = registerConsumer("Curious");
        for (int i = 0; i < 3; i++) {   // same question: answered from cache, charged once
            mvc.perform(jsonPost("/api/v1/ai/interactions", "{\"medicines\":[\"Aspirin\",\"Warfarin " + uniq + "\"]}")
                            .with(patient))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.disclaimer").isNotEmpty());
        }
        for (int i = 1; i <= 19; i++) {
            mvc.perform(jsonPost("/api/v1/ai/medicine-info", "{\"medicine\":\"Med " + uniq + " " + i + "\"}")
                    .with(patient)).andExpect(status().isOk());
        }
        mvc.perform(jsonPost("/api/v1/ai/medicine-info", "{\"medicine\":\"One too many " + uniq + "\"}").with(patient))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("AI_DAILY_LIMIT"));
    }

    @Test
    void staffAiCallsCountAgainstThePlanQuota() throws Exception {
        Session owner = registerPharmacy("Quota " + uniq);
        mvc.perform(jsonPost("/api/v1/billing/plan", "{\"plan\":\"FREE\"}").with(owner)).andExpect(status().isOk());
        for (int i = 1; i <= 25; i++) {   // FREE: 25 a month
            mvc.perform(jsonPost("/api/v1/ai/medicine-info", "{\"medicine\":\"Staff " + uniq + " " + i + "\"}")
                    .with(owner)).andExpect(status().isOk());
        }
        String body = json(mvc.perform(jsonPost("/api/v1/ai/medicine-info", "{\"medicine\":\"Over " + uniq + "\"}")
                .with(owner)).andExpect(status().isPaymentRequired()).andReturn());
        assertThat((String) JsonPath.read(body, "$.code")).isEqualTo("PLAN_LIMIT_REACHED");
    }
}
