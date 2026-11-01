package com.medassist.app.core.repository

import com.medassist.app.core.model.BranchView
import com.medassist.app.core.model.ChangePlanRequest
import com.medassist.app.core.model.ChangeRoleRequest
import com.medassist.app.core.model.InvitationCreated
import com.medassist.app.core.model.InviteRequest
import com.medassist.app.core.model.OrgSummary
import com.medassist.app.core.model.OrganizationView
import com.medassist.app.core.model.Plan
import com.medassist.app.core.model.PlanView
import com.medassist.app.core.model.Role
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post

/** The pharmacy itself: profile, branches, staff and the SaaS plan. */
class PharmacyRepository(private val client: HttpClient) {

    suspend fun organization(): Result<OrganizationView> = apiCall { client.get("org").body() }

    suspend fun branches(): Result<List<BranchView>> = apiCall { client.get("branches").body() }

    suspend fun staff(): Result<OrgSummary> = apiCall { client.get("staff").body() }

    suspend fun invite(email: String, role: Role): Result<InvitationCreated> = apiCall {
        client.post("staff/invitations") { json(InviteRequest(email.trim(), role)) }.body()
    }

    suspend fun revokeInvitation(id: String): Result<Unit> = apiCall {
        client.delete("staff/invitations/$id")
        Unit
    }

    suspend fun changeRole(membershipId: String, role: Role): Result<Unit> = apiCall {
        client.patch("staff/$membershipId/role") { json(ChangeRoleRequest(role)) }
        Unit
    }

    suspend fun removeMember(membershipId: String): Result<Unit> = apiCall {
        client.delete("staff/$membershipId")
        Unit
    }

    suspend fun plans(): Result<List<PlanView>> = apiCall { client.get("billing/plans").body() }

    suspend fun changePlan(plan: Plan): Result<OrganizationView> = apiCall {
        client.post("billing/plan") { json(ChangePlanRequest(plan)) }.body()
    }
}
