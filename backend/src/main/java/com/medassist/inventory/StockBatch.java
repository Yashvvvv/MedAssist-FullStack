package com.medassist.inventory;

import com.medassist.common.persistence.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A quantity of one product, from one manufacturing batch, at one branch. Medicines are
 * tracked per batch because expiry, recalls and MRP differ by batch.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "stock_batches")
public class StockBatch extends TenantEntity {

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "batch_no", nullable = false)
    private String batchNo;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "purchase_price", nullable = false)
    private BigDecimal purchasePrice;

    @Column(nullable = false)
    private BigDecimal mrp;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Version
    private long version;

    public boolean isExpired(LocalDate today) {
        return expiryDate.isBefore(today);
    }
}
