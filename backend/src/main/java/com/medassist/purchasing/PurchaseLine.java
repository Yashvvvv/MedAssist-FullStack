package com.medassist.purchasing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Child of a Purchase; tenant-safe through its parent. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "purchase_lines")
public class PurchaseLine {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_id")
    private Purchase purchase;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "batch_id", nullable = false)
    private UUID batchId;

    @Column(name = "batch_no", nullable = false)
    private String batchNo;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "free_quantity", nullable = false)
    private int freeQuantity;

    /** Per unit, before GST (how distributor invoices are written). */
    @Column(name = "purchase_price", nullable = false)
    private BigDecimal purchasePrice;

    @Column(nullable = false)
    private BigDecimal mrp;

    @Column(name = "gst_rate", nullable = false)
    private BigDecimal gstRate;

    @Column(name = "line_total", nullable = false)
    private BigDecimal lineTotal;
}
