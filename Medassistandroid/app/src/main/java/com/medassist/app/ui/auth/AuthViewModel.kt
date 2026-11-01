package com.medassist.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medassist.app.core.auth.SessionManager
import com.medassist.app.core.model.ApiError
import com.medassist.app.core.model.AuthResponse
import com.medassist.app.core.model.BranchRequest
import com.medassist.app.core.model.CreateOrganizationRequest
import com.medassist.app.core.model.RegisterPharmacyRequest
import com.medassist.app.core.model.RegisterRequest
import com.medassist.app.core.model.toApiError
import com.medassist.app.core.repository.AuthRepository
import com.medassist.app.data.LocationProvider
import com.medassist.app.ui.common.message
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val busy: Boolean = false,
    val error: String? = null,
    /** Server-side field errors, keyed by our field names. */
    val fieldErrors: Map<String, String> = emptyMap(),
    val info: String? = null,
)

/** Pharmacy signup form, one object so the multi-section screen survives recomposition. */
data class PharmacyForm(
    val ownerName: String = "",
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val pharmacyName: String = "",
    val gstin: String = "",
    val drugLicenseNo: String = "",
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val delivery: Boolean = true,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val session: SessionManager,
    private val location: LocationProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    private val _pharmacy = MutableStateFlow(PharmacyForm())
    val pharmacy: StateFlow<PharmacyForm> = _pharmacy.asStateFlow()

    fun updatePharmacy(transform: (PharmacyForm) -> PharmacyForm) = _pharmacy.update(transform)

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _state.value = AuthUiState(error = "Enter your email and password.")
            return
        }
        submit { auth.login(email, password) }
    }

    fun registerPatient(name: String, email: String, phone: String, password: String) {
        val problem = validateAccount(name, email, password, phone)
        if (problem != null) {
            _state.value = AuthUiState(fieldErrors = problem)
            return
        }
        submit { auth.register(RegisterRequest(email.trim(), password, name.trim(), phone.trim().ifBlank { null })) }
    }

    fun registerPharmacy() {
        val f = _pharmacy.value
        val errors = (validateAccount(f.ownerName, f.email, f.password, f.phone) ?: emptyMap()).toMutableMap()
        if (f.pharmacyName.isBlank()) errors["pharmacyName"] = "Enter the pharmacy's name"
        if (f.address.isBlank()) errors["address"] = "Enter the shop address"
        if (f.city.isBlank()) errors["city"] = "Enter the city"
        if (f.state.isBlank()) errors["state"] = "Enter the state"
        if (!Regex("^[1-9][0-9]{5}$").matches(f.pincode.trim())) errors["pincode"] = "6-digit PIN code"
        if (f.gstin.isNotBlank() && !Regex("^[0-9]{2}[A-Z0-9]{10}[0-9A-Z]Z[0-9A-Z]$").matches(f.gstin.trim().uppercase())) {
            errors["gstin"] = "15-character GSTIN, e.g. 09ABCDE1234F1Z5"
        }
        if (errors.isNotEmpty()) {
            _state.value = AuthUiState(fieldErrors = errors, error = "Some details need fixing.")
            return
        }
        val req = RegisterPharmacyRequest(
            owner = RegisterRequest(f.email.trim(), f.password, f.ownerName.trim(), f.phone.trim().ifBlank { null }),
            organization = CreateOrganizationRequest(
                name = f.pharmacyName.trim(),
                gstin = f.gstin.trim().uppercase().ifBlank { null },
                drugLicenseNo = f.drugLicenseNo.trim().ifBlank { null },
                phone = f.phone.trim().ifBlank { null },
                email = f.email.trim(),
                firstBranch = BranchRequest(
                    name = "Main store",
                    addressLine = f.address.trim(),
                    city = f.city.trim(),
                    state = f.state.trim(),
                    pincode = f.pincode.trim(),
                    latitude = f.latitude,
                    longitude = f.longitude,
                    phone = f.phone.trim().ifBlank { null },
                    deliveryAvailable = f.delivery,
                    publiclyListed = true,
                ),
            ),
        )
        submit { auth.registerPharmacy(req) }
    }

    /** Puts the shop on the map for "pharmacies near me", using where the owner is standing. */
    fun useCurrentLocation() {
        viewModelScope.launch {
            val point = location.current()
            if (point == null) {
                _state.update { it.copy(info = "Couldn't get your location. You can add it later in settings.") }
            } else {
                _pharmacy.update { it.copy(latitude = point.lat, longitude = point.lng) }
                _state.update { it.copy(info = "Shop location saved.") }
            }
        }
    }

    fun forgotPassword(email: String) {
        if (email.isBlank()) {
            _state.value = AuthUiState(error = "Enter your email first.")
            return
        }
        viewModelScope.launch {
            _state.value = AuthUiState(busy = true)
            auth.forgotPassword(email)
            // Same answer whether or not the account exists, so the form can't be used to probe emails.
            _state.value = AuthUiState(info = "If an account exists for $email, a reset link is on its way.")
        }
    }

    fun clearMessages() = _state.update { it.copy(error = null, info = null) }

    private fun submit(call: suspend () -> Result<AuthResponse>) {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.value = AuthUiState(busy = true)
            call()
                .onSuccess { session.signIn(it) } // AppRoot switches to the right app on its own
                .onFailure { e ->
                    val error = e.toApiError()
                    _state.value = when {
                        error == ApiError.Unauthorized -> AuthUiState(error = "Wrong email or password.")
                        error is ApiError.Validation -> AuthUiState(error = error.message(), fieldErrors = error.fields)
                        else -> AuthUiState(error = error.message())
                    }
                }
        }
    }

    private fun validateAccount(name: String, email: String, password: String, phone: String): Map<String, String>? {
        val errors = mutableMapOf<String, String>()
        if (name.isBlank()) errors["fullName"] = "Enter your name"
        if (!Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(email.trim())) errors["email"] = "Enter a valid email"
        if (!Regex("^(?=.*[A-Za-z])(?=.*\\d).{8,72}$").matches(password)) errors["password"] = "At least 8 characters, with a letter and a number"
        if (phone.isNotBlank() && !Regex("^\\+?[0-9]{10,13}$").matches(phone.trim())) errors["phone"] = "10-digit mobile number"
        return errors.ifEmpty { null }
    }
}
