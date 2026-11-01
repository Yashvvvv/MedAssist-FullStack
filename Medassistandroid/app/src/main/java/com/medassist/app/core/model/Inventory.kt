package com.medassist.app.core.model

import kotlinx.serialization.Serializable

@Serializable
data class ReceiveStockRequest(
    val branchId: String,
    val productId: String,
    val batchNo: String,
    val expiryDate: String,
    val quantity: Int,
    val purchasePrice: Money,
    val mrp: Money? = null,
)

@Serializable
data class BatchView(
    val id: String,
    val branchId: String,
    val productId: String,
    val batchNo: String,
    val expiryDate: String,
    val quantity: Int,
    val purchasePrice: Money,
    val mrp: Money,
    val expired: Boolean = false,
    val daysToExpiry: Long = 0,
)

@Serializable
data class StockLevelView(
    val productId: String,
    val productName: String,
    val sku: String? = null,
    val reorderLevel: Int = 0,
    val quantity: Long = 0,
    val nearestExpiry: String? = null,
    val belowReorderLevel: Boolean = false,
)

@Serializable
data class ExpiringBatchView(val batch: BatchView, val productName: String, val valueAtCost: Money)
