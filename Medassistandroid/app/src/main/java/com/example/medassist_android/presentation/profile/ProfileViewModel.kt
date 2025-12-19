package com.example.medassist_android.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medassist_android.data.model.User
import com.example.medassist_android.data.model.UserProfile
import com.example.medassist_android.domain.usecase.auth.GetCurrentUserUseCase
import com.example.medassist_android.domain.usecase.auth.GetUserProfileUseCase
import com.example.medassist_android.domain.usecase.auth.UpdateProfileUseCase
import com.example.medassist_android.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class ProfileUiState(
    val user: User? = null,
    val userProfile: UserProfile? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class EditProfileUiState(
    val firstName: String = "",
    val lastName: String = "",
    val phoneNumber: String = "",
    val medicalSpecialty: String = "",
    val hospitalAffiliation: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase
) : ViewModel() {

    private val _profileUiState = MutableStateFlow(ProfileUiState())
    val profileUiState: StateFlow<ProfileUiState> = _profileUiState.asStateFlow()

    private val _editProfileUiState = MutableStateFlow(EditProfileUiState())
    val editProfileUiState: StateFlow<EditProfileUiState> = _editProfileUiState.asStateFlow()

    init {
        loadCurrentUser()
    }

    fun loadCurrentUser() {
        viewModelScope.launch {
            getCurrentUserUseCase().collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _profileUiState.value = _profileUiState.value.copy(isLoading = true)
                    }
                    is Resource.Success -> {
                        _profileUiState.value = _profileUiState.value.copy(
                            isLoading = false,
                            user = resource.data,
                            error = null
                        )
                        // Populate edit form with current user data
                        resource.data?.let { user ->
                            _editProfileUiState.value = _editProfileUiState.value.copy(
                                firstName = user.firstName,
                                lastName = user.lastName,
                                phoneNumber = user.phoneNumber ?: "",
                                medicalSpecialty = user.medicalSpecialty ?: "",
                                hospitalAffiliation = user.hospitalAffiliation ?: ""
                            )
                        }
                    }
                    is Resource.Error -> {
                        _profileUiState.value = _profileUiState.value.copy(
                            isLoading = false,
                            error = resource.message
                        )
                    }
                }
            }
        }
    }

    fun updateEditField(field: String, value: String) {
        _editProfileUiState.value = when (field) {
            "firstName" -> _editProfileUiState.value.copy(firstName = value)
            "lastName" -> _editProfileUiState.value.copy(lastName = value)
            "phoneNumber" -> _editProfileUiState.value.copy(phoneNumber = value)
            "medicalSpecialty" -> _editProfileUiState.value.copy(medicalSpecialty = value)
            "hospitalAffiliation" -> _editProfileUiState.value.copy(hospitalAffiliation = value)
            else -> _editProfileUiState.value
        }
    }

    fun saveProfile() {
        val currentState = _editProfileUiState.value
        
        viewModelScope.launch {
            updateProfileUseCase(
                firstName = currentState.firstName.ifBlank { null },
                lastName = currentState.lastName.ifBlank { null },
                phoneNumber = currentState.phoneNumber.ifBlank { null },
                medicalSpecialty = currentState.medicalSpecialty.ifBlank { null },
                hospitalAffiliation = currentState.hospitalAffiliation.ifBlank { null }
            ).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _editProfileUiState.value = _editProfileUiState.value.copy(
                            isLoading = true,
                            error = null
                        )
                    }
                    is Resource.Success -> {
                        _editProfileUiState.value = _editProfileUiState.value.copy(
                            isLoading = false,
                            isSuccess = true,
                            error = null
                        )
                        // Refresh user data
                        loadCurrentUser()
                        Timber.d("Profile updated successfully")
                    }
                    is Resource.Error -> {
                        _editProfileUiState.value = _editProfileUiState.value.copy(
                            isLoading = false,
                            error = resource.message,
                            isSuccess = false
                        )
                        Timber.e("Profile update failed: ${resource.message}")
                    }
                }
            }
        }
    }

    fun clearEditState() {
        _editProfileUiState.value = _editProfileUiState.value.copy(
            isSuccess = false,
            error = null
        )
    }

    fun initializeEditForm() {
        _profileUiState.value.user?.let { user ->
            _editProfileUiState.value = EditProfileUiState(
                firstName = user.firstName,
                lastName = user.lastName,
                phoneNumber = user.phoneNumber ?: "",
                medicalSpecialty = user.medicalSpecialty ?: "",
                hospitalAffiliation = user.hospitalAffiliation ?: ""
            )
        }
    }
}
