package com.example.medassist_android.presentation.medicine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medassist_android.data.model.Medicine
import com.example.medassist_android.data.model.MedicineAnalysisResponse
import com.example.medassist_android.domain.usecase.medicine.*
import com.example.medassist_android.util.Resource
import com.example.medassist_android.domain.usecase.auth.IsLoggedInUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject

data class MedicineUiState(
    val medicines: List<Medicine> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRefreshing: Boolean = false,
    // Drug Interactions
    val isAnalyzing: Boolean = false,
    val analysisResponse: MedicineAnalysisResponse? = null,
    val analysisError: String? = null
)

data class MedicineDetailUiState(
    val medicine: Medicine? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isFavorite: Boolean = false
)

data class SearchUiState(
    val query: String = "",
    val searchResults: List<Medicine> = emptyList(),
    val recentSearches: List<Medicine> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSearching: Boolean = false
)

data class AiAnalysisUiState(
    val analysisResult: MedicineAnalysisResponse? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val analysisType: String = "TEXT" // TEXT, IMAGE, COMBINED
)

@HiltViewModel
class MedicineViewModel @Inject constructor(
    private val getMedicinesUseCase: GetMedicinesUseCase,
    private val getMedicineByIdUseCase: GetMedicineByIdUseCase,
    private val searchMedicinesUseCase: SearchMedicinesUseCase,
    private val analyzeByTextUseCase: AnalyzeMedicineByTextUseCase,
    private val analyzeByImageUseCase: AnalyzeMedicineByImageUseCase,
    private val analyzeCombinedUseCase: AnalyzeMedicineCombinedUseCase,
    private val getFavoriteMedicinesUseCase: GetFavoriteMedicinesUseCase,
    private val getRecentlySearchedUseCase: GetRecentlySearchedMedicinesUseCase,
    private val toggleFavoriteUseCase: ToggleMedicineFavoriteUseCase,
    private val clearSearchHistoryUseCase: ClearSearchHistoryUseCase,
    private val analyzeDrugInteractionsUseCase: AnalyzeDrugInteractionsUseCase,
    private val isLoggedInUseCase: IsLoggedInUseCase
) : ViewModel() {

    private val _medicineUiState = MutableStateFlow(MedicineUiState())
    val medicineUiState: StateFlow<MedicineUiState> = _medicineUiState.asStateFlow()

    private val _medicineDetailUiState = MutableStateFlow(MedicineDetailUiState())
    val medicineDetailUiState: StateFlow<MedicineDetailUiState> = _medicineDetailUiState.asStateFlow()

    private val _searchUiState = MutableStateFlow(SearchUiState())
    val searchUiState: StateFlow<SearchUiState> = _searchUiState.asStateFlow()

    private val _aiAnalysisUiState = MutableStateFlow(AiAnalysisUiState())
    val aiAnalysisUiState: StateFlow<AiAnalysisUiState> = _aiAnalysisUiState.asStateFlow()

    val favoriteMedicines: StateFlow<List<Medicine>> = getFavoriteMedicinesUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recentlySearched: StateFlow<List<Medicine>> = getRecentlySearchedUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        loadMedicines()
        loadRecentSearches()
    }

    fun loadMedicines(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            getMedicinesUseCase(forceRefresh).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _medicineUiState.value = _medicineUiState.value.copy(
                            isLoading = !forceRefresh,
                            isRefreshing = forceRefresh,
                            error = null
                        )
                    }
                    is Resource.Success -> {
                        _medicineUiState.value = _medicineUiState.value.copy(
                            medicines = resource.data ?: emptyList(),
                            isLoading = false,
                            isRefreshing = false,
                            error = null
                        )
                    }
                    is Resource.Error -> {
                        _medicineUiState.value = _medicineUiState.value.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = resource.message
                        )
                        Timber.e("Failed to load medicines: ${resource.message}")
                    }
                }
            }
        }
    }

    fun loadMedicineDetail(medicineId: Long) {
        viewModelScope.launch {
            getMedicineByIdUseCase(medicineId).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _medicineDetailUiState.value = _medicineDetailUiState.value.copy(
                            isLoading = true,
                            error = null
                        )
                    }
                    is Resource.Success -> {
                        _medicineDetailUiState.value = _medicineDetailUiState.value.copy(
                            medicine = resource.data,
                            isLoading = false,
                            error = null
                        )
                    }
                    is Resource.Error -> {
                        _medicineDetailUiState.value = _medicineDetailUiState.value.copy(
                            isLoading = false,
                            error = resource.message
                        )
                        Timber.e("Failed to load medicine detail: ${resource.message}")
                    }
                }
            }
        }
    }

    fun searchMedicines(query: String) {
        if (query.isBlank()) {
            _searchUiState.value = _searchUiState.value.copy(
                searchResults = emptyList(),
                isSearching = false
            )
            return
        }

        viewModelScope.launch {
            _searchUiState.value = _searchUiState.value.copy(
                query = query,
                isSearching = true
            )

            searchMedicinesUseCase(query).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _searchUiState.value = _searchUiState.value.copy(
                            isLoading = true,
                            error = null
                        )
                    }
                    is Resource.Success -> {
                        _searchUiState.value = _searchUiState.value.copy(
                            searchResults = resource.data ?: emptyList(),
                            isLoading = false,
                            isSearching = false,
                            error = null
                        )
                    }
                    is Resource.Error -> {
                        _searchUiState.value = _searchUiState.value.copy(
                            isLoading = false,
                            isSearching = false,
                            error = resource.message
                        )
                        Timber.e("Search failed: ${resource.message}")
                    }
                }
            }
        }
    }

    fun analyzeByText(query: String) {
        viewModelScope.launch {
            if (isLoggedInUseCase()) {
                _aiAnalysisUiState.value = _aiAnalysisUiState.value.copy(
                    analysisType = "TEXT"
                )

                analyzeByTextUseCase(query).collect { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            _aiAnalysisUiState.value = _aiAnalysisUiState.value.copy(
                                isLoading = true,
                                error = null
                            )
                        }
                        is Resource.Success -> {
                            _aiAnalysisUiState.value = _aiAnalysisUiState.value.copy(
                                analysisResult = resource.data,
                                isLoading = false,
                                error = null
                            )
                        }
                        is Resource.Error -> {
                            _aiAnalysisUiState.value = _aiAnalysisUiState.value.copy(
                                isLoading = false,
                                error = resource.message
                            )
                            Timber.e("AI text analysis failed: ${resource.message}")
                        }
                    }
                }
            } else {
                _aiAnalysisUiState.value = _aiAnalysisUiState.value.copy(
                    error = "User not authenticated. Please log in to continue."
                )
                Timber.e("User not authenticated")
            }
        }
    }

    fun analyzeByImage(imageFile: File) {
        viewModelScope.launch {
            if (isLoggedInUseCase()) {
                _aiAnalysisUiState.value = _aiAnalysisUiState.value.copy(
                    analysisType = "IMAGE"
                )

                analyzeByImageUseCase(imageFile).collect { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            _aiAnalysisUiState.value = _aiAnalysisUiState.value.copy(
                                isLoading = true,
                                error = null
                            )
                        }
                        is Resource.Success -> {
                            _aiAnalysisUiState.value = _aiAnalysisUiState.value.copy(
                                analysisResult = resource.data,
                                isLoading = false,
                                error = null
                            )
                        }
                        is Resource.Error -> {
                            _aiAnalysisUiState.value = _aiAnalysisUiState.value.copy(
                                isLoading = false,
                                error = resource.message
                            )
                            Timber.e("AI image analysis failed: ${resource.message}")
                        }
                    }
                }
            } else {
                _aiAnalysisUiState.value = _aiAnalysisUiState.value.copy(
                    error = "User not authenticated. Please log in to continue."
                )
                Timber.e("User not authenticated")
            }
        }
    }

    fun analyzeCombined(imageFile: File, query: String) {
        viewModelScope.launch {
            if (isLoggedInUseCase()) {
                _aiAnalysisUiState.value = _aiAnalysisUiState.value.copy(
                    analysisType = "COMBINED"
                )

                analyzeCombinedUseCase(imageFile, query).collect { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            _aiAnalysisUiState.value = _aiAnalysisUiState.value.copy(
                                isLoading = true,
                                error = null
                            )
                        }
                        is Resource.Success -> {
                            _aiAnalysisUiState.value = _aiAnalysisUiState.value.copy(
                                analysisResult = resource.data,
                                isLoading = false,
                                error = null
                            )
                        }
                        is Resource.Error -> {
                            _aiAnalysisUiState.value = _aiAnalysisUiState.value.copy(
                                isLoading = false,
                                error = resource.message
                            )
                            Timber.e("AI combined analysis failed: ${resource.message}")
                        }
                    }
                }
            } else {
                _aiAnalysisUiState.value = _aiAnalysisUiState.value.copy(
                    error = "User not authenticated. Please log in to continue."
                )
                Timber.e("User not authenticated")
            }
        }
    }

    fun toggleFavorite(medicineId: Long) {
        viewModelScope.launch {
            val result = toggleFavoriteUseCase(medicineId)
            when (result) {
                is Resource.Success -> {
                    _medicineDetailUiState.value = _medicineDetailUiState.value.copy(
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

    fun clearSearchHistory() {
        viewModelScope.launch {
            clearSearchHistoryUseCase()
        }
    }

    fun updateSearchQuery(query: String) {
        _searchUiState.value = _searchUiState.value.copy(query = query)
    }

    fun clearSearch() {
        _searchUiState.value = _searchUiState.value.copy(
            query = "",
            searchResults = emptyList(),
            isSearching = false
        )
    }

    fun clearError() {
        _medicineUiState.value = _medicineUiState.value.copy(error = null)
        _medicineDetailUiState.value = _medicineDetailUiState.value.copy(error = null)
        _searchUiState.value = _searchUiState.value.copy(error = null)
        _aiAnalysisUiState.value = _aiAnalysisUiState.value.copy(error = null)
    }

    fun clearAnalysisResult() {
        _aiAnalysisUiState.value = AiAnalysisUiState()
    }

    private fun loadRecentSearches() {
        viewModelScope.launch {
            recentlySearched.collect { medicines ->
                _searchUiState.value = _searchUiState.value.copy(
                    recentSearches = medicines
                )
            }
        }
    }

    fun analyzeDrugInteractions(primaryMedicine: String, otherMedicines: List<String>) {
        viewModelScope.launch {
            if (isLoggedInUseCase()) {
                analyzeDrugInteractionsUseCase(primaryMedicine, otherMedicines).collect { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            _medicineUiState.value = _medicineUiState.value.copy(
                                isAnalyzing = true,
                                analysisError = null
                            )
                        }
                        is Resource.Success -> {
                            _medicineUiState.value = _medicineUiState.value.copy(
                                isAnalyzing = false,
                                analysisResponse = resource.data,
                                analysisError = null
                            )
                        }
                        is Resource.Error -> {
                            _medicineUiState.value = _medicineUiState.value.copy(
                                isAnalyzing = false,
                                analysisError = resource.message
                            )
                            Timber.e("Drug interaction analysis failed: ${resource.message}")
                        }
                    }
                }
            } else {
                _medicineUiState.value = _medicineUiState.value.copy(
                    analysisError = "User not authenticated. Please log in to continue."
                )
                Timber.e("User not authenticated")
            }
        }
    }

    fun clearDrugInteractionResult() {
        _medicineUiState.value = _medicineUiState.value.copy(
            analysisResponse = null,
            analysisError = null
        )
    }
}
