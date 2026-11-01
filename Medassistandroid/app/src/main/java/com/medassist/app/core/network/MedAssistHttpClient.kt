package com.medassist.app.core.network

import com.medassist.app.core.auth.SessionManager
import com.medassist.app.core.model.AppJson
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.ANDROID
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.Url
import io.ktor.serialization.kotlinx.json.json
import java.io.IOException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/** The server answers a repeat of a request carrying this header with the original result. */
const val IDEMPOTENCY_KEY_HEADER = "Idempotency-Key"

/** Endpoints that must never carry a token or trigger a refresh. */
private val AnonymousPaths = listOf("auth/login", "auth/register", "auth/refresh", "auth/forgot-password")

object MedAssistHttpClient {

    /** Gateway errors and a sleeping free-tier instance waking up: worth one more try. */
    private val TransientStatuses = setOf(502, 503, 504)

    fun create(
        baseUrl: String,
        enableLogging: Boolean,
        engine: HttpClientEngine = OkHttp.create(),
        retryBaseDelayMs: Long = 1_000,
        session: SessionManager? = null,
        hasNetwork: () -> Boolean = { true },
    ): HttpClient = HttpClient(engine) {
        // Non-2xx throws ResponseException, which apiCall() turns into a typed ApiError.
        expectSuccess = true
        val root = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        defaultRequest { url(root) }
        install(ContentNegotiation) { json(AppJson) }

        // Only requests that are safe to repeat: GETs, and POSTs with an Idempotency-Key
        // (a retried checkout returns the same invoice instead of billing twice).
        install(HttpRequestRetry) {
            maxRetries = 2
            retryIf { request, response ->
                response.status.value in TransientStatuses &&
                    (request.method == HttpMethod.Get || request.headers.contains(IDEMPOTENCY_KEY_HEADER))
            }
            retryOnExceptionIf { request, cause ->
                cause is IOException && hasNetwork() &&
                    (request.method == HttpMethod.Get || request.headers.contains(IDEMPOTENCY_KEY_HEADER))
            }
            delayMillis(respectRetryAfterHeader = false) { retry -> retryBaseDelayMs * retry }
        }
        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            requestTimeoutMillis = 60_000 // a sleeping Render instance takes ~50 s to wake; AI calls take ~10 s
            socketTimeoutMillis = 60_000
        }
        // The client talks to one origin. Ktor's Auth plugin would otherwise hand the cached access
        // token to any host that answers 401.
        val origin = Url(root)
        install(
            createClientPlugin("SingleOriginGuard") {
                onRequest { request, _ ->
                    val url = request.url
                    check(url.protocol == origin.protocol && url.host == origin.host && url.port == origin.port) {
                        "Blocked request outside the API origin"
                    }
                }
            },
        )
        if (session != null) {
            install(Auth) {
                bearer {
                    // NonCancellable: Ktor shares one load/refresh between concurrent requests; a cancelled
                    // search must not cancel the refresh the others are waiting for, nor cut it off after
                    // the server already rotated the refresh token.
                    loadTokens {
                        withContext(NonCancellable) { session.currentTokens() }?.let { BearerTokens(it.accessToken, null) }
                    }
                    refreshTokens {
                        val rejected = oldTokens?.accessToken
                        withContext(NonCancellable) { session.refresh(rejected) }?.let { BearerTokens(it.accessToken, null) }
                    }
                    sendWithoutRequest { request ->
                        val path = request.url.buildString().substringBefore("?")
                        AnonymousPaths.none { path.endsWith(it) }
                    }
                }
            }
        }
        if (enableLogging) {
            install(Logging) {
                logger = Logger.ANDROID // Logger.DEFAULT goes to SLF4J, which has no provider on Android
                level = LogLevel.INFO
                sanitizeHeader { it == HttpHeaders.Authorization }
            }
        }
    }
}
