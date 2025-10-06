package com.example.medassist_android.data.network

import com.example.medassist_android.data.model.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface AuthApiService {

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegistrationResponse>

    @POST("api/auth/register-healthcare-provider")
    suspend fun registerHealthcareProvider(@Body request: HealthcareProviderRegisterRequest): Response<RegistrationResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/auth/refresh-token")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<RefreshTokenResponse>

    @GET("api/auth/verify-email")
    suspend fun verifyEmail(@Query("token") token: String): Response<ApiResponse<Nothing>>

    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<ForgotPasswordResponse>

    @POST("api/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<ApiResponse<Nothing>>

    @POST("api/auth/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<ApiResponse<Nothing>>

    @GET("api/auth/me")
    suspend fun getCurrentUser(): Response<ApiResponse<User>>

    @POST("api/auth/logout")
    suspend fun logout(): Response<ResponseBody>

    @POST("api/auth/resend-verification")
    suspend fun resendVerificationEmail(@Query("email") email: String): Response<ApiResponse<Nothing>>
}
