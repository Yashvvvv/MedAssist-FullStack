package com.medassist.ai.service;

import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class PromptTemplateService {

    private static final String MEDICINE_ANALYSIS_PROMPT = """
        You are an expert medical AI assistant specializing in medicine identification and analysis.
        Please analyze the following medicine information and provide a comprehensive response in JSON format.
        
        Query: {query}
        
        Please provide detailed information about this medicine including:
        1. Medicine name (brand and generic)
        2. Active ingredients
        3. Strength and form
        4. Manufacturer (if identifiable)
        5. Description and primary uses
        6. Detailed dosage information for different age groups
        7. Side effects (common and serious)
        8. Contraindications
        9. Drug interactions
        10. Warnings and precautions
        11. Storage instructions
        12. Prescription requirements
        13. Pregnancy category
        14. Emergency information (overdose symptoms and actions)
        
        Format your response as a well-structured JSON object with the following structure:
        {{
            "medicine_name": "string",
            "generic_name": "string",
            "brand_names": ["string"],
            "active_ingredients": ["string"],
            "strength": "string",
            "form": "string",
            "manufacturer": "string",
            "description": "string",
            "usage_instructions": "string",
            "dosage_information": {{
                "adult_dosage": "string",
                "pediatric_dosage": "string",
                "elderly_dosage": "string",
                "frequency": "string",
                "duration": "string",
                "maximum_daily_dose": "string"
            }},
            "side_effects": ["string"],
            "contraindications": ["string"],
            "drug_interactions": ["string"],
            "warnings": ["string"],
            "storage_instructions": "string",
            "requires_prescription": boolean,
            "pregnancy_category": "string",
            "confidence_score": number,
            "emergency_info": {{
                "overdose_symptoms": ["string"],
                "emergency_actions": ["string"],
                "poison_control_info": "string"
            }}
        }}
        
        IMPORTANT: 
        - Only provide information you are confident about
        - If you're unsure about specific details, indicate this in the response
        - Include appropriate medical disclaimers
        - Assign a confidence score between 0.0 and 1.0
        - Always recommend consulting healthcare professionals
        """;

    private static final String IMAGE_TEXT_EXTRACTION_PROMPT = """
        You are an expert at extracting text from medicine packaging images.
        Please analyze the provided image of medicine packaging and extract all visible text.
        
        Focus on identifying:
        1. Medicine name (brand name)
        2. Generic name
        3. Active ingredients
        4. Strength/dosage
        5. Manufacturer name
        6. Batch number/lot number
        7. Expiry date
        8. Any other relevant medical information
        
        Extract and return all text you can see in the image, organized by category.
        Format your response as JSON:
        {{
            "extracted_text": "string - all visible text",
            "medicine_name": "string",
            "generic_name": "string", 
            "active_ingredients": ["string"],
            "strength": "string",
            "manufacturer": "string",
            "batch_number": "string",
            "expiry_date": "string",
            "other_info": ["string"],
            "confidence_score": number
        }}
        
        If the image is unclear or you cannot identify medicine-related text, indicate this in your response.
        """;

    private static final String DRUG_INTERACTION_PROMPT = """
        You are a clinical pharmacist AI assistant specializing in drug interactions.
        Please analyze potential interactions for the following medicines:
        
        Primary Medicine: {primary_medicine}
        Other Medicines: {other_medicines}
        
        Provide a comprehensive interaction analysis including:
        1. Severity level of interactions (None, Minor, Moderate, Major, Severe)
        2. Mechanism of interaction
        3. Clinical significance
        4. Recommendations for management
        5. Monitoring requirements
        
        Format your response as JSON:
        {{
            "interactions": [
                {{
                    "interacting_medicines": ["string"],
                    "severity": "string",
                    "mechanism": "string",
                    "clinical_significance": "string",
                    "recommendations": "string",
                    "monitoring": "string"
                }}
            ],
            "overall_risk_level": "string",
            "general_recommendations": "string"
        }}
        """;

    private static final String DOSAGE_CALCULATION_PROMPT = """
        You are a clinical pharmacist AI assistant specializing in dosage calculations.
        Please calculate the appropriate dosage for the following scenario:
        
        Medicine: {medicine}
        Patient Age: {age}
        Patient Weight: {weight}
        Medical Condition: {condition}
        Other Medications: {other_medications}
        
        Provide detailed dosage recommendations including:
        1. Recommended dose
        2. Frequency of administration
        3. Duration of treatment
        4. Route of administration
        5. Special considerations
        6. Monitoring parameters
        
        Format your response as JSON:
        {{
            "recommended_dose": "string",
            "frequency": "string",
            "duration": "string",
            "route": "string",
            "special_considerations": ["string"],
            "monitoring_parameters": ["string"],
            "contraindications": ["string"],
            "adjustments": {{
                "renal_impairment": "string",
                "hepatic_impairment": "string",
                "elderly": "string"
            }}
        }}
        """;

    public String getMedicineAnalysisPrompt(String query) {
        return MEDICINE_ANALYSIS_PROMPT.replace("{query}", query);
    }

    public String getImageTextExtractionPrompt() {
        return IMAGE_TEXT_EXTRACTION_PROMPT;
    }

    public String getDrugInteractionPrompt(String primaryMedicine, String otherMedicines) {
        return DRUG_INTERACTION_PROMPT
            .replace("{primary_medicine}", primaryMedicine)
            .replace("{other_medicines}", otherMedicines);
    }

    public String getDosageCalculationPrompt(Map<String, String> parameters) {
        String prompt = DOSAGE_CALCULATION_PROMPT;
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            prompt = prompt.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return prompt;
    }

    public String getCombinedAnalysisPrompt(String extractedText, String userQuery) {
        return """
            Based on the extracted text from the medicine image and the user's query, 
            provide a comprehensive medicine analysis.
            
            Extracted Text: %s
            
            User Query: %s
            
            %s
            """.formatted(extractedText, userQuery, MEDICINE_ANALYSIS_PROMPT.replace("{query}", userQuery));
    }
}
