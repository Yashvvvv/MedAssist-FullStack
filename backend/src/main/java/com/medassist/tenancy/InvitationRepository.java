package com.medassist.tenancy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    Optional<Invitation> findByTokenHash(String tokenHash);

    Optional<Invitation> findByIdAndOrgId(UUID id, UUID orgId);

    @Query("""
            select i from Invitation i where i.orgId = :orgId and i.acceptedAt is null
            and i.revokedAt is null and i.expiresAt > :now order by i.createdAt desc""")
    List<Invitation> findPending(UUID orgId, Instant now);

    @Query("""
            select count(i) from Invitation i where i.orgId = :orgId and i.acceptedAt is null
            and i.revokedAt is null and i.expiresAt > :now""")
    long countPending(UUID orgId, Instant now);
}
