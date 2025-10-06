package com.example.medassist_android.domain.usecase.auth

import com.example.medassist_android.data.model.AuthResponse
import com.example.medassist_android.data.model.RegistrationResponse
import com.example.medassist_android.data.model.LoginRequest
import com.example.medassist_android.data.repository.AuthRepository
import com.example.medassist_android.util.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(usernameOrEmail: String, password: String): Flow<Resource<AuthResponse>> {
        return authRepository.login(LoginRequest(usernameOrEmail, password))
    }
}

class RegisterUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(
        username: String,
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        phoneNumber: String? = null
    ): Flow<Resource<RegistrationResponse>> {
        return authRepository.register(
            com.example.medassist_android.data.model.RegisterRequest(
                username = username,
                email = email,
                password = password,
                firstName = firstName,
                lastName = lastName,
                phoneNumber = phoneNumber
            )
        )
    }
}

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<Resource<Boolean>> {
        return authRepository.logout()
    }
}

class GetCurrentUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<Resource<com.example.medassist_android.data.model.User>> {
        return authRepository.getCurrentUser()
    }
}

class IsLoggedInUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Boolean {
        return authRepository.isLoggedIn()
    }

    val isLoggedInFlow: Flow<Boolean> = authRepository.isLoggedInFlow
}

class ForgotPasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(email: String): Flow<Resource<com.example.medassist_android.data.model.ForgotPasswordResponse>> {
        return authRepository.forgotPassword(
            com.example.medassist_android.data.model.ForgotPasswordRequest(email)
        )
    }
}

class ResetPasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(token: String, newPassword: String): Flow<Resource<com.example.medassist_android.data.model.ApiResponse<Nothing>>> {
        return authRepository.resetPassword(
            com.example.medassist_android.data.model.ResetPasswordRequest(token, newPassword)
        )
    }
}

class ChangePasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(currentPassword: String, newPassword: String): Flow<Resource<com.example.medassist_android.data.model.ApiResponse<Nothing>>> {
        return authRepository.changePassword(
            com.example.medassist_android.data.model.ChangePasswordRequest(currentPassword, newPassword)
        )
    }
}
