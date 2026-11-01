package com.medassist.app.core.model

/** The app's error vocabulary. Screens switch on this, never on HTTP codes or exceptions. */
sealed interface ApiError {
    /** No connection, DNS failure, timeout. */
    data object Network : ApiError
    /** The session ended (refresh token expired or revoked). */
    data object Unauthorized : ApiError
    /** Signed in, but the role is too low for this action. */
    data object Forbidden : ApiError
    data object NotFound : ApiError
    data object RateLimited : ApiError
    /** 400 VALIDATION_FAILED: field name to the server's message. */
    data class Validation(val fields: Map<String, String>) : ApiError
    /** 402: the pharmacy's plan doesn't allow this (limit reached or feature not included). */
    data class PlanLimit(val message: String) : ApiError
    /**
     * A business rule refused the request (INSUFFICIENT_STOCK, PRESCRIPTION_REQUIRED, EMAIL_TAKEN ...).
     * The server writes these messages for people ("Only 2 unit(s) of Dolo 650 in stock"), so the app shows them.
     */
    data class Business(val code: String, val message: String) : ApiError
    /** The server broke the contract: unparseable JSON or missing fields. */
    data object InvalidResponse : ApiError
    data object Server : ApiError
    data class Unknown(val message: String?) : ApiError
}

class ApiException(val error: ApiError) : Exception(error.toString())

fun Throwable.toApiError(): ApiError = (this as? ApiException)?.error ?: ApiError.Unknown(message)

/** RFC 9457 problem details as the backend sends them. */
@kotlinx.serialization.Serializable
data class ProblemDto(
    val status: Int? = null,
    val detail: String? = null,
    val code: String? = null,
    val errors: Map<String, String>? = null,
)
