package com.medassist.ai;

import java.util.List;

/** Prompt templates. Kept in one place so they can be reviewed and versioned like code. */
final class Prompts {

    private Prompts() {
    }

    private static final String RULES = """
            Rules: You assist licensed pharmacists in India. Answer ONLY with JSON matching the schema.
            Use Indian brand names and Indian drug schedules (OTC, G, H, H1, X) where relevant.
            Never invent a value: use null when unsure. Do not give a diagnosis.
            """;

    static final String IDENTIFY = RULES + """
            Task: identify the medicine in the photo (strip, bottle, box or tablet imprint).
            Schema:
            {"name": string|null, "saltComposition": string|null, "strength": string|null,
             "manufacturer": string|null, "dosageForm": string|null, "confidence": number (0-1),
             "prescriptionRequired": boolean|null, "uses": [string], "commonSideEffects": [string],
             "warnings": [string]}
            """;

    static final String PRESCRIPTION = RULES + """
            Task: read this handwritten or printed prescription. Extract each prescribed medicine as written.
            Expand common abbreviations in "frequency" (OD = once daily, BD = twice daily, TDS = three times daily,
            HS = at bedtime, SOS = when needed). Compute "quantity" = units for the full course when the dose,
            frequency and duration are all clear; otherwise null.
            Schema:
            {"doctorName": string|null, "doctorRegNo": string|null, "patientName": string|null,
             "date": string|null, "confidence": number (0-1), "notes": string|null,
             "items": [{"medicine": string, "strength": string|null, "dosage": string|null,
                        "frequency": string|null, "durationDays": integer|null, "quantity": integer|null}]}
            """;

    static String interactions(List<String> medicines) {
        return RULES + """
                Task: check clinically relevant drug-drug interactions between these medicines (brand or salt names):
                %s
                Severity is one of "major", "moderate", "minor". List only real, documented interactions;
                an empty list is a valid answer.
                Schema:
                {"interactions": [{"between": [string, string], "severity": string, "description": string,
                                   "recommendation": string}],
                 "summary": string}
                """.formatted(String.join(", ", medicines));
    }

    static String info(String medicine) {
        return RULES + """
                Task: explain this medicine to a patient in simple English: %s
                Schema:
                {"name": string, "saltComposition": string|null, "usedFor": string, "howToTake": string,
                 "commonSideEffects": [string], "seriousSideEffects": [string], "precautions": [string],
                 "storage": string|null}
                """.formatted(medicine);
    }
}
