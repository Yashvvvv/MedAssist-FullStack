package com.medassist.billing;

import com.medassist.audit.AuditService;
import com.medassist.common.config.AppProperties;
import com.medassist.common.error.ApiException;
import com.medassist.notifications.Notification;
import com.medassist.notifications.NotificationService;
import com.medassist.security.Role;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Plan changes and payment-provider events.
 *
 * <ul>
 *   <li>{@code manual} provider (default, free tier): an OWNER switches plan directly.
 *       Fine for a demo or for invoicing customers by hand.</li>
 *   <li>{@code razorpay}: the app creates a Razorpay subscription client-side; Razorpay then
 *       calls our webhook. The webhook is the only thing that changes paid state, it is
 *       verified with HMAC-SHA256 and de-duplicated by event id (Razorpay retries).</li>
 * </ul>
 */
@Slf4j
@Service
public class BillingService {

    private final SubscriptionRepository subscriptions;
    private final EntitlementService entitlements;
    private final NotificationService notifications;
    private final AuditService audit;
    private final AppProperties app;
    private final ObjectMapper mapper;
    private final Clock clock;

    public BillingService(SubscriptionRepository subscriptions, EntitlementService entitlements,
                          NotificationService notifications, AuditService audit, AppProperties app,
                          ObjectMapper mapper, Clock clock) {
        this.subscriptions = subscriptions;
        this.entitlements = entitlements;
        this.notifications = notifications;
        this.audit = audit;
        this.app = app;
        this.mapper = mapper;
        this.clock = clock;
    }

    /** Manual provider only: switch plan now, billed monthly from today. */
    @Transactional
    public Subscription changePlanManually(UUID orgId, UUID actor, Plan plan) {
        if (!"manual".equals(app.billing().provider())) {
            throw ApiException.conflict("USE_CHECKOUT", "Plans are changed through checkout on this server");
        }
        Subscription sub = subscriptions.findByOrgId(orgId).orElseThrow(() -> ApiException.notFound("Subscription"));
        Plan old = sub.effectivePlan(clock.instant());
        sub.setPlan(plan);
        sub.setStatus(Subscription.Status.ACTIVE);
        sub.setTrialEndsAt(null);
        sub.setCurrentPeriodEnd(plan == Plan.FREE ? null : clock.instant().plus(30, ChronoUnit.DAYS));
        audit.record(actor, "PLAN_CHANGED", "Subscription", sub.getId(), old + " -> " + plan);
        return sub;
    }

    /** Cancel at period end: the org keeps the paid plan until currentPeriodEnd, then drops to FREE. */
    @Transactional
    public Subscription cancel(UUID orgId, UUID actor) {
        Subscription sub = subscriptions.findByOrgId(orgId).orElseThrow(() -> ApiException.notFound("Subscription"));
        if (sub.getStatus() == Subscription.Status.TRIALING) {
            sub.setTrialEndsAt(clock.instant());
        }
        sub.setStatus(Subscription.Status.CANCELED);
        audit.record(actor, "PLAN_CANCELED", "Subscription", sub.getId(), null);
        return sub;
    }

    // ------------------------------------------------------------------ razorpay

    public boolean verifyRazorpaySignature(String body, String signature) {
        String secret = app.billing().razorpay().webhookSecret();
        if (secret == null || secret.isBlank() || signature == null) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
            // Constant-time compare, so the signature cannot be guessed byte by byte from timings.
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Applies a verified Razorpay subscription event. The org is found through
     * {@code notes.org_id} on the subscription (set when the app creates it).
     * Returns false for duplicates and events we ignore.
     */
    @Transactional
    public boolean handleRazorpayEvent(String eventId, String body) {
        JsonNode root;
        try {
            root = mapper.readTree(body);
        } catch (Exception e) {
            throw ApiException.badRequest("BAD_PAYLOAD", "Webhook body is not JSON");
        }
        String type = root.path("event").asString("");
        if (subscriptions.recordEventOnce("razorpay", eventId, type) == 0) {
            return false;   // already processed: Razorpay retries until it gets a 2xx
        }
        JsonNode s = root.at("/payload/subscription/entity");
        if (s.isMissingNode()) {
            return false;
        }
        String providerRef = s.path("id").asString(null);
        String orgIdText = s.at("/notes/org_id").asString(null);
        Subscription sub = (providerRef == null ? null : subscriptions.findByProviderRef(providerRef).orElse(null));
        if (sub == null && orgIdText != null) {
            sub = subscriptions.findByOrgId(UUID.fromString(orgIdText)).orElse(null);
        }
        if (sub == null) {
            log.warn("Razorpay event {} for unknown subscription {}", type, providerRef);
            return false;
        }
        sub.setProvider("razorpay");
        sub.setProviderRef(providerRef);
        Plan plan = planForRazorpayPlanId(s.path("plan_id").asString(""));
        long periodEnd = s.path("current_end").asLong(0);
        switch (type) {
            case "subscription.activated", "subscription.charged", "subscription.resumed" -> {
                if (plan != null) {
                    sub.setPlan(plan);
                }
                sub.setStatus(Subscription.Status.ACTIVE);
                sub.setTrialEndsAt(null);
                if (periodEnd > 0) {
                    sub.setCurrentPeriodEnd(Instant.ofEpochSecond(periodEnd));
                }
            }
            case "subscription.pending", "subscription.halted" -> {
                sub.setStatus(Subscription.Status.PAST_DUE);
                notifications.toStaff(sub.getOrgId(), List.of(Role.OWNER), "Payment failed",
                        "We could not renew your MedAssist plan. Update your payment method within 7 days.",
                        Notification.Category.BILLING);
            }
            case "subscription.cancelled", "subscription.completed" -> sub.setStatus(Subscription.Status.CANCELED);
            default -> {
                return false;
            }
        }
        audit.recordFor(sub.getOrgId(), null, "BILLING_" + type.toUpperCase().replace('.', '_'), "Subscription",
                sub.getId(), providerRef);
        return true;
    }

    private Plan planForRazorpayPlanId(String razorpayPlanId) {
        Map<String, String> ids = app.billing().razorpay().planIds();
        if (ids == null || razorpayPlanId.isBlank()) {
            return null;
        }
        return ids.entrySet().stream().filter(e -> razorpayPlanId.equals(e.getValue()))
                .map(e -> Plan.valueOf(e.getKey())).findFirst().orElse(null);
    }

    public EntitlementService entitlements() {
        return entitlements;
    }
}
