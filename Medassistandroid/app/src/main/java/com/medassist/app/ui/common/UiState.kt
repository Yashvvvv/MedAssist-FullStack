package com.medassist.app.ui.common

import com.medassist.app.core.model.ApiError

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val error: ApiError) : UiState<Nothing>
}

/** The text a person sees for each error. One place, so wording stays consistent. */
fun ApiError.message(): String = when (this) {
    ApiError.Network -> "No connection. Check your internet and try again."
    ApiError.Unauthorized -> "Your session ended. Please log in again."
    ApiError.Forbidden -> "Your role doesn't allow this. Ask the pharmacy owner."
    ApiError.NotFound -> "That doesn't exist any more."
    ApiError.RateLimited -> "Too many attempts. Wait a minute and try again."
    is ApiError.Validation -> fields.values.firstOrNull()?.let { "Please check: $it" } ?: "Please check the form."
    is ApiError.PlanLimit -> message
    is ApiError.Business -> message
    ApiError.InvalidResponse -> "Something went wrong reading the server's answer."
    ApiError.Server -> "The server had a problem. Try again in a moment."
    is ApiError.Unknown -> "Something went wrong. Try again."
}
