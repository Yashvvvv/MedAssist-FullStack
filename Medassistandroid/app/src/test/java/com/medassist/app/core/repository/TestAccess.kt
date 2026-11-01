package com.medassist.app.core.repository

/** Exposes the internal apiCall to tests in other packages. */
object TestAccess {
    suspend fun <T> call(block: suspend () -> T): Result<T> = apiCall(block)
}
