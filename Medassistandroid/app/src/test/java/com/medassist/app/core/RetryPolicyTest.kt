package com.medassist.app.core

import com.medassist.app.core.network.IDEMPOTENCY_KEY_HEADER
import com.medassist.app.core.network.MedAssistHttpClient
import com.medassist.app.core.repository.TestAccess
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Only requests that are safe to repeat are retried: a plain POST must never bill twice. */
class RetryPolicyTest {

    private fun counting(calls: AtomicInteger) = MedAssistHttpClient.create(
        "https://api.test/api/v1", false, retryBaseDelayMs = 0,
        engine = MockEngine { calls.incrementAndGet(); respond("", HttpStatusCode.ServiceUnavailable) },
    )

    @Test
    fun `GET is retried on 503`() = runTest {
        val calls = AtomicInteger()
        TestAccess.call { counting(calls).get("x") }
        assertEquals(3, calls.get())
    }

    @Test
    fun `plain POST is not retried`() = runTest {
        val calls = AtomicInteger()
        TestAccess.call { counting(calls).post("sales") }
        assertEquals(1, calls.get())
    }

    @Test
    fun `POST with an idempotency key is retried`() = runTest {
        val calls = AtomicInteger()
        TestAccess.call { counting(calls).post("sales") { header(IDEMPOTENCY_KEY_HEADER, "k1") } }
        assertEquals(3, calls.get())
    }

    @Test
    fun `requests to another host are blocked before sending`() = runTest {
        val calls = AtomicInteger()
        val result = TestAccess.call { counting(calls).get("https://evil.example/steal") }
        assertTrue(result.isFailure)
        assertEquals(0, calls.get())
    }
}
