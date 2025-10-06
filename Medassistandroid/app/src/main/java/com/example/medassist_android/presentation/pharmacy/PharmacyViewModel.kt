package com.example.medassist_android.presentation.pharmacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medassist_android.data.model.Pharmacy
import com.example.medassist_android.domain.usecase.pharmacy.*
import com.example.medassist_android.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class PharmacyUiState(
    val pharmacies: List<Pharmacy> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRefreshing: Boolean = false
)

data class PharmacyDetailUiState(
    val pharmacy: Pharmacy? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isFavorite: Boolean = false
)

data class NearbyPharmaciesUiState(
    val nearbyPharmacies: List<Pharmacy> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val userLocation: Pair<Double, Double>? = null,
    val searchRadius: Double = 10.0
)

data class LocationUiState(
    val hasLocationPermission: Boolean = false,
    val isLocationEnabled: Boolean = false,
    val currentLocation: Pair<Double, Double>? = null,
    val isLoadingLocation: Boolean = false,
    val locationError: String? = null
)

@HiltViewModel
class PharmacyViewModel @Inject constructor(
    private val getPharmaciesUseCase: GetPharmaciesUseCase,
    private val getPharmacyByIdUseCase: GetPharmacyByIdUseCase,
    private val searchPharmaciesUseCase: SearchPharmaciesUseCase,
    private val getNearbyPharmaciesUseCase: GetNearbyPharmaciesUseCase,
    private val getFavoritePharmaciesUseCase: GetFavoritePharmaciesUseCase,
    private val getRecentlyViewedUseCase: GetRecentlyViewedPharmaciesUseCase,
    private val toggleFavoriteUseCase: TogglePharmacyFavoriteUseCase
) : ViewModel() {

    private val _pharmacyUiState = MutableStateFlow(PharmacyUiState())
    val pharmacyUiState: StateFlow<PharmacyUiState> = _pharmacyUiState.asStateFlow()

    private val _pharmacyDetailUiState = MutableStateFlow(PharmacyDetailUiState())
    val pharmacyDetailUiState: StateFlow<PharmacyDetailUiState> = _pharmacyDetailUiState.asStateFlow()

    private val _nearbyPharmaciesUiState = MutableStateFlow(NearbyPharmaciesUiState())
    val nearbyPharmaciesUiState: StateFlow<NearbyPharmaciesUiState> = _nearbyPharmaciesUiState.asStateFlow()

    private val _locationUiState = MutableStateFlow(LocationUiState())
    val locationUiState: StateFlow<LocationUiState> = _locationUiState.asStateFlow()

    val favoritePharmacies: StateFlow<List<Pharmacy>> = getFavoritePharmaciesUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recentlyViewed: StateFlow<List<Pharmacy>> = getRecentlyViewedUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun loadPharmacies(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            getPharmaciesUseCase(forceRefresh).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _pharmacyUiState.value = _pharmacyUiState.value.copy(
                            isLoading = !forceRefresh,
                            isRefreshing = forceRefresh,
                            error = null
                        )
                    }
                    is Resource.Success -> {
                        _pharmacyUiState.value = _pharmacyUiState.value.copy(
                            pharmacies = resource.data ?: emptyList(),
                            isLoading = false,
                            isRefreshing = false,
                            error = null
                        )
                    }
                    is Resource.Error -> {
                        _pharmacyUiState.value = _pharmacyUiState.value.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = resource.message
                        )
                        Timber.e("Failed to load pharmacies: ${resource.message}")
                    }
                }
            }
        }
    }

    fun loadPharmacyDetail(pharmacyId: Long) {
        viewModelScope.launch {
            getPharmacyByIdUseCase(pharmacyId).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _pharmacyDetailUiState.value = _pharmacyDetailUiState.value.copy(
                            isLoading = true,
                            error = null
                        )
                    }
                    is Resource.Success -> {
                        _pharmacyDetailUiState.value = _pharmacyDetailUiState.value.copy(
                            pharmacy = resource.data,
                            isLoading = false,
                            error = null
                        )
                    }
                    is Resource.Error -> {
                        _pharmacyDetailUiState.value = _pharmacyDetailUiState.value.copy(
                            isLoading = false,
                            error = resource.message
                        )
                        Timber.e("Failed to load pharmacy detail: ${resource.message}")
                    }
                }
            }
        }
    }

    fun searchPharmacies(query: String) {
        if (query.isBlank()) return

        viewModelScope.launch {
            searchPharmaciesUseCase(query).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _pharmacyUiState.value = _pharmacyUiState.value.copy(
                            isLoading = true,
                            error = null
                        )
                    }
                    is Resource.Success -> {
                        _pharmacyUiState.value = _pharmacyUiState.value.copy(
                            pharmacies = resource.data ?: emptyList(),
                            isLoading = false,
                            error = null
                        )
                    }
                    is Resource.Error -> {
                        _pharmacyUiState.value = _pharmacyUiState.value.copy(
                            isLoading = false,
                            error = resource.message
                        )
                        Timber.e("Pharmacy search failed: ${resource.message}")
                    }
                }
            }
        }
    }

    fun findNearbyPharmacies(latitude: Double, longitude: Double, radiusKm: Double = 10.0) {
        viewModelScope.launch {
            _nearbyPharmaciesUiState.value = _nearbyPharmaciesUiState.value.copy(
                userLocation = Pair(latitude, longitude),
                searchRadius = radiusKm
            )

            getNearbyPharmaciesUseCase(latitude, longitude, radiusKm).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _nearbyPharmaciesUiState.value = _nearbyPharmaciesUiState.value.copy(
                            isLoading = true,
                            error = null
                        )
                    }
                    is Resource.Success -> {
                        _nearbyPharmaciesUiState.value = _nearbyPharmaciesUiState.value.copy(
                            nearbyPharmacies = resource.data ?: emptyList(),
                            isLoading = false,
                            error = null
                        )
                    }
                    is Resource.Error -> {
                        _nearbyPharmaciesUiState.value = _nearbyPharmaciesUiState.value.copy(
                            isLoading = false,
                            error = resource.message
                        )
                        Timber.e("Failed to find nearby pharmacies: ${resource.message}")
                    }
                }
            }
        }
    }

    fun toggleFavorite(pharmacyId: Long) {
        viewModelScope.launch {
            val result = toggleFavoriteUseCase(pharmacyId)
            when (result) {
                is Resource.Success -> {
                    _pharmacyDetailUiState.value = _pharmacyDetailUiState.value.copy(
                        isFavorite = result.data ?: false
                    )
                }
                is Resource.Error -> {
                    Timber.e("Toggle favorite failed: ${result.message}")
                }
                else -> {}
            }
        }
    }

    fun updateLocationPermission(hasPermission: Boolean) {
        _locationUiState.value = _locationUiState.value.copy(
            hasLocationPermission = hasPermission
        )
    }

    fun updateLocationEnabled(isEnabled: Boolean) {
        _locationUiState.value = _locationUiState.value.copy(
            isLocationEnabled = isEnabled
        )
    }

    fun updateCurrentLocation(latitude: Double, longitude: Double) {
        _locationUiState.value = _locationUiState.value.copy(
            currentLocation = Pair(latitude, longitude),
            isLoadingLocation = false,
            locationError = null
        )
    }

    fun setLocationLoading(isLoading: Boolean) {
        _locationUiState.value = _locationUiState.value.copy(
            isLoadingLocation = isLoading
        )
    }

    fun setLocationError(error: String?) {
        _locationUiState.value = _locationUiState.value.copy(
            locationError = error,
            isLoadingLocation = false
        )
    }

    fun clearError() {
        _pharmacyUiState.value = _pharmacyUiState.value.copy(error = null)
        _pharmacyDetailUiState.value = _pharmacyDetailUiState.value.copy(error = null)
        _nearbyPharmaciesUiState.value = _nearbyPharmaciesUiState.value.copy(error = null)
        _locationUiState.value = _locationUiState.value.copy(locationError = null)
    }
}
