package com.example.medassist_android.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * RESET PASSWORD SCREEN
 * 
 * This screen is COMMENTED OUT / DISABLED because the backend email service 
 * for sending password reset tokens is not yet configured.
 * 
 * To enable this feature:
 * 1. Configure an email service (e.g., SendGrid, AWS SES, SMTP) in the backend
 * 2. Uncomment the email sending code in AuthenticationController.java
 * 3. Add this screen to the navigation graph in MainActivity
 * 4. Navigate to this screen from ForgotPasswordScreen after email is sent
 * 
 * Flow:
 * 1. User requests password reset on ForgotPasswordScreen
 * 2. Backend sends email with reset token
 * 3. User clicks link in email or enters token manually
 * 4. User enters new password on this screen
 * 5. Backend validates token and updates password
 */

/*
// Uncomment this entire block when email service is configured

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordScreen(
    // Token can be passed from deep link or entered manually
    resetToken: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val resetPasswordState by authViewModel.resetPasswordUiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    var token by remember { mutableStateOf(resetToken ?: "") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }

    // Validate passwords match
    val passwordsMatch = newPassword == confirmPassword
    val isFormValid = token.isNotBlank() && 
                      newPassword.length >= 8 && 
                      passwordsMatch

    // Navigate to login on success
    LaunchedEffect(resetPasswordState.isSuccess) {
        if (resetPasswordState.isSuccess) {
            kotlinx.coroutines.delay(2000)
            onNavigateToLogin()
        }
    }

    // Clear state when screen loads
    LaunchedEffect(Unit) {
        authViewModel.clearResetPasswordState()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reset Password") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon
            Icon(
                imageVector = Icons.Default.LockReset,
                contentDescription = "Reset Password",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                text = "Create New Password",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Instructions
            Text(
                text = "Enter the reset token from your email and create a new password.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Success Message
            if (resetPasswordState.isSuccess) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Password Reset Successful!",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Redirecting to login...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                
                return@Column
            }

            // Error Message
            resetPasswordState.error?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Reset Token Field (if not provided via deep link)
            if (resetToken.isNullOrBlank()) {
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("Reset Token") },
                    placeholder = { Text("Enter token from email") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "Token"
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    enabled = !resetPasswordState.isLoading
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // New Password Field
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("New Password") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Password"
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showPassword) "Hide password" else "Show password"
                        )
                    }
                },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                enabled = !resetPasswordState.isLoading,
                isError = newPassword.isNotEmpty() && newPassword.length < 8,
                supportingText = {
                    if (newPassword.isNotEmpty() && newPassword.length < 8) {
                        Text("Password must be at least 8 characters")
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Confirm Password Field
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Confirm Password"
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                        Icon(
                            imageVector = if (showConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showConfirmPassword) "Hide password" else "Show password"
                        )
                    }
                },
                visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (isFormValid) {
                            authViewModel.resetPassword(token, newPassword)
                        }
                    }
                ),
                enabled = !resetPasswordState.isLoading,
                isError = confirmPassword.isNotEmpty() && !passwordsMatch,
                supportingText = {
                    if (confirmPassword.isNotEmpty() && !passwordsMatch) {
                        Text("Passwords do not match")
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Reset Button
            Button(
                onClick = { authViewModel.resetPassword(token, newPassword) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = isFormValid && !resetPasswordState.isLoading
            ) {
                if (resetPasswordState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (resetPasswordState.isLoading) "Resetting..." else "Reset Password")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Password Requirements
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Password Requirements:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PasswordRequirement(
                        text = "At least 8 characters",
                        isMet = newPassword.length >= 8
                    )
                    PasswordRequirement(
                        text = "Passwords match",
                        isMet = passwordsMatch && confirmPassword.isNotEmpty()
                    )
                }
            }
        }
    }
}

@Composable
private fun PasswordRequirement(
    text: String,
    isMet: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = if (isMet) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (isMet) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (isMet) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Add this to AuthViewModel when enabling reset password:
/*
data class ResetPasswordUiState(
    val token: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

private val _resetPasswordUiState = MutableStateFlow(ResetPasswordUiState())
val resetPasswordUiState: StateFlow<ResetPasswordUiState> = _resetPasswordUiState.asStateFlow()

fun resetPassword(token: String, newPassword: String) {
    viewModelScope.launch {
        resetPasswordUseCase(token, newPassword).collect { resource ->
            when (resource) {
                is Resource.Loading -> {
                    _resetPasswordUiState.value = _resetPasswordUiState.value.copy(
                        isLoading = true,
                        error = null
                    )
                }
                is Resource.Success -> {
                    _resetPasswordUiState.value = _resetPasswordUiState.value.copy(
                        isLoading = false,
                        isSuccess = true,
                        error = null
                    )
                    Timber.d("Password reset successful")
                }
                is Resource.Error -> {
                    _resetPasswordUiState.value = _resetPasswordUiState.value.copy(
                        isLoading = false,
                        error = resource.message,
                        isSuccess = false
                    )
                    Timber.e("Password reset failed: ${resource.message}")
                }
            }
        }
    }
}

fun clearResetPasswordState() {
    _resetPasswordUiState.value = ResetPasswordUiState()
}
*/

// Add this navigation route in MainActivity when enabling:
/*
composable("reset_password/{token}") { backStackEntry ->
    val token = backStackEntry.arguments?.getString("token")
    ResetPasswordScreen(
        resetToken = token,
        onNavigateBack = { navController.popBackStack() },
        onNavigateToLogin = { 
            navController.navigate("login") {
                popUpTo("login") { inclusive = true }
            }
        },
        authViewModel = authViewModel
    )
}

// Or without token (manual entry):
composable("reset_password") {
    ResetPasswordScreen(
        onNavigateBack = { navController.popBackStack() },
        onNavigateToLogin = { 
            navController.navigate("login") {
                popUpTo("login") { inclusive = true }
            }
        },
        authViewModel = authViewModel
    )
}
*/

*/
