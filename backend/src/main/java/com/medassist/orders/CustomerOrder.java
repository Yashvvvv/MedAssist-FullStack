package com.medassist.orders;

import com.medassist.common.persistence.TenantEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * An order a patient places from the app at one pharmacy branch (reserve for pickup or
 * home delivery). It becomes a real sale only when staff complete it, so stock is
 * deducted at handover, not at order time.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "orders")
public class CustomerOrder extends TenantEntity {

    public enum Status {
        PLACED, ACCEPTED, READY, COMPLETED, REJECTED, CANCELLED;

        /** Allowed transitions; anything else is a 409. */
        public Set<Status> next() {
            return switch (this) {
                case PLACED -> EnumSet.of(ACCEPTED, REJECTED, CANCELLED);
                case ACCEPTED -> EnumSet.of(READY, REJECTED, COMPLETED);
                case READY -> EnumSet.of(COMPLETED, REJECTED);
                default -> EnumSet.noneOf(Status.class);
            };
        }
    }

    public enum Fulfilment { PICKUP, DELIVERY }

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "placed_by", nullable = false)
    private UUID placedBy;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "order_no", nullable = false)
    private String orderNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PLACED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Fulfilment fulfilment;

    @Column(name = "delivery_address")
    private String deliveryAddress;

    @Column(name = "contact_phone", nullable = false)
    private String contactPhone;

    private String notes;

    @Column(name = "prescription_id")
    private UUID prescriptionId;

    @Column(name = "estimated_total", nullable = false)
    private BigDecimal estimatedTotal;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "sale_id")
    private UUID saleId;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<Line> lines = new ArrayList<>();

    public void addLine(Line l) {
        l.setOrder(this);
        lines.add(l);
    }

    public void moveTo(Status target) {
        if (!status.next().contains(target)) {
            throw com.medassist.common.error.ApiException.conflict("INVALID_ORDER_TRANSITION",
                    "Order is " + status + " and cannot become " + target);
        }
        status = target;
    }

    @Override
    protected void touch() {
        updatedAt = Instant.now();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @Entity
    @Table(name = "order_lines")
    public static class Line {

        @Id
        @GeneratedValue
        @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
        private UUID id;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "order_id")
        private CustomerOrder order;

        @Column(name = "product_id", nullable = false)
        private UUID productId;

        @Column(name = "product_name", nullable = false)
        private String productName;

        @Column(nullable = false)
        private int quantity;

        @Column(name = "unit_price", nullable = false)
        private BigDecimal unitPrice;
    }
}
