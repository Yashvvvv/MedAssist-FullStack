package com.example.medassist_android.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiErrorResponse(
    val success: Boolean,
    val error: ErrorDetails
) {
    @JsonClass(generateAdapter = true)
    data class ErrorDetails(
        val code: String,
        val timestamp: Long,
        val message: String,
        val details: Map<String, String>? = null
    )

    // Helper methods to extract validation errors
    fun getValidationErrors(): Map<String, String> {
        return error.details ?: emptyMap()
    }

    fun getFormattedErrorMessage(): String {
        val baseMessage = error.message
        val validationErrors = getValidationErrors()

        return if (validationErrors.isNotEmpty()) {
            val detailsText = validationErrors.entries.joinToString("; ") { (field, message) ->
                "$field: $message"
            }
            "$baseMessage - $detailsText"
        } else {
            baseMessage
        }
    }
}
