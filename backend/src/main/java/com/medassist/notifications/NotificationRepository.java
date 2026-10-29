package com.medassist.notifications;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * Claims a batch of due outbox rows. SKIP LOCKED lets several instances poll the same
     * table without double-sending (lock timeout -2 = SKIP LOCKED in Hibernate).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("""
            select n from Notification n where n.status = 'PENDING' and n.channel <> 'IN_APP'
            and n.scheduledAt <= :now order by n.scheduledAt""")
    List<Notification> claimDue(Instant now, Pageable pageable);

    @Query("""
            select n from Notification n where n.channel = 'IN_APP' and n.userId = :userId
            and (n.orgId = :orgId or n.orgId is null) order by n.createdAt desc""")
    Page<Notification> inbox(UUID userId, UUID orgId, Pageable pageable);

    @Query("""
            select count(n) from Notification n where n.channel = 'IN_APP' and n.userId = :userId
            and (n.orgId = :orgId or n.orgId is null) and n.readAt is null""")
    long unreadCount(UUID userId, UUID orgId);

    @Modifying
    @Query("update Notification n set n.readAt = :now where n.channel = 'IN_APP' and n.userId = :userId and n.readAt is null")
    int markAllRead(UUID userId, Instant now);
}
