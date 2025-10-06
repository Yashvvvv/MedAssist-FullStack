package com.example.medassist_android.data.model

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Json

@JsonClass(generateAdapter = true)
data class Medicine(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "genericName") val genericName: String,
    @Json(name = "brandNames") val brandNames: List<String>,
    @Json(name = "description") val description: String?,
    @Json(name = "usageDescription") val usageDescription: String?,
    @Json(name = "dosageInformation") val dosageInformation: String?,
    @Json(name = "sideEffects") val sideEffects: List<String>,
    @Json(name = "manufacturer") val manufacturer: String,
    @Json(name = "category") val category: String,
    @Json(name = "form") val form: String,
    @Json(name = "strength") val strength: String,
    @Json(name = "activeIngredient") val activeIngredient: String, // Changed from activeIngredients to activeIngredient
    @Json(name = "requiresPrescription") val requiresPrescription: Boolean,
    @Json(name = "storageInstructions") val storageInstructions: String?, // Added missing field from backend
    @Json(name = "createdAt") val createdAt: String,
    @Json(name = "updatedAt") val updatedAt: String
)

@JsonClass(generateAdapter = true)
data class MedicineSearchRequest(
    @Json(name = "query") val query: String
)

@JsonClass(generateAdapter = true)
data class MedicineAnalysisRequest(
    @Json(name = "query") val query: String
)

@JsonClass(generateAdapter = true)
data class MedicineAnalysisResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "analysisType") val analysisType: String,
    @Json(name = "query") val query: String?,
    @Json(name = "medicines") val medicines: List<MedicineMatch>,
    @Json(name = "aiInsights") val aiInsights: AiInsights,
    @Json(name = "timestamp") val timestamp: String,
    @Json(name = "imageAnalysis") val imageAnalysis: ImageAnalysis?
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
    @Json(name = "brandNames") val brandNames: List<String>,
    @Json(name = "description") val description: String?,
    @Json(name = "usageDescription") val usageDescription: String?,
    @Json(name = "dosageInformation") val dosageInformation: String?,
    @Json(name = "sideEffects") val sideEffects: List<String>,
    @Json(name = "manufacturer") val manufacturer: String,
    @Json(name = "category") val category: String,
    @Json(name = "form") val form: String,
    @Json(name = "strength") val strength: String,
    @Json(name = "activeIngredients") val activeIngredients: List<String>,
    @Json(name = "requiresPrescription") val requiresPrescription: Boolean,
    @Json(name = "isActive") val isActive: Boolean = true
)
