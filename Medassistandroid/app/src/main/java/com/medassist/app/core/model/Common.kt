package com.medassist.app.core.model

import kotlinx.serialization.Serializable

@Serializable
data class PageResponse<T>(
    val content: List<T> = emptyList(),
    val page: Int = 0,
    val size: Int = 0,
    val totalElements: Long = 0,
    val totalPages: Int = 0,
)

enum class Role {
    OWNER, ADMIN, PHARMACIST, CASHIER;

    /** Mirrors the server's hierarchy: OWNER > ADMIN > PHARMACIST > CASHIER. */
    fun atLeast(other: Role): Boolean = ordinal <= other.ordinal
}

enum class DrugSchedule {
    OTC, G, H, H1, X;

    val requiresPrescription: Boolean get() = this == H || this == H1 || this == X
}

enum class PaymentMode { CASH, UPI, CARD, CREDIT }
