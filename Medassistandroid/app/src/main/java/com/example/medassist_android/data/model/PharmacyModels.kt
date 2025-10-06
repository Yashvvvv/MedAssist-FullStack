package com.example.medassist_android.data.model

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Json

@JsonClass(generateAdapter = true)
data class Pharmacy(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "address") val address: String,
    @Json(name = "phoneNumber") val phoneNumber: String,
    @Json(name = "email") val email: String?,
    @Json(name = "website") val website: String?,
    @Json(name = "operatingHours") val operatingHours: OperatingHours,
    @Json(name = "services") val services: List<String>,
    @Json(name = "isActive") val isActive: Boolean,
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double,
    @Json(name = "distance") val distance: Double? = null,
    @Json(name = "distanceUnit") val distanceUnit: String? = null,
    @Json(name = "isCurrentlyOpen") val isCurrentlyOpen: Boolean? = null,
    @Json(name = "nextOpeningTime") val nextOpeningTime: String? = null,
    @Json(name = "nextClosingTime") val nextClosingTime: String? = null,
    @Json(name = "googleMapsUrl") val googleMapsUrl: String? = null,
    @Json(name = "directionsUrl") val directionsUrl: String? = null,
    @Json(name = "createdAt") val createdAt: String,
    @Json(name = "updatedAt") val updatedAt: String
)

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

@JsonClass(generateAdapter = true)
data class NearbyPharmaciesRequest(
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double,
    @Json(name = "radiusKm") val radiusKm: Double = 10.0,
    @Json(name = "maxResults") val maxResults: Int = 20,
    @Json(name = "includeOperatingHours") val includeOperatingHours: Boolean = true,
    @Json(name = "includeServices") val includeServices: Boolean = true,
    @Json(name = "filterByServices") val filterByServices: List<String>? = null,
    @Json(name = "sortBy") val sortBy: String = "DISTANCE"
)

@JsonClass(generateAdapter = true)
data class CreatePharmacyRequest(
    @Json(name = "name") val name: String,
    @Json(name = "address") val address: String,
    @Json(name = "phoneNumber") val phoneNumber: String,
    @Json(name = "email") val email: String?,
    @Json(name = "website") val website: String?,
    @Json(name = "operatingHours") val operatingHours: OperatingHours,
    @Json(name = "services") val services: List<String>,
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double,
    @Json(name = "isActive") val isActive: Boolean = true
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
