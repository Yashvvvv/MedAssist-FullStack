package com.medassist.app.core.repository

import com.medassist.app.core.auth.AuthGateway
import com.medassist.app.core.auth.RefreshResult
import com.medassist.app.core.model.ApiError
import com.medassist.app.core.model.AuthResponse
import com.medassist.app.core.model.ChangePasswordRequest
import com.medassist.app.core.model.EmailRequest
import com.medassist.app.core.model.LoginRequest
import com.medassist.app.core.model.MeResponse
import com.medassist.app.core.model.RefreshRequest
import com.medassist.app.core.model.RegisterPharmacyRequest
import com.medassist.app.core.model.RegisterRequest
import com.medassist.app.core.model.SwitchOrgRequest
import com.medassist.app.core.model.UpdateProfileRequest
import com.medassist.app.core.model.toApiError
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * Account endpoints. [plain] is a client without the Auth plugin: login, refresh and logout must
 * never carry a stale token or trigger a refresh themselves.
 */
class AuthRepository(private val client: HttpClient, private val plain: HttpClient) : AuthGateway {

    suspend fun login(email: String, password: String): Result<AuthResponse> = apiCall {
        plain.post("auth/login") { json(LoginRequest(email.trim(), password)) }.body()
    }

    suspend fun register(req: RegisterRequest): Result<AuthResponse> = apiCall {
        plain.post("auth/register") { json(req) }.body()
    }

    suspend fun registerPharmacy(req: RegisterPharmacyRequest): Result<AuthResponse> = apiCall {
        plain.post("auth/register-pharmacy") { json(req) }.body()
    }

    suspend fun forgotPassword(email: String): Result<Unit> = apiCall {
        plain.post("auth/forgot-password") { json(EmailRequest(email.trim())) }
        Unit
    }

    suspend fun me(): Result<MeResponse> = apiCall { client.get("auth/me").body() }

    suspend fun updateProfile(req: UpdateProfileRequest): Result<Unit> = apiCall {
        client.put("auth/me") { json(req) }
        Unit
    }

    suspend fun changePassword(current: String, new: String): Result<Unit> = apiCall {
        client.post("auth/change-password") { json(ChangePasswordRequest(current, new)) }
        Unit
    }

    suspend fun switchOrg(orgId: String?): Result<AuthResponse> = apiCall {
        client.post("auth/switch-org") { json(SwitchOrgRequest(orgId)) }.body()
    }

    override suspend fun refresh(refreshToken: String): RefreshResult =
        apiCall<AuthResponse> { plain.post("auth/refresh") { json(RefreshRequest(refreshToken)) }.body() }.fold(
            onSuccess = { RefreshResult.Success(it) },
            onFailure = {
                when (it.toApiError()) {
                    ApiError.Unauthorized, is ApiError.Business -> RefreshResult.Rejected
                    else -> RefreshResult.Unavailable
                }
            },
        )

    override suspend fun logout(refreshToken: String) {
        apiCall { plain.post("auth/logout") { json(RefreshRequest(refreshToken)) } }
    }
}

internal inline fun <reified T> HttpRequestBuilder.json(body: T) {
    contentType(ContentType.Application.Json)
    setBody(body)
}
