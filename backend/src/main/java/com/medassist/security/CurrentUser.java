package com.medassist.security;

import com.medassist.common.error.ApiException;

import java.util.UUID;

/**
 * The authenticated caller, built from the access token. Controllers receive it as a
 * method parameter (see {@link CurrentUserArgumentResolver}).
 *
 * @param orgId null for consumers (patients) and for staff who have not picked an org
 * @param role  the caller's role in {@code orgId}; null when orgId is null
 */
public record CurrentUser(UUID userId, String email, String name, UUID orgId, Role role) {

    public UUID requireOrg() {
        if (orgId == null) {
            throw ApiException.forbidden("NO_ACTIVE_ORG",
                    "Select a pharmacy first (POST /api/v1/auth/switch-org)");
        }
        return orgId;
    }

    public boolean isStaff() {
        return orgId != null && role != null;
    }
}
