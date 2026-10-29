package com.medassist.audit;

import com.medassist.common.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Records audit entries in the caller's transaction (so an action and its audit row
 * commit or roll back together).
 */
@Service
public class AuditService {

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void record(UUID actor, String action, String entityType, UUID entityId, String details) {
        AuditLog log = new AuditLog();
        log.setActorUserId(actor);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDetails(details == null || details.length() <= 2000 ? details : details.substring(0, 2000));
        UUID org = TenantContext.current()
                .filter(id -> !TenantContext.ROOT.equals(id))
                .orElseThrow(() -> new IllegalStateException("record() needs a tenant; use recordFor()"));
        log.assignTenant(org);
        repository.save(log);
    }

    /** For code running as ROOT (signup, consumer actions) that acts on a known org. */
    @Transactional
    public void recordFor(UUID orgId, UUID actor, String action, String entityType, UUID entityId, String details) {
        AuditLog log = new AuditLog();
        log.assignTenant(orgId);
        log.setActorUserId(actor);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDetails(details);
        repository.save(log);
    }
}
