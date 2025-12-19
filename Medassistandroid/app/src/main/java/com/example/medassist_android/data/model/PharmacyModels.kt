package com.example.medassist_android.data.model

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Json

// Basic Pharmacy model matching backend Pharmacy entity
@JsonClass(generateAdapter = true)
data class Pharmacy(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "address") val address: String,
    @Json(name = "city") val city: String?,
    @Json(name = "state") val state: String?,
    @Json(name = "zipCode") val zipCode: String?,
    @Json(name = "country") val country: String?,
    @Json(name = "phoneNumber") val phoneNumber: String?,
    @Json(name = "emailAddress") val emailAddress: String?,
    @Json(name = "operatingHours") val operatingHours: String?,
    @Json(name = "emergencyHours") val emergencyHours: String?,
    @Json(name = "websiteUrl") val websiteUrl: String?,
    @Json(name = "is24Hours") val is24Hours: Boolean = false,
    @Json(name = "acceptsInsurance") val acceptsInsurance: Boolean = false,
    @Json(name = "hasDriveThrough") val hasDriveThrough: Boolean = false,
    @Json(name = "hasDelivery") val hasDelivery: Boolean = false,
    @Json(name = "hasConsultation") val hasConsultation: Boolean = false,
    @Json(name = "services") val services: List<String>?,
    @Json(name = "latitude") val latitude: Double?,
    @Json(name = "longitude") val longitude: Double?,
    @Json(name = "licenseNumber") val licenseNumber: String?,
    @Json(name = "managerName") val managerName: String?,
    @Json(name = "pharmacistName") val pharmacistName: String?,
    @Json(name = "chainName") val chainName: String?,
    @Json(name = "rating") val rating: Double?,
    @Json(name = "isActive") val isActive: Boolean = true,
    @Json(name = "createdAt") val createdAt: String?,
    @Json(name = "updatedAt") val updatedAt: String?,
    // Computed fields from location queries (not from backend directly)
    val distance: Double? = null,
    val isCurrentlyOpen: Boolean? = null
)

// PharmacyLocationResponse matching backend DTO (snake_case JSON)
@JsonClass(generateAdapter = true)
data class PharmacyLocationResponse(
    @Json(name = "pharmacy_id") val pharmacyId: Long?,
    @Json(name = "name") val name: String,
    @Json(name = "address") val address: String,
    @Json(name = "city") val city: String?,
    @Json(name = "state") val state: String?,
    @Json(name = "zip_code") val zipCode: String?,
    @Json(name = "phone_number") val phoneNumber: String?,
    @Json(name = "email_address") val emailAddress: String?,
    @Json(name = "website_url") val websiteUrl: String?,
    @Json(name = "latitude") val latitude: Double?,
    @Json(name = "longitude") val longitude: Double?,
    @Json(name = "distance_km") val distanceKm: Double?,
    @Json(name = "travel_time_minutes") val travelTimeMinutes: Int?,
    @Json(name = "operating_hours") val operatingHours: String?,
    @Json(name = "emergency_hours") val emergencyHours: String?,
    @Json(name = "is_open_now") val isOpenNow: Boolean?,
    @Json(name = "is_24_hours") val is24Hours: Boolean?,
    @Json(name = "accepts_insurance") val acceptsInsurance: Boolean?,
    @Json(name = "has_drive_through") val hasDriveThrough: Boolean?,
    @Json(name = "has_delivery") val hasDelivery: Boolean?,
    @Json(name = "has_consultation") val hasConsultation: Boolean?,
    @Json(name = "services") val services: List<String>?,
    @Json(name = "chain_name") val chainName: String?,
    @Json(name = "manager_name") val managerName: String?,
    @Json(name = "pharmacist_name") val pharmacistName: String?,
    @Json(name = "rating") val rating: Double?,
    @Json(name = "medicine_availability") val medicineAvailability: MedicineAvailability?,
    @Json(name = "directions_url") val directionsUrl: String?,
    @Json(name = "place_id") val placeId: String?,
    @Json(name = "response_timestamp") val responseTimestamp: String?
)

@JsonClass(generateAdapter = true)
data class MedicineAvailability(
    @Json(name = "medicine_name") val medicineName: String?,
    @Json(name = "likely_available") val likelyAvailable: Boolean?,
    @Json(name = "availability_confidence") val availabilityConfidence: Double?,
    @Json(name = "estimated_stock_level") val estimatedStockLevel: String?, // HIGH, MEDIUM, LOW, OUT_OF_STOCK, UNKNOWN
    @Json(name = "last_updated") val lastUpdated: String?
)

// Request model matching backend PharmacyLocationRequest (snake_case JSON)
@JsonClass(generateAdapter = true)
data class NearbyPharmaciesRequest(
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double,
    @Json(name = "radius_km") val radiusKm: Double = 10.0,
    @Json(name = "max_results") val maxResults: Int = 20,
    @Json(name = "open_now") val openNow: Boolean? = null,
    @Json(name = "has_delivery") val hasDelivery: Boolean? = null,
    @Json(name = "has_drive_through") val hasDriveThrough: Boolean? = null,
    @Json(name = "accepts_insurance") val acceptsInsurance: Boolean? = null,
    @Json(name = "is_24_hours") val is24Hours: Boolean? = null,
    @Json(name = "chain_name") val chainName: String? = null,
    @Json(name = "services") val services: List<String>? = null,
    @Json(name = "medicine_name") val medicineName: String? = null,
    @Json(name = "sort_by") val sortBy: String = "DISTANCE" // DISTANCE, RATING, NAME, OPENING_HOURS
)

@JsonClass(generateAdapter = true)
data class CreatePharmacyRequest(
    @Json(name = "name") val name: String,
    @Json(name = "address") val address: String,
    @Json(name = "city") val city: String?,
    @Json(name = "state") val state: String?,
    @Json(name = "zipCode") val zipCode: String?,
    @Json(name = "country") val country: String?,
    @Json(name = "phoneNumber") val phoneNumber: String?,
    @Json(name = "emailAddress") val emailAddress: String?,
    @Json(name = "operatingHours") val operatingHours: String?,
    @Json(name = "emergencyHours") val emergencyHours: String?,
    @Json(name = "websiteUrl") val websiteUrl: String?,
    @Json(name = "is24Hours") val is24Hours: Boolean = false,
    @Json(name = "acceptsInsurance") val acceptsInsurance: Boolean = false,
    @Json(name = "hasDriveThrough") val hasDriveThrough: Boolean = false,
    @Json(name = "hasDelivery") val hasDelivery: Boolean = false,
    @Json(name = "hasConsultation") val hasConsultation: Boolean = false,
    @Json(name = "services") val services: List<String>?,
    @Json(name = "latitude") val latitude: Double?,
    @Json(name = "longitude") val longitude: Double?,
    @Json(name = "licenseNumber") val licenseNumber: String?,
    @Json(name = "managerName") val managerName: String?,
    @Json(name = "pharmacistName") val pharmacistName: String?,
    @Json(name = "chainName") val chainName: String?
)

@JsonClass(generateAdapter = true)
data class PharmacySearchRequest(
    @Json(name = "query") val query: String
)

@JsonClass(generateAdapter = true)
data class Location(
    val latitude: Double,
    val longitude: Double,
    val address: String? = null,
    val accuracy: Float? = null
)

// Legacy OperatingHours for backward compatibility
@JsonClass(generateAdapter = true)
data class OperatingHours(
    @Json(name = "monday") val monday: String?,
    @Json(name = "tuesday") val tuesday: String?,
    @Json(name = "wednesday") val wednesday: String?,
    @Json(name = "thursday") val thursday: String?,
    @Json(name = "friday") val friday: String?,
    @Json(name = "saturday") val saturday: String?,
    @Json(name = "sunday") val sunday: String?
)
