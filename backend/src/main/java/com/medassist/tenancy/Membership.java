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

import java.util.UUID;

/**
 * Links a user to an organization with a role. Deliberately not a TenantEntity:
 * login must list a user's memberships across all orgs before any tenant is chosen.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "memberships")
public class Membership extends BaseEntity {

    public enum Status { ACTIVE, SUSPENDED }

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;

    public Membership(UUID orgId, UUID userId, Role role) {
        this.orgId = orgId;
        this.userId = userId;
        this.role = role;
    }

    public boolean isActive() {
        return status == Status.ACTIVE;
    }
}
