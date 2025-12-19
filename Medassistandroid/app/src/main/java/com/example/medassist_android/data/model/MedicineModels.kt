package com.example.medassist_android.data.model

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Json

@JsonClass(generateAdapter = true)
data class Medicine(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "genericName") val genericName: String,
    @Json(name = "brandNames") val brandNames: List<String>?,
    @Json(name = "description") val description: String?,
    @Json(name = "usageDescription") val usageDescription: String?,
    @Json(name = "dosageInformation") val dosageInformation: String?,
    @Json(name = "sideEffects") val sideEffects: List<String>?,
    @Json(name = "manufacturer") val manufacturer: String,
    @Json(name = "category") val category: String?,
    @Json(name = "form") val form: String?,
    @Json(name = "strength") val strength: String?,
    @Json(name = "activeIngredient") val activeIngredient: String?,
    @Json(name = "activeIngredients") val activeIngredients: List<String>?, // Backend provides both
    @Json(name = "requiresPrescription") val requiresPrescription: Boolean,
    @Json(name = "storageInstructions") val storageInstructions: String?,
    @Json(name = "createdAt") val createdAt: String?,
    @Json(name = "updatedAt") val updatedAt: String?
)

@JsonClass(generateAdapter = true)
data class MedicineSearchRequest(
    @Json(name = "query") val query: String
)

@JsonClass(generateAdapter = true)
data class MedicineAnalysisRequest(
    @Json(name = "query") val query: String
)

// Updated to match backend's MedicineAnalysisResponse (snake_case JSON)
@JsonClass(generateAdapter = true)
data class MedicineAnalysisResponse(
    @Json(name = "medicine_name") val medicineName: String?,
    @Json(name = "generic_name") val genericName: String?,
    @Json(name = "brand_names") val brandNames: List<String>?,
    @Json(name = "active_ingredients") val activeIngredients: List<String>?,
    @Json(name = "strength") val strength: String?,
    @Json(name = "form") val form: String?,
    @Json(name = "manufacturer") val manufacturer: String?,
    @Json(name = "description") val description: String?,
    @Json(name = "usage_instructions") val usageInstructions: String?,
    @Json(name = "dosage_information") val dosageInformation: DosageInformation?,
    @Json(name = "side_effects") val sideEffects: List<String>?,
    @Json(name = "contraindications") val contraindications: List<String>?,
    @Json(name = "drug_interactions") val drugInteractions: List<String>?,
    @Json(name = "warnings") val warnings: List<String>?,
    @Json(name = "storage_instructions") val storageInstructions: String?,
    @Json(name = "requires_prescription") val requiresPrescription: Boolean?,
    @Json(name = "pregnancy_category") val pregnancyCategory: String?,
    @Json(name = "confidence_score") val confidenceScore: Double?,
    @Json(name = "analysis_source") val analysisSource: String?,
    @Json(name = "extracted_text") val extractedText: String?,
    @Json(name = "analysis_timestamp") val analysisTimestamp: String?,
    @Json(name = "emergency_info") val emergencyInfo: EmergencyInformation?
)

@JsonClass(generateAdapter = true)
data class DosageInformation(
    @Json(name = "adult_dosage") val adultDosage: String?,
    @Json(name = "pediatric_dosage") val pediatricDosage: String?,
    @Json(name = "elderly_dosage") val elderlyDosage: String?,
    @Json(name = "frequency") val frequency: String?,
    @Json(name = "duration") val duration: String?,
    @Json(name = "maximum_daily_dose") val maximumDailyDose: String?
)

@JsonClass(generateAdapter = true)
data class EmergencyInformation(
    @Json(name = "overdose_symptoms") val overdoseSymptoms: List<String>?,
    @Json(name = "emergency_actions") val emergencyActions: List<String>?,
    @Json(name = "poison_control_info") val poisonControlInfo: String?
)

@JsonClass(generateAdapter = true)
data class MedicineMatch(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "genericName") val genericName: String,
    @Json(name = "confidence") val confidence: Double,
    @Json(name = "matchReason") val matchReason: String
)

@JsonClass(generateAdapter = true)
data class AiInsights(
    @Json(name = "summary") val summary: String,
    @Json(name = "recommendations") val recommendations: List<String>,
    @Json(name = "warnings") val warnings: List<String>
)

@JsonClass(generateAdapter = true)
data class ImageAnalysis(
    @Json(name = "detectedText") val detectedText: String,
    @Json(name = "confidence") val confidence: Double,
    @Json(name = "imageQuality") val imageQuality: String
)

@JsonClass(generateAdapter = true)
data class CreateMedicineRequest(
    @Json(name = "name") val name: String,
    @Json(name = "genericName") val genericName: String,
    @Json(name = "brandNames") val brandNames: List<String>?,
    @Json(name = "description") val description: String?,
    @Json(name = "usageDescription") val usageDescription: String?,
    @Json(name = "dosageInformation") val dosageInformation: String?,
    @Json(name = "sideEffects") val sideEffects: List<String>?,
    @Json(name = "manufacturer") val manufacturer: String,
    @Json(name = "category") val category: String?,
    @Json(name = "form") val form: String?,
    @Json(name = "strength") val strength: String?,
    @Json(name = "activeIngredient") val activeIngredient: String?,
    @Json(name = "requiresPrescription") val requiresPrescription: Boolean,
    @Json(name = "storageInstructions") val storageInstructions: String?
)
