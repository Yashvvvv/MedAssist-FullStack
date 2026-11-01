package com.medassist.common.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

/**
 * Superclass for every row that belongs to one pharmacy organization.
 *
 * <p>{@link TenantId} makes Hibernate (a) stamp the current tenant on insert and
 * (b) add {@code where org_id = :tenant} to every query on this entity. The current
 * tenant comes from {@code TenantIdentifierResolver}, which reads the org claim of
 * the caller's JWT. Code in services therefore never filters by org by hand, and
 * cannot forget to.</p>
 *
 * <p>Only code running as the ROOT tenant (signup, consumer orders, jobs) may assign
 * the org explicitly via {@link #assignTenant(UUID)}; Hibernate rejects an explicit org
 * that differs from the session's tenant everywhere else.</p>
 */
@Getter
@MappedSuperclass
public abstract class TenantEntity extends BaseEntity {

    @TenantId
    @Column(name = "org_id", nullable = false, updatable = false)
    private UUID orgId;

    public void assignTenant(UUID orgId) {
        if (this.orgId != null && !this.orgId.equals(orgId)) {
            throw new IllegalStateException("Tenant of an entity cannot change");
        }
        this.orgId = orgId;
    }
}
