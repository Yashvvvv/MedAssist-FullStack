package com.example.medassist_android.data.network

import com.example.medassist_android.data.local.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Skip auth for login/register endpoints
        val skipAuth = originalRequest.url.encodedPath.contains("/auth/login") ||
                      originalRequest.url.encodedPath.contains("/auth/register") ||
                      originalRequest.url.encodedPath.contains("/auth/forgot-password") ||
                      originalRequest.url.encodedPath.contains("/auth/reset-password") ||
                      originalRequest.url.encodedPath.contains("/auth/verify-email") ||
                      originalRequest.url.encodedPath.contains("/auth/resend-verification") ||
                      originalRequest.url.encodedPath.contains("/medicines/search") || // Add public medicine search
                      originalRequest.url.encodedPath.matches(Regex(".*/medicines/\\d+$")) // Add public medicine detail by ID
        // Removed AI endpoints from skipAuth

        if (skipAuth) {
            return chain.proceed(originalRequest)
        }

        val token = runBlocking { tokenManager.getAccessToken() }
        Timber.d("Access token used for request: $token") // Debug log for token

        val requestWithAuth = if (token != null) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }

        Timber.d("Making request to: ${requestWithAuth.url}")
        return chain.proceed(requestWithAuth)
    }
}

class TokenRefreshInterceptor @Inject constructor(
    private val tokenManager: TokenManager,
    private val authApiService: AuthApiService
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val response = chain.proceed(originalRequest)

        // If we get a 401 (unauthorized), try to refresh the token
        if (response.code == 401 && !originalRequest.url.encodedPath.contains("/auth/refresh-token")) {
            response.close()

            val refreshToken = runBlocking { tokenManager.getRefreshToken() }
            if (refreshToken != null) {
                try {
                    val refreshResponse = runBlocking {
                        authApiService.refreshToken(
                            com.example.medassist_android.data.model.RefreshTokenRequest(refreshToken)
                        )
                    }

                    if (refreshResponse.isSuccessful) {
                        val newTokenData = refreshResponse.body()
                        if (newTokenData != null) {
                            runBlocking {
                                tokenManager.saveTokens(
                                    newTokenData.accessToken,
                                    newTokenData.refreshToken
                                )
                            }

                            // Retry the original request with new token
                            val newRequest = originalRequest.newBuilder()
                                .header("Authorization", "Bearer ${newTokenData.accessToken}")
                                .build()

                            return chain.proceed(newRequest)
                        }
                    } else {
                        // Refresh failed, clear tokens
                        runBlocking { tokenManager.clearTokens() }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Token refresh failed")
                    runBlocking { tokenManager.clearTokens() }
                }
            }
        }

        return response
    }
}
