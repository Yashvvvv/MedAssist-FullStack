package com.example.medassist_android.data.model

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Json

@JsonClass(generateAdapter = true)
data class User(
    @Json(name = "id") val id: Long,
    @Json(name = "username") val username: String,
    @Json(name = "email") val email: String,
    @Json(name = "firstName") val firstName: String,
    @Json(name = "lastName") val lastName: String,
    @Json(name = "phoneNumber") val phoneNumber: String?,
    @Json(name = "isVerified") val isVerified: Boolean,
    @Json(name = "isHealthcareProvider") val isHealthcareProvider: Boolean,
    @Json(name = "providerVerified") val providerVerified: Boolean,
    @Json(name = "medicalSpecialty") val medicalSpecialty: String?,
    @Json(name = "hospitalAffiliation") val hospitalAffiliation: String?,
    @Json(name = "lastLogin") val lastLogin: String?,
    @Json(name = "roles") val roles: List<String>,
    @Json(name = "permissions") val permissions: List<String>
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String,
    @Json(name = "data") val data: AuthData?
)

@JsonClass(generateAdapter = true)
data class AuthData(
    @Json(name = "accessToken") val accessToken: String,
    @Json(name = "refreshToken") val refreshToken: String,
    @Json(name = "tokenType") val tokenType: String,
    @Json(name = "user") val user: User
)

@JsonClass(generateAdapter = true)
data class RegistrationData(
    @Json(name = "email") val email: String,
    @Json(name = "userId") val userId: Long,
    @Json(name = "username") val username: String,
    @Json(name = "emailVerificationRequired") val emailVerificationRequired: Boolean
)

@JsonClass(generateAdapter = true)
data class RegistrationResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String,
    @Json(name = "data") val data: RegistrationData?
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "usernameOrEmail") val usernameOrEmail: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    @Json(name = "username") val username: String,
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String,
    @Json(name = "firstName") val firstName: String,
    @Json(name = "lastName") val lastName: String,
    @Json(name = "phoneNumber") val phoneNumber: String?
)

@JsonClass(generateAdapter = true)
data class HealthcareProviderRegisterRequest(
    @Json(name = "username") val username: String,
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String,
    @Json(name = "firstName") val firstName: String,
    @Json(name = "lastName") val lastName: String,
    @Json(name = "phoneNumber") val phoneNumber: String?,
    @Json(name = "licenseNumber") val licenseNumber: String,
    @Json(name = "medicalSpecialty") val medicalSpecialty: String,
    @Json(name = "hospitalAffiliation") val hospitalAffiliation: String
)

@JsonClass(generateAdapter = true)
data class ForgotPasswordRequest(
    @Json(name = "email") val email: String
)

@JsonClass(generateAdapter = true)
data class ResetPasswordRequest(
    @Json(name = "token") val token: String,
    @Json(name = "newPassword") val newPassword: String
)

@JsonClass(generateAdapter = true)
data class ChangePasswordRequest(
    @Json(name = "currentPassword") val currentPassword: String,
    @Json(name = "newPassword") val newPassword: String
)

@JsonClass(generateAdapter = true)
data class RefreshTokenRequest(
    @Json(name = "refreshToken") val refreshToken: String
)

@JsonClass(generateAdapter = true)
data class RefreshTokenResponse(
    @Json(name = "accessToken") val accessToken: String,
    @Json(name = "refreshToken") val refreshToken: String,
    @Json(name = "tokenType") val tokenType: String
)

@JsonClass(generateAdapter = true)
data class ForgotPasswordResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String,
    @Json(name = "data") val data: String? = null
)

@JsonClass(generateAdapter = true)
data class ApiResponse<T>(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String,
    @Json(name = "data") val data: T?
)

@JsonClass(generateAdapter = true)
data class ApiError(
    @Json(name = "success") val success: Boolean,
    @Json(name = "error") val error: ErrorDetail
)

@JsonClass(generateAdapter = true)
data class ErrorDetail(
    @Json(name = "message") val message: String,
    @Json(name = "code") val code: String,
    @Json(name = "timestamp") val timestamp: Long
)
