package com.medassist.refills;

import com.medassist.common.persistence.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * "Mrs Sharma buys Telma 40 every 30 days." Created at billing; the daily refill job
 * reminds the customer shortly before {@code nextDueDate}, and the next purchase of the
 * same product pushes the date forward. Chronic-care repeat business is the single
 * biggest retention lever for a neighbourhood pharmacy.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "refill_schedules")
public class RefillSchedule extends TenantEntity {

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "interval_days", nullable = false)
    private int intervalDays;

    @Column(name = "next_due_date", nullable = false)
    private LocalDate nextDueDate;

    /** The due date we last sent a reminder for: one reminder per refill cycle. */
    @Column(name = "last_reminded_on")
    private LocalDate remindedForDueDate;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Override
    protected void touch() {
        updatedAt = Instant.now();
    }
}
