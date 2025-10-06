package com.example.medassist_android.data.repository

import com.example.medassist_android.data.local.dao.MedicineDao
import com.example.medassist_android.data.local.dao.SearchHistoryDao
import com.example.medassist_android.data.local.entity.MedicineEntity
import com.example.medassist_android.data.local.entity.SearchHistoryEntity
import com.example.medassist_android.data.model.*
import com.example.medassist_android.data.network.MedicineApiService
import com.example.medassist_android.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import timber.log.Timber
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicineRepository @Inject constructor(
    private val medicineApiService: MedicineApiService,
    private val medicineDao: MedicineDao,
    private val searchHistoryDao: SearchHistoryDao
) {

    fun getMedicines(forceRefresh: Boolean = false): Flow<Resource<List<Medicine>>> = flow {
        try {
            emit(Resource.Loading())

            // First emit cached data if available
            if (!forceRefresh) {
                val cachedMedicines = medicineDao.getAllMedicines()
                cachedMedicines.collect { entities ->
                    if (entities.isNotEmpty()) {
                        emit(Resource.Success(entities.map { it.toMedicine() }))
                    }
                }
            }

            // Fetch from API
            val response = medicineApiService.getAllMedicines()
            if (response.isSuccessful) {
                val medicines = response.body() ?: emptyList()

                // Cache the medicines
                medicineDao.deleteAllMedicines()
                medicineDao.insertMedicines(medicines.map { it.toEntity() })

                emit(Resource.Success(medicines))
            } else {
                emit(Resource.Error("Failed to fetch medicines"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error(e.localizedMessage ?: "Network error"))
        } catch (e: IOException) {
            emit(Resource.Error("Network connection error"))
        } catch (e: Exception) {
            Timber.e(e, "Get medicines error")
            emit(Resource.Error("An unexpected error occurred"))
        }
    }

    fun getMedicineById(id: Long): Flow<Resource<Medicine>> = flow {
        try {
            emit(Resource.Loading())

            // Check cache first
            val cachedMedicine = medicineDao.getMedicineById(id)
            if (cachedMedicine != null) {
                emit(Resource.Success(cachedMedicine.toMedicine()))
            }

            // Fetch from API
            val response = medicineApiService.getMedicineById(id)
            if (response.isSuccessful) {
                val medicine = response.body()
                if (medicine != null) {
                    // Cache the medicine
                    medicineDao.insertMedicine(medicine.toEntity())
                    emit(Resource.Success(medicine))
                } else {
                    emit(Resource.Error("Medicine not found"))
                }
            } else {
                emit(Resource.Error("Failed to fetch medicine details"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error(e.localizedMessage ?: "Network error"))
        } catch (e: IOException) {
            emit(Resource.Error("Network connection error"))
        } catch (e: Exception) {
            Timber.e(e, "Get medicine by ID error")
            emit(Resource.Error("An unexpected error occurred"))
        }
    }

    fun searchMedicines(query: String): Flow<Resource<List<Medicine>>> = flow {
        try {
            emit(Resource.Loading())

            // Save search history
            val searchHistory = SearchHistoryEntity(
                query = query,
                searchType = "TEXT",
                timestamp = System.currentTimeMillis(),
                resultCount = 0,
                userId = null // Will be set when user info is available
            )

            // Search in cache first
            val cachedResults = medicineDao.searchMedicines(query)
            if (cachedResults.isNotEmpty()) {
                emit(Resource.Success(cachedResults.map { it.toMedicine() }))
            }

            // Search via API
            val response = medicineApiService.searchMedicines(query)
            if (response.isSuccessful) {
                val medicines = response.body() ?: emptyList()

                // Cache the search results
                medicines.forEach { medicine ->
                    medicineDao.insertMedicine(medicine.toEntity())
                    medicineDao.updateLastSearched(medicine.id, System.currentTimeMillis())
                }

                // Update search history
                searchHistoryDao.insertSearchHistory(
                    searchHistory.copy(resultCount = medicines.size)
                )

                emit(Resource.Success(medicines))
            } else {
                emit(Resource.Error("Search failed"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error(e.localizedMessage ?: "Network error"))
        } catch (e: IOException) {
            emit(Resource.Error("Network connection error"))
        } catch (e: Exception) {
            Timber.e(e, "Search medicines error")
            emit(Resource.Error("An unexpected error occurred"))
        }
    }

    fun analyzeByText(query: String): Flow<Resource<MedicineAnalysisResponse>> = flow {
        try {
            emit(Resource.Loading())

            val request = MedicineAnalysisRequest(query)
            val response = medicineApiService.analyzeByText(request)

            if (response.isSuccessful) {
                val analysisResponse = response.body()
                if (analysisResponse != null) {
                    // Save search history
                    val searchHistory = SearchHistoryEntity(
                        query = query,
                        searchType = "AI_TEXT",
                        timestamp = System.currentTimeMillis(),
                        resultCount = analysisResponse.medicines.size,
                        userId = null
                    )
                    searchHistoryDao.insertSearchHistory(searchHistory)

                    emit(Resource.Success(analysisResponse))
                } else {
                    emit(Resource.Error("Analysis failed"))
                }
            } else {
                emit(Resource.Error("AI analysis failed"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error(e.localizedMessage ?: "Network error"))
        } catch (e: IOException) {
            emit(Resource.Error("Network connection error"))
        } catch (e: Exception) {
            Timber.e(e, "Analyze by text error")
            emit(Resource.Error("An unexpected error occurred"))
        }
    }

    fun analyzeByImage(imageFile: File): Flow<Resource<MedicineAnalysisResponse>> = flow {
        try {
            emit(Resource.Loading())

            val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)

            val response = medicineApiService.analyzeByImage(imagePart)

            if (response.isSuccessful) {
                val analysisResponse = response.body()
                if (analysisResponse != null) {
                    // Save search history
                    val searchHistory = SearchHistoryEntity(
                        query = "Image analysis",
                        searchType = "AI_IMAGE",
                        timestamp = System.currentTimeMillis(),
                        resultCount = analysisResponse.medicines.size,
                        userId = null
                    )
                    searchHistoryDao.insertSearchHistory(searchHistory)

                    emit(Resource.Success(analysisResponse))
                } else {
                    emit(Resource.Error("Image analysis failed"))
                }
            } else {
                emit(Resource.Error("AI image analysis failed"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error(e.localizedMessage ?: "Network error"))
        } catch (e: IOException) {
            emit(Resource.Error("Network connection error"))
        } catch (e: Exception) {
            Timber.e(e, "Analyze by image error")
            emit(Resource.Error("An unexpected error occurred"))
        }
    }

    fun analyzeCombined(imageFile: File, query: String): Flow<Resource<MedicineAnalysisResponse>> = flow {
        try {
            emit(Resource.Loading())

            val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)
            val queryPart = query.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = medicineApiService.analyzeCombined(imagePart, queryPart)

            if (response.isSuccessful) {
                val analysisResponse = response.body()
                if (analysisResponse != null) {
                    // Save search history
                    val searchHistory = SearchHistoryEntity(
                        query = query,
                        searchType = "AI_COMBINED",
                        timestamp = System.currentTimeMillis(),
                        resultCount = analysisResponse.medicines.size,
                        userId = null
                    )
                    searchHistoryDao.insertSearchHistory(searchHistory)

                    emit(Resource.Success(analysisResponse))
                } else {
                    emit(Resource.Error("Combined analysis failed"))
                }
            } else {
                emit(Resource.Error("AI combined analysis failed"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error(e.localizedMessage ?: "Network error"))
        } catch (e: IOException) {
            emit(Resource.Error("Network connection error"))
        } catch (e: Exception) {
            Timber.e(e, "Analyze combined error")
            emit(Resource.Error("An unexpected error occurred"))
        }
    }

    fun getFavoriteMedicines(): Flow<List<Medicine>> {
        return medicineDao.getFavoriteMedicines().map { entities ->
            entities.map { it.toMedicine() }
        }
    }

    fun getRecentlySearchedMedicines(): Flow<List<Medicine>> {
        return medicineDao.getRecentlySearchedMedicines().map { entities ->
            entities.map { it.toMedicine() }
        }
    }

    suspend fun toggleFavorite(medicineId: Long): Resource<Boolean> {
        return try {
            val medicine = medicineDao.getMedicineById(medicineId)
            if (medicine != null) {
                val newFavoriteStatus = !medicine.isFavorite
                medicineDao.updateFavoriteStatus(medicineId, newFavoriteStatus)
                Resource.Success(newFavoriteStatus)
            } else {
                Resource.Error("Medicine not found")
            }
        } catch (e: Exception) {
            Timber.e(e, "Toggle favorite error")
            Resource.Error("Failed to update favorite status")
        }
    }

    fun getSearchHistory(): Flow<List<SearchHistoryEntity>> {
        return searchHistoryDao.getAllSearchHistory()
    }

    suspend fun clearSearchHistory() {
        searchHistoryDao.deleteAllSearchHistory()
    }
}

// Extension functions for data mapping
private fun Medicine.toEntity(): MedicineEntity {
    return MedicineEntity(
        id = id,
        name = name,
        genericName = genericName,
        brandNames = brandNames,
        description = description,
        usageDescription = usageDescription,
        dosageInformation = dosageInformation,
        sideEffects = sideEffects,
        manufacturer = manufacturer,
        category = category,
        form = form,
        strength = strength,
        activeIngredient = activeIngredient, // Changed from activeIngredients
        requiresPrescription = requiresPrescription,
        storageInstructions = storageInstructions, // Added missing field
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

private fun MedicineEntity.toMedicine(): Medicine {
    return Medicine(
        id = id,
        name = name,
        genericName = genericName,
        brandNames = brandNames,
        description = description,
        usageDescription = usageDescription,
        dosageInformation = dosageInformation,
        sideEffects = sideEffects,
        manufacturer = manufacturer,
        category = category,
        form = form,
        strength = strength,
        activeIngredient = activeIngredient, // Changed from activeIngredients
        requiresPrescription = requiresPrescription,
        storageInstructions = storageInstructions, // Added missing field
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
