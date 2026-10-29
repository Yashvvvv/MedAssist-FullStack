package com.medassist.tenancy;

import com.medassist.common.persistence.BaseEntity;
import com.medassist.security.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** A pending invite for a staff member, accepted with a single-use token sent by email. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "invitations")
public class Invitation extends BaseEntity {

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "invited_by")
    private UUID invitedBy;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    public boolean isPending(Instant now) {
        return acceptedAt == null && revokedAt == null && expiresAt.isAfter(now);
    }
}
