package com.medassist.identity;

import com.medassist.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * A refresh token, stored only as its SHA-256 hash.
 *
 * <p>Rotation with reuse detection: every refresh revokes the presented token and issues
 * a new one in the same {@code familyId}. If a token that was already rotated is presented
 * again, someone replayed a stolen token, so the whole family is revoked and both the
 * thief and the real user must log in again (the OAuth 2.0 Security BCP recommendation).</p>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    /** The org the session is scoped to; refreshed access tokens keep it. */
    @Column(name = "org_id")
    private UUID orgId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "replaced_by")
    private UUID replacedBy;

    @Column(name = "user_agent")
    private String userAgent;

    public boolean isActive(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }
}
