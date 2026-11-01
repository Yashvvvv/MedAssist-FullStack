package com.medassist.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    @Query("""
            select a from AuditLog a where (:entityType is null or a.entityType = :entityType)
            and (:actor is null or a.actorUserId = :actor) order by a.createdAt desc""")
    Page<AuditLog> search(String entityType, UUID actor, Pageable pageable);
}
