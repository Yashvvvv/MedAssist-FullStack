package com.medassist.jobs;

import com.medassist.billing.EntitlementService;
import com.medassist.billing.Plan;
import com.medassist.catalog.ProductRepository;
import com.medassist.common.tenant.TenantContext;
import com.medassist.common.tenant.TenantScope;
import com.medassist.common.time.BusinessTime;
import com.medassist.customers.CustomerRepository;
import com.medassist.identity.RefreshTokenRepository;
import com.medassist.notifications.Notification;
import com.medassist.notifications.NotificationService;
import com.medassist.refills.RefillSchedule;
import com.medassist.refills.RefillScheduleRepository;
import com.medassist.security.Role;
import com.medassist.tenancy.OrganizationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Background jobs. Each one reads across all pharmacies as ROOT, then does the writes
 * for each pharmacy inside that pharmacy's tenant (one transaction per org), so one
 * pharmacy's bad data can never block or leak into another's.
 *
 * <p>Every job is idempotent: running it twice on the same day sends nothing twice. That
 * matters on the free tier, where the instance sleeps and a missed run may happen late.</p>
 */
@Slf4j
@Component
public class ScheduledJobs {

    /** Remind this many days before the refill is due. */
    static final int REMIND_DAYS_AHEAD = 2;

    private final RefillScheduleRepository refills;
    private final CustomerRepository customers;
    private final ProductRepository products;
    private final OrganizationRepository organizations;
    private final RefreshTokenRepository refreshTokens;
    private final EntitlementService entitlements;
    private final NotificationService notifications;
    private final TenantScope tenantScope;
    private final BusinessTime time;
    private final JdbcClient jdbc;

    public ScheduledJobs(RefillScheduleRepository refills, CustomerRepository customers, ProductRepository products,
                         OrganizationRepository organizations, RefreshTokenRepository refreshTokens,
                         EntitlementService entitlements, NotificationService notifications, TenantScope tenantScope,
                         BusinessTime time, DataSource dataSource) {
        this.refills = refills;
        this.customers = customers;
        this.products = products;
        this.organizations = organizations;
        this.refreshTokens = refreshTokens;
        this.entitlements = entitlements;
        this.notifications = notifications;
        this.tenantScope = tenantScope;
        this.time = time;
        this.jdbc = JdbcClient.create(dataSource);
    }

    // ------------------------------------------------------------------ refills

    @Scheduled(cron = "${app.jobs.refill-reminder-cron}", zone = "${app.business-zone}")
    public void refillReminderJob() {
        int sent = sendRefillReminders(time.today());
        log.info("Refill reminders sent: {}", sent);
    }

    /** One WhatsApp reminder per refill cycle, to customers whose medicine runs out within 2 days. */
    public int sendRefillReminders(LocalDate today) {
        List<RefillSchedule> due = tenantScope.inTenant(TenantContext.ROOT,
                () -> refills.needingReminder(today.plusDays(REMIND_DAYS_AHEAD)));
        Map<UUID, List<UUID>> byOrg = new LinkedHashMap<>();
        due.forEach(r -> byOrg.computeIfAbsent(r.getOrgId(), k -> new ArrayList<>()).add(r.getId()));
        int sent = 0;
        for (Map.Entry<UUID, List<UUID>> e : byOrg.entrySet()) {
            UUID org = e.getKey();
            if (!entitlements.planOf(org).has(Plan.Feature.REFILL_REMINDERS)) {
                continue;
            }
            try {
                sent += tenantScope.inTenant(org, () -> remindOrg(org, e.getValue()));
            } catch (RuntimeException ex) {
                log.error("Refill reminders failed for org {}", org, ex);
            }
        }
        return sent;
    }

    private int remindOrg(UUID org, List<UUID> scheduleIds) {
        String pharmacy = organizations.findById(org).map(o -> o.getName()).orElse("your pharmacy");
        int sent = 0;
        for (RefillSchedule r : refills.findAllById(scheduleIds)) {
            var customer = customers.findById(r.getCustomerId()).orElse(null);
            var product = products.findById(r.getProductId()).orElse(null);
            if (customer == null || product == null) {
                continue;
            }
            notifications.toCustomer(org, customer.getId(), customer.getPhone(),
                    "Time to refill " + product.getName(),
                    "Hi " + customer.getName() + ", your " + product.getName() + " is due for a refill on "
                            + r.getNextDueDate() + ". Reply YES and " + pharmacy + " will keep "
                            + r.getQuantity() + " ready for you.",
                    Notification.Category.REFILL_DUE);
            r.setRemindedForDueDate(r.getNextDueDate());
            sent++;
        }
        return sent;
    }

    // ------------------------------------------------------------------ expiry

    @Scheduled(cron = "${app.jobs.expiry-alert-cron}", zone = "${app.business-zone}")
    public void expiryAlertJob() {
        log.info("Expiry alerts sent to {} pharmacies", sendExpiryAlerts(time.today()));
    }

    /**
     * Weekly digest to owners/admins/pharmacists: what expires in the next 30 days and what
     * it is worth at cost, so they can return it to the distributor before it is dead stock.
     */
    public int sendExpiryAlerts(LocalDate today) {
        record Row(UUID org, long batches, BigDecimal value) {
        }
        List<Row> rows = jdbc.sql("""
                        select org_id, count(*), sum(quantity * purchase_price) from stock_batches
                        where quantity > 0 and expiry_date between :today and :until
                        group by org_id""")
                .param("today", Date.valueOf(today)).param("until", Date.valueOf(today.plusDays(30)))
                .query((rs, i) -> new Row(rs.getObject(1, UUID.class), rs.getLong(2), rs.getBigDecimal(3)))
                .list();
        for (Row r : rows) {
            tenantScope.runInTenant(r.org(), () -> notifications.toStaff(r.org(),
                    List.of(Role.OWNER, Role.ADMIN, Role.PHARMACIST),
                    r.batches() + " batch(es) expire within 30 days",
                    "Stock worth ₹" + r.value().setScale(0, RoundingMode.HALF_UP)
                            + " at cost expires soon. Open Inventory > Expiring to return or discount it.",
                    Notification.Category.EXPIRY_ALERT));
        }
        return rows.size();
    }

    // ------------------------------------------------------------------ housekeeping

    @Scheduled(cron = "0 15 3 * * *", zone = "${app.business-zone}")
    public void cleanup() {
        int tokens = tenantScope.inTenant(TenantContext.ROOT,
                () -> refreshTokens.deleteExpiredBefore(time.now().minus(Duration.ofDays(7))));
        int otts = jdbc.sql("delete from one_time_tokens where expires_at < now() - interval '7 days'").update();
        log.info("Cleanup: {} refresh tokens, {} one-time tokens removed", tokens, otts);
    }
}
