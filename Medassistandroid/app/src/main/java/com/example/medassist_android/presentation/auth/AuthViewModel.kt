package com.example.medassist_android.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medassist_android.domain.usecase.auth.*
import com.example.medassist_android.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val user: com.example.medassist_android.data.model.User? = null,
    val error: String? = null,
    val isSuccess: Boolean = false
)

data class LoginUiState(
    val usernameOrEmail: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val showPassword: Boolean = false
)

data class RegisterUiState(
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val phoneNumber: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val showPassword: Boolean = false,
    val showConfirmPassword: Boolean = false,
    val passwordsMatch: Boolean = true
)

data class ForgotPasswordUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val isLoggedInUseCase: IsLoggedInUseCase,
    private val forgotPasswordUseCase: ForgotPasswordUseCase,
    private val changePasswordUseCase: ChangePasswordUseCase
) : ViewModel() {

    private val _authUiState = MutableStateFlow(AuthUiState())
    val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()

    private val _loginUiState = MutableStateFlow(LoginUiState())
    val loginUiState: StateFlow<LoginUiState> = _loginUiState.asStateFlow()

    private val _registerUiState = MutableStateFlow(RegisterUiState())
    val registerUiState: StateFlow<RegisterUiState> = _registerUiState.asStateFlow()

    private val _forgotPasswordUiState = MutableStateFlow(ForgotPasswordUiState())
    val forgotPasswordUiState: StateFlow<ForgotPasswordUiState> = _forgotPasswordUiState.asStateFlow()

    val isLoggedIn: StateFlow<Boolean> = isLoggedInUseCase.isLoggedInFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    init {
        checkAuthStatus()
    }

    fun checkAuthStatus() {
        viewModelScope.launch {
            val loggedIn = isLoggedInUseCase()
            _authUiState.value = _authUiState.value.copy(isLoggedIn = loggedIn)

            if (loggedIn) {
                getCurrentUser()
            }
        }
    }

    fun login(usernameOrEmail: String, password: String) {
        viewModelScope.launch {
            loginUseCase(usernameOrEmail, password).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _loginUiState.value = _loginUiState.value.copy(
                            isLoading = true,
                            error = null
                        )
                    }
                    is Resource.Success -> {
                        _loginUiState.value = _loginUiState.value.copy(
                            isLoading = false,
                            isSuccess = true,
                            error = null
                        )
                        _authUiState.value = _authUiState.value.copy(
                            isLoggedIn = true,
                            user = resource.data?.data?.user
                        )
                        Timber.d("Login successful")
                    }
                    is Resource.Error -> {
                        _loginUiState.value = _loginUiState.value.copy(
                            isLoading = false,
                            error = resource.message,
                            isSuccess = false
                        )
                        Timber.e("Login failed: ${resource.message}")
                    }
                }
            }
        }
    }

    fun register(
        username: String,
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        phoneNumber: String?
    ) {
        viewModelScope.launch {
            registerUseCase(username, email, password, firstName, lastName, phoneNumber)
                .collect { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            _registerUiState.value = _registerUiState.value.copy(
                                isLoading = true,
                                error = null
                            )
                        }
                        is Resource.Success -> {
                            _registerUiState.value = _registerUiState.value.copy(
                                isLoading = false,
                                isSuccess = true,
                                error = null
                            )
                            // Note: Registration successful but user needs to verify email
                            // No token saving here since user isn't authenticated yet
                            Timber.d("Registration successful: ${resource.data?.message}")
                        }
                        is Resource.Error -> {
                            _registerUiState.value = _registerUiState.value.copy(
                                isLoading = false,
                                error = resource.message,
                                isSuccess = false
                            )
                            Timber.e("Registration failed: ${resource.message}")
                        }
                    }
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            logoutUseCase().collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _authUiState.value = _authUiState.value.copy(isLoading = true)
                    }
                    is Resource.Success -> {
                        _authUiState.value = AuthUiState(isLoggedIn = false)
                        _loginUiState.value = LoginUiState()
                        _registerUiState.value = RegisterUiState()
                        Timber.d("Logout successful")
                    }
                    is Resource.Error -> {
                        _authUiState.value = _authUiState.value.copy(
                            isLoading = false,
                            error = resource.message
                        )
                        Timber.e("Logout failed: ${resource.message}")
                    }
                }
            }
        }
    }

    fun getCurrentUser() {
        viewModelScope.launch {
            getCurrentUserUseCase().collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _authUiState.value = _authUiState.value.copy(isLoading = true)
                    }
                    is Resource.Success -> {
                        _authUiState.value = _authUiState.value.copy(
                            isLoading = false,
                            user = resource.data,
                            error = null
                        )
                    }
                    is Resource.Error -> {
                        _authUiState.value = _authUiState.value.copy(
                            isLoading = false,
                            error = resource.message
                        )
                    }
                }
            }
        }
    }

    fun forgotPassword(email: String) {
        viewModelScope.launch {
            forgotPasswordUseCase(email).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _forgotPasswordUiState.value = _forgotPasswordUiState.value.copy(
                            isLoading = true,
                            error = null
                        )
                    }
                    is Resource.Success -> {
                        _forgotPasswordUiState.value = _forgotPasswordUiState.value.copy(
                            isLoading = false,
                            isSuccess = true,
                            error = null
                        )
                        Timber.d("Forgot password request successful")
                    }
                    is Resource.Error -> {
                        _forgotPasswordUiState.value = _forgotPasswordUiState.value.copy(
                            isLoading = false,
                            error = resource.message,
                            isSuccess = false
                        )
                        Timber.e("Forgot password request failed: ${resource.message}")
                    }
                }
            }
        }
    }

    fun updateLoginField(field: String, value: String) {
        _loginUiState.value = when (field) {
            "usernameOrEmail" -> _loginUiState.value.copy(usernameOrEmail = value)
            "password" -> _loginUiState.value.copy(password = value)
            else -> _loginUiState.value
        }
    }

    fun updateRegisterField(field: String, value: String) {
        _registerUiState.value = when (field) {
            "username" -> _registerUiState.value.copy(username = value)
            "email" -> _registerUiState.value.copy(email = value)
            "password" -> _registerUiState.value.copy(password = value)
            "confirmPassword" -> _registerUiState.value.copy(
                confirmPassword = value,
                passwordsMatch = value == _registerUiState.value.password
            )
            "firstName" -> _registerUiState.value.copy(firstName = value)
            "lastName" -> _registerUiState.value.copy(lastName = value)
            "phoneNumber" -> _registerUiState.value.copy(phoneNumber = value)
            else -> _registerUiState.value
        }
    }

    fun togglePasswordVisibility(isLogin: Boolean) {
        if (isLogin) {
            _loginUiState.value = _loginUiState.value.copy(
                showPassword = !_loginUiState.value.showPassword
            )
        } else {
            _registerUiState.value = _registerUiState.value.copy(
                showPassword = !_registerUiState.value.showPassword
            )
        }
    }

    fun toggleConfirmPasswordVisibility() {
        _registerUiState.value = _registerUiState.value.copy(
            showConfirmPassword = !_registerUiState.value.showConfirmPassword
        )
    }

    fun clearError() {
        _authUiState.value = _authUiState.value.copy(error = null)
        _loginUiState.value = _loginUiState.value.copy(error = null)
        _registerUiState.value = _registerUiState.value.copy(error = null)
        _forgotPasswordUiState.value = _forgotPasswordUiState.value.copy(error = null)
    }

    fun clearSuccess() {
        _loginUiState.value = _loginUiState.value.copy(isSuccess = false)
        _registerUiState.value = _registerUiState.value.copy(isSuccess = false)
        _forgotPasswordUiState.value = _forgotPasswordUiState.value.copy(isSuccess = false)
    }

    fun clearForgotPasswordState() {
        _forgotPasswordUiState.value = ForgotPasswordUiState()
    }
}
