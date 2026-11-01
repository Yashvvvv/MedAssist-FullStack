package com.medassist.audit;

import com.medassist.common.web.PageResponse;
import com.medassist.common.web.Pages;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@Tag(name = "Audit", description = "Who did what in your pharmacy")
@RestController
public class AuditController {

    public record AuditView(UUID id, UUID actorUserId, String action, String entityType, UUID entityId,
                            String details, Instant createdAt) {
    }

    private final AuditLogRepository logs;

    public AuditController(AuditLogRepository logs) {
        this.logs = logs;
    }

    @Operation(summary = "Activity log, newest first (tenant-filtered)")
    @GetMapping("/api/v1/audit")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public PageResponse<AuditView> search(@RequestParam(required = false) String entityType,
                                          @RequestParam(required = false) UUID actor,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "50") int size) {
        return PageResponse.of(logs.search(entityType, actor, Pages.of(page, size)),
                a -> new AuditView(a.getId(), a.getActorUserId(), a.getAction(), a.getEntityType(), a.getEntityId(),
                        a.getDetails(), a.getCreatedAt()));
    }
}
