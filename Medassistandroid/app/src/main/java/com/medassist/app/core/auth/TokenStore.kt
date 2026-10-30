package com.medassist.app.core.auth

import kotlinx.coroutines.flow.Flow

/** Where the session lives. Keystore-encrypted DataStore on the phone, in-memory in unit tests. */
interface TokenStore {
    /** Current tokens, or null when signed out or when stored data can't be decrypted. */
    val tokens: Flow<AuthTokens?>
    suspend fun save(tokens: AuthTokens)
    suspend fun clear()
}
