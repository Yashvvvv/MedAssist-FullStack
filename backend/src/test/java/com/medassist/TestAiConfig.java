package com.medassist;

import com.medassist.ai.AiClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.medassist.ai.AiDtos.DISCLAIMER;
import static com.medassist.ai.AiDtos.IdentifyResult;
import static com.medassist.ai.AiDtos.InteractionResult;
import static com.medassist.ai.AiDtos.MedicineIdentification;
import static com.medassist.ai.AiDtos.MedicineInfo;
import static com.medassist.ai.AiDtos.PrescriptionExtraction;
import static com.medassist.ai.AiDtos.PrescriptionReadResult;
import static com.medassist.ai.AiDtos.ProductMatch;
import static com.medassist.ai.AiDtos.RxItem;
import static com.medassist.ai.AiDtos.SuggestedLine;

/**
 * Stub AI for tests: returns deterministic, valid JSON without calling any API.
 */
@TestConfiguration
@Profile("test")
public class TestAiConfig {

    @Bean
    @Primary
    AiClient fakeAiClient() {
        return new AiClient() {
            @Override
            public String generateJson(String prompt, List<Image> images) {
                if (prompt.contains("identify")) {
                    return """
                            {"name":"Dolo 650","saltComposition":"Paracetamol (650mg)","strength":"650 mg",
                             "manufacturer":"Micro Labs","dosageForm":"Tablet","confidence":0.97,
                             "prescriptionRequired":false,"uses":["Fever","Pain"],"commonSideEffects":["Nausea"],
                             "warnings":["Do not exceed 4g/day"]}
                            """;
                }
                if (prompt.contains("prescription")) {
                    return """
                            {"doctorName":"Dr. Test","doctorRegNo":"DMC-1","patientName":"Patient","date":null,
                             "confidence":0.9,"notes":null,
                             "items":[{"medicine":"Dolo 650","strength":"650 mg","dosage":"1 tab","frequency":"BD",
                                       "durationDays":3,"quantity":6}]}
                            """;
                }
                if (prompt.contains("interactions")) {
                    return """
                            {"interactions":[],"summary":"No known interactions between the given medicines."}
                            """;
                }
                if (prompt.contains("explain")) {
                    return """
                            {"name":"Test Medicine","saltComposition":"Test Salt","usedFor":"Testing","howToTake":"As directed",
                             "commonSideEffects":["Mild"],"seriousSideEffects":[],"precautions":["None"],"storage":null}
                            """;
                }
                return "{}";
            }

            @Override
            public boolean isConfigured() {
                return true;
            }
        };
    }
}