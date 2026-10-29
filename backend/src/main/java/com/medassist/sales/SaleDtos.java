package com.medassist.sales;

import com.medassist.catalog.DrugSchedule;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class SaleDtos {

    private SaleDtos() {
    }

    public record WalkInCustomer(@NotBlank @Size(max = 120) String name, @NotBlank @Size(max = 20) String phone) {
    }

    public record SaleLineRequest(
            @NotNull UUID productId,
            /* Optional: sell from this exact batch; otherwise FEFO picks. */
            UUID batchId,
            @Positive @Max(10_000) int quantity,
            @DecimalMin("0") @DecimalMax("50") BigDecimal discountPct,
            /* Optional override of the product's selling price; never above the batch MRP. */
            @DecimalMin("0.01") BigDecimal unitPrice,
            /* Optional: remind the customer to refill after this many days. */
            @Min(1) @Max(365) Integer refillAfterDays) {
    }

    public record CreateSaleRequest(
            @NotNull UUID branchId,
            UUID customerId,
            @Valid WalkInCustomer newCustomer,
            UUID prescriptionId,
            @NotNull Sale.PaymentMode paymentMode,
            @NotEmpty @Size(max = 100) List<@Valid SaleLineRequest> lines) {
    }

    public record SaleLineView(UUID id, UUID productId, String productName, UUID batchId, String batchNo,
                               LocalDate expiryDate, String hsnCode, DrugSchedule schedule, int quantity,
                               int returnedQty, BigDecimal unitPrice, BigDecimal discountPct, BigDecimal gstRate,
                               BigDecimal taxableValue, BigDecimal cgst, BigDecimal sgst, BigDecimal lineTotal) {
    }

    public record SaleView(UUID id, String invoiceNo, UUID branchId, UUID customerId, String customerName,
                           UUID prescriptionId, String status, String paymentMode, BigDecimal subtotal,
                           BigDecimal discountTotal, BigDecimal taxTotal, BigDecimal total, Instant createdAt,
                           UUID createdBy, List<SaleLineView> lines, List<String> warnings) {
    }

    public record ReturnLineRequest(@NotNull UUID saleLineId, @Positive int quantity) {
    }

    public record ReturnRequest(@NotBlank @Size(max = 255) String reason,
                                @NotEmpty List<@Valid ReturnLineRequest> lines) {
    }

    public record ReturnView(UUID id, String creditNoteNo, UUID saleId, String reason, BigDecimal refundTotal,
                             BigDecimal taxableTotal, BigDecimal taxTotal, Instant createdAt) {
    }
}
