package com.medassist.tenancy;

import com.medassist.security.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    Optional<Membership> findByOrgIdAndUserId(UUID orgId, UUID userId);

    List<Membership> findByUserIdAndStatus(UUID userId, Membership.Status status);

    List<Membership> findByOrgIdOrderByCreatedAtAsc(UUID orgId);

    long countByOrgIdAndStatus(UUID orgId, Membership.Status status);

    long countByOrgIdAndRoleAndStatus(UUID orgId, Role role, Membership.Status status);

    @Query("select m.userId from Membership m where m.orgId = :orgId and m.status = 'ACTIVE' and m.role in :roles")
    List<UUID> findActiveUserIdsWithRoles(UUID orgId, List<Role> roles);
}
