package com.medassist.app.core.model

import kotlinx.serialization.Serializable

@Serializable
data class WalkInCustomer(val name: String, val phone: String)

@Serializable
data class SaleLineRequest(
    val productId: String,
    val quantity: Int,
    val discountPct: Money? = null,
    val refillAfterDays: Int? = null,
)

@Serializable
data class CreateSaleRequest(
    val branchId: String,
    val paymentMode: PaymentMode,
    val lines: List<SaleLineRequest>,
    val customerId: String? = null,
    val newCustomer: WalkInCustomer? = null,
    val prescriptionId: String? = null,
)

@Serializable
data class SaleLineView(
    val id: String,
    val productId: String,
    val productName: String,
    val batchNo: String? = null,
    val expiryDate: String? = null,
    val schedule: DrugSchedule = DrugSchedule.OTC,
    val quantity: Int,
    val returnedQty: Int = 0,
    val unitPrice: Money,
    val discountPct: Money? = null,
    val gstRate: Money,
    val taxableValue: Money,
    val cgst: Money,
    val sgst: Money,
    val lineTotal: Money,
)

@Serializable
data class SaleView(
    val id: String,
    val invoiceNo: String,
    val branchId: String,
    val customerId: String? = null,
    val customerName: String? = null,
    val status: String = "",
    val paymentMode: String = "",
    val subtotal: Money,
    val discountTotal: Money,
    val taxTotal: Money,
    val total: Money,
    val createdAt: String,
    val lines: List<SaleLineView> = emptyList(),
    val warnings: List<String> = emptyList(),
)

@Serializable
data class CustomerView(
    val id: String,
    val name: String,
    val phone: String? = null,
    val email: String? = null,
    val allergies: String? = null,
    val chronicConditions: String? = null,
    val appUser: Boolean = false,
)

@Serializable
data class PrescriptionView(
    val id: String,
    val customerId: String? = null,
    val doctorName: String? = null,
    val doctorRegNo: String? = null,
    val status: String = "",
    val createdAt: String? = null,
)

@Serializable
data class PrescriptionMeta(val customerId: String? = null, val doctorName: String? = null, val doctorRegNo: String? = null)
