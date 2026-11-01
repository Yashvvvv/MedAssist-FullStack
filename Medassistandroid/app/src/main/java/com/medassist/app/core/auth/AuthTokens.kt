package com.medassist.app.core.auth

import com.medassist.app.core.model.AuthResponse
import com.medassist.app.core.model.Role
import kotlinx.serialization.Serializable

/**
 * What the app keeps between launches: the two tokens plus enough of the user to draw the
 * first screen without a network call (name, active pharmacy and role decide the app mode).
 */
@Serializable
data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val fullName: String,
    val email: String,
    val orgId: String? = null,
    val orgName: String? = null,
    val role: Role? = null,
    val membershipCount: Int = 0,
) {
    companion object {
        fun from(r: AuthResponse) = AuthTokens(
            accessToken = r.accessToken,
            refreshToken = r.refreshToken,
            userId = r.user.id,
            fullName = r.user.fullName,
            email = r.user.email,
            orgId = r.activeOrg?.orgId,
            orgName = r.activeOrg?.orgName,
            role = r.activeOrg?.role,
            membershipCount = r.memberships.size,
        )
    }
}
