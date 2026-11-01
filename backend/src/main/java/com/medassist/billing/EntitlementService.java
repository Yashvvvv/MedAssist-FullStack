package com.medassist.billing;

import com.medassist.common.config.AppProperties;
import com.medassist.common.error.ApiException;
import com.medassist.tenancy.InvitationRepository;
import com.medassist.tenancy.Membership;
import com.medassist.tenancy.MembershipRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.YearMonth;
import java.util.Map;
import java.util.UUID;

/**
 * The single place that answers "is this org allowed to do X?". Limit breaches raise
 * HTTP 402 with code PLAN_LIMIT_REACHED and the current/limit numbers, so the app can
 * show an upgrade prompt instead of a generic error.
 */
@Service
public class EntitlementService {

    public enum Metric { AI_CALLS }

    private final SubscriptionRepository subscriptions;
    private final MembershipRepository memberships;
    private final InvitationRepository invitations;
    private final Clock clock;
    private final AppProperties app;

    public EntitlementService(SubscriptionRepository subscriptions, MembershipRepository memberships,
                              InvitationRepository invitations, Clock clock,
                              AppProperties app) {
        this.subscriptions = subscriptions;
        this.memberships = memberships;
        this.invitations = invitations;
        this.clock = clock;
        this.app = app;
    }

    @Transactional(readOnly = true)
    public Plan planOf(UUID orgId) {
        return subscriptions.findByOrgId(orgId)
                .map(s -> s.effectivePlan(clock.instant()))
                .orElse(Plan.FREE);
    }

    public void requireFeature(UUID orgId, Plan.Feature feature) {
        Plan plan = planOf(orgId);
        if (!plan.has(feature)) {
            throw new ApiException(HttpStatus.PAYMENT_REQUIRED, "PLAN_FEATURE_UNAVAILABLE",
                    feature + " is not included in the " + plan + " plan",
                    Map.of("feature", feature.name(), "plan", plan.name()));
        }
    }

    public void checkBranchLimit(UUID orgId, long currentBranches) {
        Plan plan = planOf(orgId);
        if (currentBranches >= plan.maxBranches()) {
            throw limit("branches", currentBranches, plan.maxBranches(), plan);
        }
    }

    public void checkProductLimit(UUID orgId, long currentProducts, int adding) {
        Plan plan = planOf(orgId);
        if (currentProducts + adding > plan.maxProducts()) {
            throw limit("products", currentProducts, plan.maxProducts(), plan);
        }
    }

    /** Seats = active members + pending invitations (an invite reserves a seat). */
    public void checkStaffLimit(UUID orgId) {
        Plan plan = planOf(orgId);
        long seats = memberships.countByOrgIdAndStatus(orgId, Membership.Status.ACTIVE)
                + invitations.countPending(orgId, clock.instant());
        if (seats >= plan.maxStaff()) {
            throw limit("staff", seats, plan.maxStaff(), plan);
        }
    }

    /**
     * Counts one unit of a metered resource, failing if the monthly quota is used up.
     * Runs in its own transaction so the count sticks even if the caller later fails
     * (an AI call costs money whether or not we use the answer).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public long consume(UUID orgId, Metric metric) {
        Plan plan = planOf(orgId);
        String period = currentPeriod();
        long used = subscriptions.currentUsage(orgId, metric.name(), period);
        int quota = switch (metric) {
            case AI_CALLS -> plan.aiCallsPerMonth();
        };
        if (used >= quota) {
            throw limit("aiCallsPerMonth", used, quota, plan);
        }
        return subscriptions.incrementUsage(orgId, metric.name(), period);
    }

    @Transactional(readOnly = true)
    public long usage(UUID orgId, Metric metric) {
        return subscriptions.currentUsage(orgId, metric.name(), currentPeriod());
    }

    private String currentPeriod() {
        return YearMonth.now(clock.withZone(app.businessZone())).toString();
    }

    private static ApiException limit(String what, long current, long max, Plan plan) {
        return new ApiException(HttpStatus.PAYMENT_REQUIRED, "PLAN_LIMIT_REACHED",
                "Your " + plan + " plan allows " + max + " " + what + ". Upgrade to add more.",
                Map.of("limit", what, "current", current, "max", max, "plan", plan.name()));
    }
}
