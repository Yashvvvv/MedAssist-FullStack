package com.example.medassist_android.data.repository

import com.example.medassist_android.data.local.TokenManager
import com.example.medassist_android.data.model.*
import com.example.medassist_android.data.network.AuthApiService
import com.example.medassist_android.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import com.squareup.moshi.Moshi

@Singleton
class AuthRepository @Inject constructor(
    private val authApiService: AuthApiService,
    private val tokenManager: TokenManager,
    private val moshi: Moshi
) {

    private fun parseErrorBody(errorBodyString: String?): String {
        if (errorBodyString.isNullOrEmpty()) {
            return "Unknown error"
        }
        return try {
            val adapter = moshi.adapter(ApiErrorResponse::class.java)
            val errorResponse = adapter.fromJson(errorBodyString)
            errorResponse?.getFormattedErrorMessage() ?: "Unknown error"
        } catch (e: Exception) {
            Timber.e(e, "Error parsing error body: $errorBodyString")
            // Enhanced fallback: try to extract message from nested JSON structure
            try {
                if (errorBodyString.contains("\"error\"") && errorBodyString.contains("\"message\"")) {
                    // Try to extract the nested message
                    val messageRegex = """"error"\s*:\s*\{[^}]*"message"\s*:\s*"([^"]+)"""".toRegex()
                    val matchResult = messageRegex.find(errorBodyString)
                    val baseMessage = matchResult?.groupValues?.get(1) ?: "Validation failed"

                    // Try to extract validation details
                    val detailsRegex = """"details"\s*:\s*\{([^}]+)\}""".toRegex()
                    val detailsMatch = detailsRegex.find(errorBodyString)
                    if (detailsMatch != null) {
                        val detailsText = detailsMatch.groupValues[1]
                        val fieldErrors = mutableListOf<String>()
                        val fieldRegex = """"([^"]+)"\s*:\s*"([^"]+)"""".toRegex()
                        fieldRegex.findAll(detailsText).forEach { match ->
                            val field = match.groupValues[1]
                            val message = match.groupValues[2]
                            fieldErrors.add("$field: $message")
                        }
                        if (fieldErrors.isNotEmpty()) {
                            return "$baseMessage - ${fieldErrors.joinToString("; ")}"
                        }
                    }
                    baseMessage
                } else {
                    errorBodyString
                }
            } catch (fallbackException: Exception) {
                Timber.e(fallbackException, "Fallback error parsing failed")
                errorBodyString
            }
        }
    }

    fun login(request: LoginRequest): Flow<Resource<AuthResponse>> = flow {
        try {
            emit(Resource.Loading())

            Timber.d("Login attempt for user: ${request.usernameOrEmail}")
            val response = authApiService.login(request)

            Timber.d("Login response code: ${response.code()}")
            if (response.isSuccessful) {
                val authResponse = response.body()
                Timber.d("Login response success: ${authResponse?.success}")

                if (authResponse?.success == true && authResponse.data != null) {
                    // Save tokens and user info
                    tokenManager.saveTokens(
                        authResponse.data.accessToken,
                        authResponse.data.refreshToken
                    )
                    tokenManager.saveUserInfo(
                        authResponse.data.user.id.toString(),
                        authResponse.data.user.username,
                        authResponse.data.user.email
                    )

                    emit(Resource.Success(authResponse))
                } else {
                    val errorMsg = authResponse?.message ?: "Login failed"
                    Timber.e("Login failed: $errorMsg")
                    emit(Resource.Error(errorMsg))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Timber.e("Login HTTP error: ${response.code()} - $errorBody")
                emit(Resource.Error(parseErrorBody(errorBody)))
            }
        } catch (e: HttpException) {
            Timber.e(e, "Login HTTP exception")
            emit(Resource.Error(e.localizedMessage ?: "Network error"))
        } catch (e: IOException) {
            Timber.e(e, "Login IO exception")
            emit(Resource.Error("Network connection error"))
        } catch (e: Exception) {
            Timber.e(e, "Login error")
            emit(Resource.Error("An unexpected error occurred"))
        }
    }

    fun register(request: RegisterRequest): Flow<Resource<RegistrationResponse>> = flow {
        try {
            emit(Resource.Loading())

            val response = authApiService.register(request)
            if (response.isSuccessful) {
                val registrationResponse = response.body()
                if (registrationResponse?.success == true) {
                    emit(Resource.Success(registrationResponse))
                } else {
                    emit(Resource.Error(registrationResponse?.message ?: "Registration failed"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                emit(Resource.Error(parseErrorBody(errorBody)))
            }
        } catch (e: HttpException) {
            emit(Resource.Error(e.localizedMessage ?: "Network error"))
        } catch (e: IOException) {
            emit(Resource.Error("Network connection error"))
        } catch (e: Exception) {
            Timber.e(e, "Registration error")
            emit(Resource.Error("An unexpected error occurred"))
        }
    }

    fun registerHealthcareProvider(request: HealthcareProviderRegisterRequest): Flow<Resource<RegistrationResponse>> = flow {
        try {
            emit(Resource.Loading())

            val response = authApiService.registerHealthcareProvider(request)
            if (response.isSuccessful) {
                val registrationResponse = response.body()
                if (registrationResponse?.success == true) {
                    emit(Resource.Success(registrationResponse))
                } else {
                    emit(Resource.Error(registrationResponse?.message ?: "Healthcare provider registration failed"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                emit(Resource.Error(parseErrorBody(errorBody)))
            }
        } catch (e: HttpException) {
            emit(Resource.Error(e.localizedMessage ?: "Network error"))
        } catch (e: IOException) {
            emit(Resource.Error("Network connection error"))
        } catch (e: Exception) {
            Timber.e(e, "Healthcare provider registration error")
            emit(Resource.Error("An unexpected error occurred"))
        }
    }

    fun forgotPassword(request: ForgotPasswordRequest): Flow<Resource<ForgotPasswordResponse>> = flow {
        try {
            emit(Resource.Loading())

            val response = authApiService.forgotPassword(request)
            if (response.isSuccessful) {
                val apiResponse = response.body()
                if (apiResponse?.success == true) {
                    emit(Resource.Success(apiResponse))
                } else {
                    emit(Resource.Error(apiResponse?.message ?: "Failed to send reset email"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                emit(Resource.Error(parseErrorBody(errorBody) ?: "Failed to send reset email"))
            }
        } catch (e: HttpException) {
            Timber.e(e, "Forgot password HTTP error")
            emit(Resource.Error(e.localizedMessage ?: "Network error"))
        } catch (e: IOException) {
            Timber.e(e, "Forgot password IO error")
            emit(Resource.Error("Network connection error"))
        } catch (e: Exception) {
            Timber.e(e, "Forgot password error")
            emit(Resource.Error("An unexpected error occurred"))
        }
    }

    fun resetPassword(request: ResetPasswordRequest): Flow<Resource<ApiResponse<Nothing>>> = flow {
        try {
            emit(Resource.Loading())

            val response = authApiService.resetPassword(request)
            if (response.isSuccessful) {
                val apiResponse = response.body()
                if (apiResponse?.success == true) {
                    emit(Resource.Success(apiResponse))
                } else {
                    emit(Resource.Error(apiResponse?.message ?: "Password reset failed"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                emit(Resource.Error(parseErrorBody(errorBody) ?: "Password reset failed"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error(e.localizedMessage ?: "Network error"))
        } catch (e: IOException) {
            emit(Resource.Error("Network connection error"))
        } catch (e: Exception) {
            Timber.e(e, "Reset password error")
            emit(Resource.Error("An unexpected error occurred"))
        }
    }

    fun changePassword(request: ChangePasswordRequest): Flow<Resource<ApiResponse<Nothing>>> = flow {
        try {
            emit(Resource.Loading())

            val response = authApiService.changePassword(request)
            if (response.isSuccessful) {
                val apiResponse = response.body()
                if (apiResponse?.success == true) {
                    emit(Resource.Success(apiResponse))
                } else {
                    emit(Resource.Error(apiResponse?.message ?: "Password change failed"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                emit(Resource.Error(parseErrorBody(errorBody) ?: "Password change failed"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error(e.localizedMessage ?: "Network error"))
        } catch (e: IOException) {
            emit(Resource.Error("Network connection error"))
        } catch (e: Exception) {
            Timber.e(e, "Change password error")
            emit(Resource.Error("An unexpected error occurred"))
        }
    }

    fun getCurrentUser(): Flow<Resource<User>> = flow {
        try {
            emit(Resource.Loading())

            val response = authApiService.getCurrentUser()
            if (response.isSuccessful) {
                val apiResponse = response.body()
                if (apiResponse?.success == true && apiResponse.data != null) {
                    emit(Resource.Success(apiResponse.data))
                } else {
                    emit(Resource.Error(apiResponse?.message ?: "Failed to get user info"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                emit(Resource.Error(parseErrorBody(errorBody) ?: "Failed to get user info"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error(e.localizedMessage ?: "Network error"))
        } catch (e: IOException) {
            emit(Resource.Error("Network connection error"))
        } catch (e: Exception) {
            Timber.e(e, "Get current user error")
            emit(Resource.Error("An unexpected error occurred"))
        }
    }

    fun logout(): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            val response = authApiService.logout()
            if (response.isSuccessful) {
                tokenManager.clearTokens()
                emit(Resource.Success(true))
            } else {
                val errorBody = response.errorBody()?.string()
                emit(Resource.Error(parseErrorBody(errorBody) ?: "Logout failed"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error(e.localizedMessage ?: "Network error"))
        } catch (e: IOException) {
            emit(Resource.Error("Network connection error"))
        } catch (e: Exception) {
            Timber.e(e, "Logout error")
            emit(Resource.Error("An unexpected error occurred"))
        }
    }

    suspend fun isLoggedIn(): Boolean = tokenManager.isLoggedIn()

    val isLoggedInFlow: Flow<Boolean> = tokenManager.isLoggedInFlow

    suspend fun refreshToken(): Flow<Resource<RefreshTokenResponse>> = flow {
        try {
            emit(Resource.Loading())

            val refreshToken = tokenManager.getRefreshToken()
            if (refreshToken != null) {
                val response = authApiService.refreshToken(RefreshTokenRequest(refreshToken))
                if (response.isSuccessful) {
                    val tokenResponse = response.body()
                    if (tokenResponse != null) {
                        tokenManager.saveTokens(
                            tokenResponse.accessToken,
                            tokenResponse.refreshToken
                        )
                        emit(Resource.Success(tokenResponse))
                    } else {
                        tokenManager.clearTokens()
                        emit(Resource.Error("Token refresh failed"))
                    }
                } else {
                    tokenManager.clearTokens()
                    emit(Resource.Error("Token refresh failed"))
                }
            } else {
                emit(Resource.Error("No refresh token available"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Token refresh error")
            tokenManager.clearTokens()
            emit(Resource.Error("Token refresh failed"))
        }
    }
}
