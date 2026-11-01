package com.medassist.app.core.model

import kotlinx.serialization.Serializable

@Serializable
data class PharmacyView(
    val branchId: String,
    val orgId: String? = null,
    val name: String,
    val pharmacyName: String? = null,
    val addressLine: String? = null,
    val city: String? = null,
    val pincode: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val phone: String? = null,
    val openingHours: String? = null,
    val open24x7: Boolean = false,
    val deliveryAvailable: Boolean = false,
    val distanceKm: Double? = null,
)

enum class Availability { IN_STOCK, LOW_STOCK, OUT_OF_STOCK }

@Serializable
data class PublicProductView(
    val productId: String,
    val medicineId: String? = null,
    val name: String,
    val saltComposition: String? = null,
    val manufacturer: String? = null,
    val packSize: String? = null,
    val prescriptionRequired: Boolean = false,
    val price: Money,
    val mrp: Money,
    val availability: Availability,
)

@Serializable
data class WhoHasResult(
    val branchId: String,
    val name: String,
    val pharmacyName: String? = null,
    val addressLine: String? = null,
    val city: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val phone: String? = null,
    val deliveryAvailable: Boolean = false,
    val productId: String,
    val productName: String,
    val price: Money,
    val distanceKm: Double = 0.0,
)
