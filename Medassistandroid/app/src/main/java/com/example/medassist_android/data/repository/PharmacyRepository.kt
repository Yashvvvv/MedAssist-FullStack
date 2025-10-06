package com.example.medassist_android.data.repository

import com.example.medassist_android.data.local.dao.PharmacyDao
import com.example.medassist_android.data.local.entity.PharmacyEntity
import com.example.medassist_android.data.local.entity.SearchHistoryEntity
import com.example.medassist_android.data.model.*
import com.example.medassist_android.data.network.PharmacyApiService
import com.example.medassist_android.util.Resource
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PharmacyRepository @Inject constructor(
    private val pharmacyApiService: PharmacyApiService,
    private val pharmacyDao: PharmacyDao,
    private val moshi: Moshi
) {

    fun getPharmacies(forceRefresh: Boolean = false): Flow<Resource<List<Pharmacy>>> = flow {
        try {
            emit(Resource.Loading())

            // First emit cached data if available
            if (!forceRefresh) {
                val cachedPharmacies = pharmacyDao.getAllPharmacies()
                cachedPharmacies.collect { entities ->
                    if (entities.isNotEmpty()) {
                        emit(Resource.Success(entities.map { it.toPharmacy(moshi) }))
                    }
                }
            }

            // Fetch from API
            val response = pharmacyApiService.getAllPharmacies()
            if (response.isSuccessful) {
                val pharmacies = response.body() ?: emptyList()

                // Cache the pharmacies
                pharmacyDao.deleteAllPharmacies()
                pharmacyDao.insertPharmacies(pharmacies.map { it.toEntity(moshi) })

                emit(Resource.Success(pharmacies))
            } else {
                emit(Resource.Error("Failed to fetch pharmacies"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error(e.localizedMessage ?: "Network error"))
        } catch (e: IOException) {
            emit(Resource.Error("Network connection error"))
        } catch (e: Exception) {
            Timber.e(e, "Get pharmacies error")
            emit(Resource.Error("An unexpected error occurred"))
        }
    }

    fun getPharmacyById(id: Long): Flow<Resource<Pharmacy>> = flow {
        try {
            emit(Resource.Loading())

            // Check cache first
            val cachedPharmacy = pharmacyDao.getPharmacyById(id)
            if (cachedPharmacy != null) {
                emit(Resource.Success(cachedPharmacy.toPharmacy(moshi)))
            }

            // Fetch from API
            val response = pharmacyApiService.getPharmacyById(id)
            if (response.isSuccessful) {
                val pharmacy = response.body()
                if (pharmacy != null) {
                    // Cache the pharmacy
                    pharmacyDao.insertPharmacy(pharmacy.toEntity(moshi))
                    // Update last viewed
                    pharmacyDao.updateLastViewed(id, System.currentTimeMillis())
                    emit(Resource.Success(pharmacy))
                } else {
                    emit(Resource.Error("Pharmacy not found"))
                }
            } else {
                emit(Resource.Error("Failed to fetch pharmacy details"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error(e.localizedMessage ?: "Network error"))
        } catch (e: IOException) {
            emit(Resource.Error("Network connection error"))
        } catch (e: Exception) {
            Timber.e(e, "Get pharmacy by ID error")
            emit(Resource.Error("An unexpected error occurred"))
        }
    }

    fun searchPharmacies(query: String): Flow<Resource<List<Pharmacy>>> = flow {
        try {
            emit(Resource.Loading())

            // Search in cache first
            val cachedResults = pharmacyDao.searchPharmacies(query)
            if (cachedResults.isNotEmpty()) {
                emit(Resource.Success(cachedResults.map { it.toPharmacy(moshi) }))
            }

            // Search via API
            val response = pharmacyApiService.searchPharmacies(query)
            if (response.isSuccessful) {
                val pharmacies = response.body() ?: emptyList()

                // Cache the search results
                pharmacies.forEach { pharmacy ->
                    pharmacyDao.insertPharmacy(pharmacy.toEntity(moshi))
                }

                emit(Resource.Success(pharmacies))
            } else {
                emit(Resource.Error("Search failed"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error(e.localizedMessage ?: "Network error"))
        } catch (e: IOException) {
            emit(Resource.Error("Network connection error"))
        } catch (e: Exception) {
            Timber.e(e, "Search pharmacies error")
            emit(Resource.Error("An unexpected error occurred"))
        }
    }

    fun getNearbyPharmacies(request: NearbyPharmaciesRequest): Flow<Resource<List<Pharmacy>>> = flow {
        try {
            emit(Resource.Loading())

            val response = pharmacyApiService.getNearbyPharmacies(request)
            if (response.isSuccessful) {
                val pharmacies = response.body() ?: emptyList()

                // Cache the nearby pharmacies
                pharmacies.forEach { pharmacy ->
                    pharmacyDao.insertPharmacy(pharmacy.toEntity(moshi))
                }

                emit(Resource.Success(pharmacies))
            } else {
                emit(Resource.Error("Failed to find nearby pharmacies"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error(e.localizedMessage ?: "Network error"))
        } catch (e: IOException) {
            emit(Resource.Error("Network connection error"))
        } catch (e: Exception) {
            Timber.e(e, "Get nearby pharmacies error")
            emit(Resource.Error("An unexpected error occurred"))
        }
    }

    fun getFavoritePharmacies(): Flow<List<Pharmacy>> {
        return pharmacyDao.getFavoritePharmacies().map { entities ->
            entities.map { it.toPharmacy(moshi) }
        }
    }

    fun getRecentlyViewedPharmacies(): Flow<List<Pharmacy>> {
        return pharmacyDao.getRecentlyViewedPharmacies().map { entities ->
            entities.map { it.toPharmacy(moshi) }
        }
    }

    suspend fun toggleFavorite(pharmacyId: Long): Resource<Boolean> {
        return try {
            val pharmacy = pharmacyDao.getPharmacyById(pharmacyId)
            if (pharmacy != null) {
                val newFavoriteStatus = !pharmacy.isFavorite
                pharmacyDao.updateFavoriteStatus(pharmacyId, newFavoriteStatus)
                Resource.Success(newFavoriteStatus)
            } else {
                Resource.Error("Pharmacy not found")
            }
        } catch (e: Exception) {
            Timber.e(e, "Toggle favorite error")
            Resource.Error("Failed to update favorite status")
        }
    }

    suspend fun getCachedNearbyPharmacies(): List<Pharmacy> {
        return pharmacyDao.getNearbyPharmacies().map { it.toPharmacy(moshi) }
    }
}

// Extension functions for data mapping
private fun Pharmacy.toEntity(moshi: Moshi): PharmacyEntity {
    val operatingHoursJson = moshi.adapter(OperatingHours::class.java).toJson(operatingHours)
    val servicesJson = moshi.adapter(List::class.java).toJson(services)

    return PharmacyEntity(
        id = id,
        name = name,
        address = address,
        phoneNumber = phoneNumber,
        email = email,
        website = website,
        operatingHours = operatingHoursJson,
        services = servicesJson,
        isActive = isActive,
        latitude = latitude,
        longitude = longitude,
        distance = distance,
        distanceUnit = distanceUnit,
        isCurrentlyOpen = isCurrentlyOpen,
        nextOpeningTime = nextOpeningTime,
        nextClosingTime = nextClosingTime,
        googleMapsUrl = googleMapsUrl,
        directionsUrl = directionsUrl,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

private fun PharmacyEntity.toPharmacy(moshi: Moshi): Pharmacy {
    val operatingHours = try {
        moshi.adapter(OperatingHours::class.java).fromJson(operatingHours) ?: OperatingHours(
            null, null, null, null, null, null, null
        )
    } catch (e: Exception) {
        OperatingHours(null, null, null, null, null, null, null)
    }

    val servicesListType = Types.newParameterizedType(List::class.java, String::class.java)
    val services = try {
        moshi.adapter<List<String>>(servicesListType).fromJson(services) ?: emptyList()
    } catch (e: Exception) {
        emptyList<String>()
    }

    return Pharmacy(
        id = id,
        name = name,
        address = address,
        phoneNumber = phoneNumber,
        email = email,
        website = website,
        operatingHours = operatingHours,
        services = services,
        isActive = isActive,
        latitude = latitude,
        longitude = longitude,
        distance = distance,
        distanceUnit = distanceUnit,
        isCurrentlyOpen = isCurrentlyOpen,
        nextOpeningTime = nextOpeningTime,
        nextClosingTime = nextClosingTime,
        googleMapsUrl = googleMapsUrl,
        directionsUrl = directionsUrl,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
