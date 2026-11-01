package com.medassist.app.core.model

import kotlinx.serialization.Serializable

@Serializable
data class BranchView(
    val id: String,
    val name: String,
    val code: String? = null,
    val addressLine: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val phone: String? = null,
    val openingHours: String? = null,
    val open24x7: Boolean = false,
    val deliveryAvailable: Boolean = false,
    val publiclyListed: Boolean = true,
    val active: Boolean = true,
)

enum class Plan { FREE, PRO, BUSINESS }

@Serializable
data class PlanView(
    val plan: Plan,
    val pricePaisePerMonth: Int,
    val maxBranches: Int,
    val maxStaff: Int,
    val maxProducts: Int,
    val aiCallsPerMonth: Int,
    val features: Set<String> = emptySet(),
)

@Serializable
data class UsageView(val branches: Long = 0, val staffSeats: Long = 0, val products: Long = 0, val aiCallsThisMonth: Long = 0)

@Serializable
data class SubscriptionView(
    val plan: Plan,
    val effectivePlan: Plan,
    val status: String,
    val trialEndsAt: String? = null,
    val currentPeriodEnd: String? = null,
)

@Serializable
data class OrganizationView(
    val id: String,
    val name: String,
    val gstin: String? = null,
    val drugLicenseNo: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val yourRole: Role? = null,
    val subscription: SubscriptionView? = null,
    val limits: PlanView? = null,
    val usage: UsageView? = null,
)

@Serializable
data class ChangePlanRequest(val plan: Plan)

@Serializable
data class MemberView(
    val membershipId: String,
    val userId: String,
    val fullName: String,
    val email: String,
    val role: Role,
    val status: String,
)

@Serializable
data class InvitationView(val id: String, val email: String, val role: Role, val expiresAt: String? = null)

@Serializable
data class OrgSummary(val members: List<MemberView> = emptyList(), val pendingInvitations: List<InvitationView> = emptyList())

@Serializable
data class InviteRequest(val email: String, val role: Role)

@Serializable
data class InvitationCreated(val invitation: InvitationView, val acceptUrl: String)

@Serializable
data class ChangeRoleRequest(val role: Role)
