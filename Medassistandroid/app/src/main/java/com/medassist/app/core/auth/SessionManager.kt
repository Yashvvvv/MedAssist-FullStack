package com.medassist.app.core.auth

import com.medassist.app.core.model.AuthResponse
import com.medassist.app.core.model.Role
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface Session {
    /** Storage not read yet: show the splash, neither login nor home. */
    data object Unknown : Session
    data object SignedOut : Session
    data class SignedIn(val tokens: AuthTokens) : Session {
        /** Staff of a pharmacy get the pharmacy app; everyone else the patient app. */
        val isStaff: Boolean get() = tokens.role != null && tokens.orgId != null
        val role: Role? get() = tokens.role
    }
}

/** The outcome of presenting a refresh token to the server. */
sealed interface RefreshResult {
    data class Success(val response: AuthResponse) : RefreshResult
    /** 401: the refresh token is expired, revoked, or was already used. The session is over. */
    data object Rejected : RefreshResult
    /** Offline or the server is down: keep the session, fail only this request. */
    data object Unavailable : RefreshResult
}

/** The network side of sessions, implemented by AuthRepository (kept as an interface so tests can fake it). */
interface AuthGateway {
    suspend fun refresh(refreshToken: String): RefreshResult
    suspend fun logout(refreshToken: String)
}

/**
 * The single owner of the auth session. Every token write goes through [mutex], so sign-in,
 * sign-out and refresh never interleave.
 *
 * Refresh tokens rotate: each works once, and presenting a used one makes the server revoke the
 * whole session family (theft detection). So concurrent 401s must cause exactly one refresh; the
 * mutex plus the "has someone already refreshed?" check guarantee it.
 */
class SessionManager(
    private val store: TokenStore,
    private val gateway: AuthGateway,
    scope: CoroutineScope,
) {
    val session: StateFlow<Session> = store.tokens
        .map { if (it == null) Session.SignedOut else Session.SignedIn(it) }
        .stateIn(scope, SharingStarted.Eagerly, Session.Unknown)

    private val mutex = Mutex()

    suspend fun currentTokens(): AuthTokens? = store.tokens.first()

    suspend fun signIn(response: AuthResponse) = mutex.withLock { store.save(AuthTokens.from(response)) }

    /** Local sign-out always completes; revoking the refresh token on the server is best effort. */
    suspend fun signOut() {
        val refreshToken = mutex.withLock {
            val rt = currentTokens()?.refreshToken
            store.clear()
            rt
        }
        refreshToken?.let { runCatching { gateway.logout(it) } }
    }

    /**
     * Called by the HTTP client after a 401. [failedAccessToken] is the token the server just rejected.
     * Returns fresh tokens, or null when the session is over or the server can't be reached.
     */
    suspend fun refresh(failedAccessToken: String?): AuthTokens? = mutex.withLock {
        val stored = currentTokens() ?: return@withLock null
        if (stored.accessToken != failedAccessToken) return@withLock stored // another request already refreshed
        when (val result = gateway.refresh(stored.refreshToken)) {
            is RefreshResult.Success -> AuthTokens.from(result.response).also { store.save(it) }
            RefreshResult.Rejected -> {
                store.clear()
                null
            }
            RefreshResult.Unavailable -> null
        }
    }
}
