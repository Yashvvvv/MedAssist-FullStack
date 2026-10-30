package com.medassist.app.core.model

import kotlinx.serialization.Serializable

@Serializable
data class MedicineIdentification(
    val name: String? = null,
    val saltComposition: String? = null,
    val strength: String? = null,
    val manufacturer: String? = null,
    val dosageForm: String? = null,
    val confidence: Double? = null,
    val prescriptionRequired: Boolean? = null,
    val uses: List<String> = emptyList(),
    val commonSideEffects: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
)

@Serializable
data class IdentifyResult(
    val identification: MedicineIdentification,
    val catalogMatches: List<MedicineView> = emptyList(),
    val disclaimer: String? = null,
)

@Serializable
data class InteractionRequest(val medicines: List<String>)

@Serializable
data class Interaction(
    val between: List<String> = emptyList(),
    val severity: String? = null,
    val description: String? = null,
    val recommendation: String? = null,
)

@Serializable
data class InteractionResult(val interactions: List<Interaction> = emptyList(), val summary: String? = null, val disclaimer: String? = null)
