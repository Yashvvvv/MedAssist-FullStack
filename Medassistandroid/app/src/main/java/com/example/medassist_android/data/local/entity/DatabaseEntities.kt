package com.example.medassist_android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.medassist_android.data.local.converter.StringListConverter

@Entity(tableName = "medicines")
@TypeConverters(StringListConverter::class)
data class MedicineEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val genericName: String,
    val brandNames: List<String>,
    val description: String?,
    val usageDescription: String?,
    val dosageInformation: String?,
    val sideEffects: List<String>,
    val manufacturer: String,
    val category: String,
    val form: String,
    val strength: String,
    val activeIngredient: String, // Changed from List<String> to String
    val requiresPrescription: Boolean,
    val storageInstructions: String?, // Added missing field
    val createdAt: String,
    val updatedAt: String,
    val isFavorite: Boolean = false,
    val lastSearched: Long? = null
)

@Entity(tableName = "pharmacies")
data class PharmacyEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val address: String,
    val phoneNumber: String,
    val email: String?,
    val website: String?,
    val operatingHours: String, // JSON string
    val services: String, // JSON string
    val isActive: Boolean,
    val latitude: Double,
    val longitude: Double,
    val distance: Double?,
    val distanceUnit: String?,
    val isCurrentlyOpen: Boolean?,
    val nextOpeningTime: String?,
    val nextClosingTime: String?,
    val googleMapsUrl: String?,
    val directionsUrl: String?,
    val createdAt: String,
    val updatedAt: String,
    val isFavorite: Boolean = false,
    val lastViewed: Long? = null
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val searchType: String, // TEXT, IMAGE, COMBINED
    val timestamp: Long,
    val resultCount: Int,
    val userId: Long?
)

@Entity(tableName = "user_favorites")
data class UserFavoriteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val itemId: Long,
    val itemType: String, // MEDICINE, PHARMACY
    val addedAt: Long
)
