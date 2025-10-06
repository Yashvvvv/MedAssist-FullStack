package com.example.medassist_android.domain.usecase.medicine

import com.example.medassist_android.data.model.Medicine
import com.example.medassist_android.data.model.MedicineAnalysisResponse
import com.example.medassist_android.data.repository.MedicineRepository
import com.example.medassist_android.util.Resource
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject

class GetMedicinesUseCase @Inject constructor(
    private val medicineRepository: MedicineRepository
) {
    operator fun invoke(forceRefresh: Boolean = false): Flow<Resource<List<Medicine>>> {
        return medicineRepository.getMedicines(forceRefresh)
    }
}

class GetMedicineByIdUseCase @Inject constructor(
    private val medicineRepository: MedicineRepository
) {
    operator fun invoke(id: Long): Flow<Resource<Medicine>> {
        return medicineRepository.getMedicineById(id)
    }
}

class SearchMedicinesUseCase @Inject constructor(
    private val medicineRepository: MedicineRepository
) {
    operator fun invoke(query: String): Flow<Resource<List<Medicine>>> {
        return medicineRepository.searchMedicines(query)
    }
}

class AnalyzeMedicineByTextUseCase @Inject constructor(
    private val medicineRepository: MedicineRepository
) {
    operator fun invoke(query: String): Flow<Resource<MedicineAnalysisResponse>> {
        return medicineRepository.analyzeByText(query)
    }
}

class AnalyzeMedicineByImageUseCase @Inject constructor(
    private val medicineRepository: MedicineRepository
) {
    operator fun invoke(imageFile: File): Flow<Resource<MedicineAnalysisResponse>> {
        return medicineRepository.analyzeByImage(imageFile)
    }
}

class AnalyzeMedicineCombinedUseCase @Inject constructor(
    private val medicineRepository: MedicineRepository
) {
    operator fun invoke(imageFile: File, query: String): Flow<Resource<MedicineAnalysisResponse>> {
        return medicineRepository.analyzeCombined(imageFile, query)
    }
}

class GetFavoriteMedicinesUseCase @Inject constructor(
    private val medicineRepository: MedicineRepository
) {
    operator fun invoke(): Flow<List<Medicine>> {
        return medicineRepository.getFavoriteMedicines()
    }
}

class GetRecentlySearchedMedicinesUseCase @Inject constructor(
    private val medicineRepository: MedicineRepository
) {
    operator fun invoke(): Flow<List<Medicine>> {
        return medicineRepository.getRecentlySearchedMedicines()
    }
}

class ToggleMedicineFavoriteUseCase @Inject constructor(
    private val medicineRepository: MedicineRepository
) {
    suspend operator fun invoke(medicineId: Long): Resource<Boolean> {
        return medicineRepository.toggleFavorite(medicineId)
    }
}

class ClearSearchHistoryUseCase @Inject constructor(
    private val medicineRepository: MedicineRepository
) {
    suspend operator fun invoke() {
        medicineRepository.clearSearchHistory()
    }
}
