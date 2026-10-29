package com.medassist.billing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/** One per organization. The plan an org actually gets is {@link #effectivePlan(Instant)}. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "subscriptions")
public class Subscription {

    /** Grace period after a failed renewal before the org drops to FREE. */
    public static final Duration PAST_DUE_GRACE = Duration.ofDays(7);

    public enum Status { TRIALING, ACTIVE, PAST_DUE, CANCELED }

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "org_id", nullable = false, unique = true)
    private UUID orgId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Plan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "trial_ends_at")
    private Instant trialEndsAt;

    @Column(name = "current_period_end")
    private Instant currentPeriodEnd;

    /** manual | razorpay */
    @Column(nullable = false)
    private String provider;

    /** The provider's subscription id, used to match webhooks. */
    @Column(name = "provider_ref")
    private String providerRef;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    public Plan effectivePlan(Instant now) {
        return switch (status) {
            case TRIALING -> trialEndsAt != null && trialEndsAt.isAfter(now) ? plan : Plan.FREE;
            case ACTIVE -> currentPeriodEnd == null || currentPeriodEnd.isAfter(now) ? plan : Plan.FREE;
            case PAST_DUE -> currentPeriodEnd != null && currentPeriodEnd.plus(PAST_DUE_GRACE).isAfter(now)
                    ? plan : Plan.FREE;
            case CANCELED -> currentPeriodEnd != null && currentPeriodEnd.isAfter(now) ? plan : Plan.FREE;
        };
    }
}
