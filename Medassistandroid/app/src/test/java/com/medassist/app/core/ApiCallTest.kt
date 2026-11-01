package com.medassist.app.core

import com.medassist.app.core.model.ApiError
import com.medassist.app.core.model.toApiError
import com.medassist.app.core.network.MedAssistHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Server errors (real bodies) become the right ApiError, so screens show the right message. */
class ApiCallTest {

    private fun clientAnswering(status: Int, body: String, contentType: String = "application/problem+json") =
        MedAssistHttpClient.create(
            "https://api.test/api/v1",
            enableLogging = false,
            retryBaseDelayMs = 0,
            engine = MockEngine { respond(body, HttpStatusCode.fromValue(status), headersOf(HttpHeaders.ContentType, contentType)) },
        )

    private suspend fun errorFor(status: Int, fixture: String): ApiError {
        val client = clientAnswering(status, Fixtures.read(fixture))
        return com.medassist.app.core.repository.TestAccess.call { client.get("x") }.exceptionOrNull()!!.toApiError()
    }

    @Test
    fun `wrong password is Unauthorized`() = runTest {
        assertEquals(ApiError.Unauthorized, errorFor(401, "error_bad_login"))
    }

    @Test
    fun `business rules keep the server's code and message`() = runTest {
        val rx = errorFor(422, "error_rx_required")
        assertTrue(rx is ApiError.Business && rx.code == "PRESCRIPTION_REQUIRED")
        val stock = errorFor(409, "error_insufficient_stock")
        assertTrue(stock is ApiError.Business && stock.code == "INSUFFICIENT_STOCK" && stock.message.contains("in stock"))
    }

    @Test
    fun `validation, plan limit and role errors`() = runTest {
        assertTrue(errorFor(400, "error_validation") is ApiError.Validation)
        assertTrue(errorFor(402, "error_plan_limit") is ApiError.PlanLimit)
        assertEquals(ApiError.Forbidden, errorFor(403, "error_forbidden"))
    }

    @Test
    fun `a gateway HTML page is a server error, not a crash`() = runTest {
        val client = clientAnswering(502, "<html>Bad gateway</html>", "text/html")
        val e = com.medassist.app.core.repository.TestAccess.call { client.get("x") }.exceptionOrNull()!!.toApiError()
        assertEquals(ApiError.Server, e)
    }

    @Test
    fun `no connection is Network`() = runTest {
        val client = MedAssistHttpClient.create("https://api.test/api/v1", false, retryBaseDelayMs = 0, engine = MockEngine { throw IOException("offline") })
        val e = com.medassist.app.core.repository.TestAccess.call { client.get("x") }.exceptionOrNull()!!.toApiError()
        assertEquals(ApiError.Network, e)
    }
}
