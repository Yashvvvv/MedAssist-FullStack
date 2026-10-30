package com.medassist.app.core.repository

import com.medassist.app.core.model.ApiError
import com.medassist.app.core.model.ApiException
import com.medassist.app.core.model.ProblemDto
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.serialization.ContentConvertException
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.SerializationException

/**
 * Runs one API call: success, or a failure carrying a typed [ApiError]. Only our own coroutine
 * cancellation escapes. Every repository function goes through here.
 */
internal suspend fun <T> apiCall(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    // Ours (screen closed, newer search): stop silently. A cancellation handed over by a shared step
    // (the token load) is reported as a network failure, so the screen shows Retry instead of spinning.
    currentCoroutineContext().ensureActive()
    Result.failure(ApiException(ApiError.Network))
} catch (e: ApiException) {
    Result.failure(e)
} catch (e: ResponseException) {
    Result.failure(ApiException(e.toApiError()))
} catch (e: IOException) {
    Result.failure(ApiException(ApiError.Network))
} catch (e: ContentConvertException) {
    Result.failure(ApiException(ApiError.InvalidResponse))
} catch (e: SerializationException) {
    Result.failure(ApiException(ApiError.InvalidResponse))
} catch (e: Exception) {
    Result.failure(ApiException(ApiError.Unknown(e.message)))
}

/** The `code` field is the contract; the HTTP status is the fallback for codes this app version doesn't know. */
internal suspend fun ResponseException.toApiError(): ApiError {
    val status = response.status.value
    val body = runCatching { response.body<ProblemDto>() }.getOrNull()
    val code = body?.code
    return when {
        code == "VALIDATION_FAILED" -> ApiError.Validation(body.errors.orEmpty())
        status == 402 -> ApiError.PlanLimit(body?.detail ?: "Your plan doesn't include this. Upgrade to continue.")
        status == 401 -> ApiError.Unauthorized
        status == 403 && (code == null || code == "FORBIDDEN") -> ApiError.Forbidden
        status == 404 && (code == null || code == "NOT_FOUND") -> ApiError.NotFound
        status == 429 -> ApiError.RateLimited
        status >= 500 -> ApiError.Server
        code != null && body.detail != null -> ApiError.Business(code, body.detail)
        status == 404 -> ApiError.NotFound
        status == 403 -> ApiError.Forbidden
        else -> ApiError.Unknown(body?.detail)
    }
}
