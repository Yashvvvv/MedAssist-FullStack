package com.medassist.purchasing;

import com.medassist.tenancy.dto.OrgDtos;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class PurchaseDtos {

    private PurchaseDtos() {
    }

    public record SupplierRequest(
            @NotBlank @Size(max = 150) String name,
            @Pattern(regexp = OrgDtos.GSTIN_RULE, message = "must be a valid 15-character GSTIN") String gstin,
            @Size(max = 60) String drugLicenseNo,
            @Size(max = 20) String phone,
            @Email String email,
            @Size(max = 255) String address) {
    }

    public record SupplierView(UUID id, String name, String gstin, String drugLicenseNo, String phone, String email,
                               String address) {
        static SupplierView of(Supplier s) {
            return new SupplierView(s.getId(), s.getName(), s.getGstin(), s.getDrugLicenseNo(), s.getPhone(),
                    s.getEmail(), s.getAddress());
        }
    }

    public record PurchaseLineRequest(
            @NotNull UUID productId,
            @NotBlank @Size(max = 40) String batchNo,
            @NotNull LocalDate expiryDate,
            @Positive int quantity,
            @Min(0) int freeQuantity,
            @NotNull @DecimalMin("0") BigDecimal purchasePrice,
            BigDecimal mrp,
            BigDecimal gstRate) {
    }

    public record PurchaseRequest(
            @NotNull UUID branchId,
            @NotNull UUID supplierId,
            @NotBlank @Size(max = 60) String invoiceNo,
            @NotNull @PastOrPresent LocalDate invoiceDate,
            @Size(max = 255) String notes,
            @NotEmpty @Size(max = 200) List<@Valid PurchaseLineRequest> lines) {
    }

    public record PurchaseLineView(UUID id, UUID productId, UUID batchId, String batchNo, LocalDate expiryDate,
                                   int quantity, int freeQuantity, BigDecimal purchasePrice, BigDecimal mrp,
                                   BigDecimal gstRate, BigDecimal lineTotal) {
    }

    public record PurchaseView(UUID id, UUID branchId, UUID supplierId, String supplierName, String invoiceNo,
                               LocalDate invoiceDate, BigDecimal subtotal, BigDecimal taxTotal, BigDecimal total,
                               String notes, Instant createdAt, List<PurchaseLineView> lines) {
    }
}
