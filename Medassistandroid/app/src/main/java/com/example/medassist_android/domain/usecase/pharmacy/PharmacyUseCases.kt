package com.example.medassist_android.domain.usecase.pharmacy

import com.example.medassist_android.data.model.NearbyPharmaciesRequest
import com.example.medassist_android.data.model.Pharmacy
import com.example.medassist_android.data.repository.PharmacyRepository
import com.example.medassist_android.util.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPharmaciesUseCase @Inject constructor(
    private val pharmacyRepository: PharmacyRepository
) {
    operator fun invoke(forceRefresh: Boolean = false): Flow<Resource<List<Pharmacy>>> {
        return pharmacyRepository.getPharmacies(forceRefresh)
    }
}

class GetPharmacyByIdUseCase @Inject constructor(
    private val pharmacyRepository: PharmacyRepository
) {
    operator fun invoke(id: Long): Flow<Resource<Pharmacy>> {
        return pharmacyRepository.getPharmacyById(id)
    }
}

class SearchPharmaciesUseCase @Inject constructor(
    private val pharmacyRepository: PharmacyRepository
) {
    operator fun invoke(query: String): Flow<Resource<List<Pharmacy>>> {
        return pharmacyRepository.searchPharmacies(query)
    }
}

class GetNearbyPharmaciesUseCase @Inject constructor(
    private val pharmacyRepository: PharmacyRepository
) {
    operator fun invoke(
        latitude: Double,
        longitude: Double,
        radiusKm: Double = 10.0,
        maxResults: Int = 20
    ): Flow<Resource<List<Pharmacy>>> {
        val request = NearbyPharmaciesRequest(
            latitude = latitude,
            longitude = longitude,
            radiusKm = radiusKm,
            maxResults = maxResults
        )
        return pharmacyRepository.getNearbyPharmacies(request)
    }
}

class GetFavoritePharmaciesUseCase @Inject constructor(
    private val pharmacyRepository: PharmacyRepository
) {
    operator fun invoke(): Flow<List<Pharmacy>> {
        return pharmacyRepository.getFavoritePharmacies()
    }
}

class GetRecentlyViewedPharmaciesUseCase @Inject constructor(
    private val pharmacyRepository: PharmacyRepository
) {
    operator fun invoke(): Flow<List<Pharmacy>> {
        return pharmacyRepository.getRecentlyViewedPharmacies()
    }
}

class TogglePharmacyFavoriteUseCase @Inject constructor(
    private val pharmacyRepository: PharmacyRepository
) {
    suspend operator fun invoke(pharmacyId: Long): Resource<Boolean> {
        return pharmacyRepository.toggleFavorite(pharmacyId)
    }
}
