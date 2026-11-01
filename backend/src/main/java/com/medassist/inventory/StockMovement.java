package com.medassist.inventory;

import com.medassist.common.persistence.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Append-only stock ledger. The batch quantity is a cached balance; this table is the
 * truth that explains it ("why do we have 3 strips of Pan 40 and not 5?").
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "stock_movements")
public class StockMovement extends TenantEntity {

    public enum Type {
        OPENING, PURCHASE, SALE, SALE_RETURN, ADJUSTMENT, TRANSFER_IN, TRANSFER_OUT, EXPIRED_WRITE_OFF
    }

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "batch_id", nullable = false)
    private UUID batchId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    /** Signed: positive adds stock, negative removes it. */
    @Column(nullable = false)
    private int quantity;

    @Column(name = "balance_after", nullable = false)
    private int balanceAfter;

    @Column(name = "reference_type")
    private String referenceType;

    @Column(name = "reference_id")
    private UUID referenceId;

    private String note;

    @Column(name = "created_by")
    private UUID createdBy;
}
