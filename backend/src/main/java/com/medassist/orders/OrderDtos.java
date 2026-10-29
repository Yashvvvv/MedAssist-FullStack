package com.medassist.orders;

import com.medassist.identity.dto.AuthDtos;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class OrderDtos {

    private OrderDtos() {
    }

    public record OrderLineRequest(@NotNull UUID productId, @Positive @Max(100) int quantity) {
    }

    public record PlaceOrderRequest(
            @NotNull UUID branchId,
            @NotNull CustomerOrder.Fulfilment fulfilment,
            @Size(max = 255) String deliveryAddress,
            @NotBlank @Pattern(regexp = AuthDtos.PHONE_RULE, message = "must be a valid phone number") String contactPhone,
            @Size(max = 500) String notes,
            @NotEmpty @Size(max = 30) List<@Valid OrderLineRequest> lines) {
    }

    public record RejectRequest(@NotBlank @Size(max = 255) String reason) {
    }

    public record CompleteRequest(@NotNull com.medassist.sales.Sale.PaymentMode paymentMode) {
    }

    public record OrderLineView(UUID productId, String productName, int quantity, BigDecimal unitPrice) {
    }

    public record OrderView(UUID id, String orderNo, UUID orgId, UUID branchId, String branchName, String status,
                            String fulfilment, String deliveryAddress, String contactPhone, String notes,
                            UUID prescriptionId, BigDecimal estimatedTotal, String rejectionReason, UUID saleId,
                            Instant createdAt, Instant updatedAt, List<OrderLineView> lines) {
    }
}
