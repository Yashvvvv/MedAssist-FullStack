package com.medassist.app.core.model

import kotlinx.serialization.Serializable

enum class OrderStatus {
    PLACED, ACCEPTED, READY, COMPLETED, REJECTED, CANCELLED;

    val isOpen: Boolean get() = this == PLACED || this == ACCEPTED || this == READY
}

enum class Fulfilment { PICKUP, DELIVERY }

@Serializable
data class OrderLineRequest(val productId: String, val quantity: Int)

@Serializable
data class PlaceOrderRequest(
    val branchId: String,
    val fulfilment: Fulfilment,
    val contactPhone: String,
    val lines: List<OrderLineRequest>,
    val deliveryAddress: String? = null,
    val notes: String? = null,
)

@Serializable
data class OrderLineView(val productId: String, val productName: String, val quantity: Int, val unitPrice: Money)

@Serializable
data class OrderView(
    val id: String,
    val orderNo: String,
    val branchId: String,
    val branchName: String? = null,
    val status: OrderStatus,
    val fulfilment: Fulfilment,
    val deliveryAddress: String? = null,
    val contactPhone: String = "",
    val notes: String? = null,
    val prescriptionId: String? = null,
    val estimatedTotal: Money,
    val rejectionReason: String? = null,
    val saleId: String? = null,
    val createdAt: String,
    val updatedAt: String? = null,
    val lines: List<OrderLineView> = emptyList(),
)

@Serializable
data class RejectRequest(val reason: String)

@Serializable
data class CompleteRequest(val paymentMode: PaymentMode)
