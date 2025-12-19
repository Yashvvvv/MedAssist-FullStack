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
    val brandNames: List<String>?,
    val description: String?,
    val usageDescription: String?,
    val dosageInformation: String?,
    val sideEffects: List<String>?,
    val manufacturer: String,
    val category: String?,
    val form: String?,
    val strength: String?,
    val activeIngredient: String?,
    val requiresPrescription: Boolean,
    val storageInstructions: String?,
    val createdAt: String?,
    val updatedAt: String?,
    val isFavorite: Boolean = false,
    val lastSearched: Long? = null
)

// Updated PharmacyEntity to match backend Pharmacy entity
@Entity(tableName = "pharmacies")
data class PharmacyEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val address: String,
    val city: String?,
    val state: String?,
    val zipCode: String?,
    val country: String?,
    val phoneNumber: String?,
    val emailAddress: String?,
    val operatingHours: String?, // JSON string or simple text
    val emergencyHours: String?,
    val websiteUrl: String?,
    val is24Hours: Boolean = false,
    val acceptsInsurance: Boolean = false,
    val hasDriveThrough: Boolean = false,
    val hasDelivery: Boolean = false,
    val hasConsultation: Boolean = false,
    val services: String?, // JSON string
    val latitude: Double?,
    val longitude: Double?,
    val licenseNumber: String?,
    val managerName: String?,
    val pharmacistName: String?,
    val chainName: String?,
    val rating: Double?,
    val isActive: Boolean = true,
    val createdAt: String?,
    val updatedAt: String?,
    val isFavorite: Boolean = false,
    val lastViewed: Long? = null
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val searchType: String, // TEXT, IMAGE, COMBINED, AI_TEXT, AI_IMAGE, AI_COMBINED
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
