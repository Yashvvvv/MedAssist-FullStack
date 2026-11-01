package com.medassist.app.core.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class RegisterRequest(val email: String, val password: String, val fullName: String, val phone: String? = null)

@Serializable
data class BranchRequest(
    val name: String,
    val addressLine: String,
    val city: String,
    val state: String,
    val pincode: String,
    val code: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val phone: String? = null,
    val openingHours: String? = null,
    val open24x7: Boolean = false,
    val deliveryAvailable: Boolean = false,
    val publiclyListed: Boolean? = true,
)

@Serializable
data class CreateOrganizationRequest(
    val name: String,
    val firstBranch: BranchRequest,
    val gstin: String? = null,
    val drugLicenseNo: String? = null,
    val phone: String? = null,
    val email: String? = null,
)

@Serializable
data class RegisterPharmacyRequest(val owner: RegisterRequest, val organization: CreateOrganizationRequest)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class SwitchOrgRequest(val orgId: String?)

@Serializable
data class EmailRequest(val email: String)

@Serializable
data class UpdateProfileRequest(val fullName: String, val phone: String? = null)

@Serializable
data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)

@Serializable
data class UserView(
    val id: String,
    val email: String,
    val fullName: String,
    val phone: String? = null,
    val emailVerified: Boolean = false,
)

@Serializable
data class MembershipView(val orgId: String, val orgName: String, val role: Role)

@Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: UserView,
    val activeOrg: MembershipView? = null,
    val memberships: List<MembershipView> = emptyList(),
)

@Serializable
data class MeResponse(
    val user: UserView,
    val activeOrg: MembershipView? = null,
    val memberships: List<MembershipView> = emptyList(),
)
