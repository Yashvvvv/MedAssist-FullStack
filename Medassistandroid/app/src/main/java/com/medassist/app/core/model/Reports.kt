package com.medassist.app.core.model

import kotlinx.serialization.Serializable

@Serializable
data class DayPoint(val date: String, val invoices: Long, val revenue: Money)

@Serializable
data class Dashboard(
    val today: String,
    val invoicesToday: Long,
    val salesToday: Money,
    val salesMonthToDate: Money,
    val grossProfitMonthToDate: Money,
    val lowStockProducts: Long,
    val batchesExpiringIn30Days: Long,
    val valueExpiringIn30Days: Money,
    val stockValueAtCost: Money,
    val openOrders: Long,
    val refillsDueIn3Days: Long,
    val last7Days: List<DayPoint> = emptyList(),
)

@Serializable
data class TopProduct(val productId: String, val name: String, val quantity: Long, val revenue: Money, val grossProfit: Money)

@Serializable
data class PaymentSplit(val paymentMode: String, val invoices: Long, val amount: Money)

@Serializable
data class SalesSummary(
    val from: String,
    val to: String,
    val invoices: Long,
    val grossSales: Money,
    val discounts: Money,
    val tax: Money,
    val returns: Money,
    val netSales: Money,
    val costOfGoods: Money,
    val grossProfit: Money,
    val averageBill: Money,
    val byDay: List<DayPoint> = emptyList(),
    val byPaymentMode: List<PaymentSplit> = emptyList(),
    val topProducts: List<TopProduct> = emptyList(),
)

@Serializable
data class GstRateRow(
    val rate: Money,
    val taxable: Money,
    val cgst: Money,
    val sgst: Money,
    val returnedTaxable: Money,
    val returnedTax: Money,
    val netTaxable: Money,
    val netTax: Money,
)

@Serializable
data class GstSummary(
    val from: String,
    val to: String,
    val gstin: String? = null,
    val byRate: List<GstRateRow> = emptyList(),
    val totalNetTaxable: Money,
    val totalNetTax: Money,
)
