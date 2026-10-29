package com.medassist.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findByOrgId(UUID orgId);

    Optional<Subscription> findByProviderRef(String providerRef);

    /** Atomic upsert-and-increment; returns the new count. Safe under concurrent requests. */
    @Modifying
    @Query(value = """
            insert into usage_counters (org_id, metric, period, count) values (:orgId, :metric, :period, 1)
            on conflict (org_id, metric, period) do update set count = usage_counters.count + 1
            returning count""", nativeQuery = true)
    long incrementUsage(@Param("orgId") UUID orgId, @Param("metric") String metric, @Param("period") String period);

    @Query(value = "select coalesce((select count from usage_counters where org_id = :orgId and metric = :metric and period = :period), 0)",
            nativeQuery = true)
    long currentUsage(@Param("orgId") UUID orgId, @Param("metric") String metric, @Param("period") String period);

    @Modifying
    @Query(value = "insert into billing_events (id, provider, event_id, event_type, received_at) "
            + "values (gen_random_uuid(), :provider, :eventId, :eventType, now()) on conflict do nothing",
            nativeQuery = true)
    int recordEventOnce(@Param("provider") String provider, @Param("eventId") String eventId,
                        @Param("eventType") String eventType);
}
