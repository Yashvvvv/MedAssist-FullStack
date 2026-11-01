package com.medassist.inventory;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class InventoryDtos {

    private InventoryDtos() {
    }

    /** Opening stock / manual receipt outside a purchase invoice. */
    public record ReceiveStockRequest(
            @NotNull UUID branchId,
            @NotNull UUID productId,
            @NotBlank @Size(max = 40) String batchNo,
            @NotNull @Future LocalDate expiryDate,
            @Positive int quantity,
            @NotNull @DecimalMin("0") BigDecimal purchasePrice,
            BigDecimal mrp) {
    }

    public record AdjustStockRequest(
            @NotNull Integer delta,
            @NotBlank @Size(max = 255) String reason) {
    }

    public record TransferRequest(
            @NotNull UUID fromBranchId,
            @NotNull UUID toBranchId,
            @NotNull UUID productId,
            @Min(1) int quantity,
            @Size(max = 255) String note) {
    }

    public record BatchView(UUID id, UUID branchId, UUID productId, String batchNo, LocalDate expiryDate, int quantity,
                            BigDecimal purchasePrice, BigDecimal mrp, Instant receivedAt, boolean expired,
                            long daysToExpiry) {
    }

    public record StockLevelView(UUID productId, String productName, String sku, int reorderLevel, long quantity,
                                 LocalDate nearestExpiry, boolean belowReorderLevel) {
    }

    public record MovementView(UUID id, UUID branchId, UUID batchId, String type, int quantity, int balanceAfter,
                               String referenceType, UUID referenceId, String note, UUID createdBy, Instant createdAt) {
        static MovementView of(StockMovement m) {
            return new MovementView(m.getId(), m.getBranchId(), m.getBatchId(), m.getType().name(), m.getQuantity(),
                    m.getBalanceAfter(), m.getReferenceType(), m.getReferenceId(), m.getNote(), m.getCreatedBy(),
                    m.getCreatedAt());
        }
    }

    public record ExpiringBatchView(BatchView batch, String productName, BigDecimal valueAtCost) {
    }

    public record TransferResult(int quantity, int batchesMoved) {
    }

    public record WriteOffResult(int batches, int units) {
    }
}
