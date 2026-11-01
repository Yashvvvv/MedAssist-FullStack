package com.medassist.app.core

import com.medassist.app.core.auth.AuthGateway
import com.medassist.app.core.auth.AuthTokens
import com.medassist.app.core.auth.RefreshResult
import com.medassist.app.core.auth.Session
import com.medassist.app.core.auth.SessionManager
import com.medassist.app.core.auth.TokenStore
import com.medassist.app.core.model.AppJson
import com.medassist.app.core.model.AuthResponse
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionManagerTest {

    private class MemoryStore : TokenStore {
        val flow = MutableStateFlow<AuthTokens?>(null)
        override val tokens = flow
        override suspend fun save(tokens: AuthTokens) { flow.value = tokens }
        override suspend fun clear() { flow.value = null }
    }

    private val response: AuthResponse = AppJson.decodeFromString(Fixtures.read("auth_owner"))

    private class FakeGateway(val result: RefreshResult) : AuthGateway {
        val refreshes = AtomicInteger()
        override suspend fun refresh(refreshToken: String): RefreshResult {
            refreshes.incrementAndGet()
            delay(50) // slow network: concurrent callers pile up behind the mutex
            return result
        }
        override suspend fun logout(refreshToken: String) {}
    }

    @Test
    fun `five concurrent 401s cause exactly one refresh`() = runTest {
        val store = MemoryStore()
        val rotated = response.copy(accessToken = "new-access", refreshToken = "new-refresh")
        val gateway = FakeGateway(RefreshResult.Success(rotated))
        val manager = SessionManager(store, gateway, backgroundScope)
        manager.signIn(response)
        val old = manager.currentTokens()!!.accessToken

        val results = (1..5).map { async { manager.refresh(old) } }.awaitAll()

        assertEquals(1, gateway.refreshes.get())
        assertTrue(results.all { it?.accessToken == "new-access" })
        assertEquals("new-refresh", store.flow.value?.refreshToken)
    }

    @Test
    fun `a rejected refresh token ends the session`() = runTest {
        val store = MemoryStore()
        val manager = SessionManager(store, FakeGateway(RefreshResult.Rejected), backgroundScope)
        manager.signIn(response)
        assertNull(manager.refresh(manager.currentTokens()!!.accessToken))
        assertNull(store.flow.value)
        assertEquals(Session.SignedOut, manager.session.first { it != Session.Unknown && it !is Session.SignedIn })
    }

    @Test
    fun `being offline keeps the session`() = runTest {
        val store = MemoryStore()
        val manager = SessionManager(store, FakeGateway(RefreshResult.Unavailable), backgroundScope)
        manager.signIn(response)
        assertNull(manager.refresh(manager.currentTokens()!!.accessToken))
        assertTrue(store.flow.value != null)
    }

    @Test
    fun `staff session opens the pharmacy app`() = runTest {
        val store = MemoryStore()
        val manager = SessionManager(store, FakeGateway(RefreshResult.Unavailable), backgroundScope)
        manager.signIn(response)
        val s = manager.session.first { it is Session.SignedIn } as Session.SignedIn
        assertTrue(s.isStaff)
    }
}
