package com.example.medassist_android.data.network

import com.example.medassist_android.data.model.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface AuthApiService {

    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegistrationResponse>

    @POST("api/v1/auth/register-healthcare-provider")
    suspend fun registerHealthcareProvider(@Body request: HealthcareProviderRegisterRequest): Response<RegistrationResponse>

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/v1/auth/refresh-token")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<RefreshTokenResponse>

    @GET("api/v1/auth/verify-email")
    suspend fun verifyEmail(@Query("token") token: String): Response<ApiResponse<Nothing>>

    @POST("api/v1/auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<ForgotPasswordResponse>

    @POST("api/v1/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<ApiResponse<Nothing>>

    @POST("api/v1/auth/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<ApiResponse<Nothing>>

    @GET("api/v1/auth/me")
    suspend fun getCurrentUser(): Response<ApiResponse<User>>

    @POST("api/v1/auth/logout")
    suspend fun logout(): Response<ResponseBody>

    @POST("api/v1/auth/resend-verification")
    suspend fun resendVerificationEmail(@Query("email") email: String): Response<ApiResponse<Nothing>>

    // Profile endpoints (aligned with UserProfileController)
    @GET("api/v1/profile/me")
    suspend fun getUserProfile(): Response<ApiResponse<UserProfile>>

    @PUT("api/v1/profile/update")
    suspend fun updateProfile(@Body request: UserProfileUpdateRequest): Response<ApiResponse<UserProfile>>

    @POST("api/v1/profile/deactivate")
    suspend fun deactivateAccount(): Response<ApiResponse<Nothing>>

    @GET("api/v1/profile/account-status")
    suspend fun getAccountStatus(): Response<ApiResponse<AccountStatus>>
}
