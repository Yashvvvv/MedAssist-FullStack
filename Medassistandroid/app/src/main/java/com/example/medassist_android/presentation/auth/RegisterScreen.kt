package com.example.medassist_android.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    authViewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val registerUiState by authViewModel.registerUiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    LaunchedEffect(registerUiState.isSuccess) {
        if (registerUiState.isSuccess) {
            authViewModel.clearSuccess()
            onNavigateToHome()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Logo and Title
        Text(
            text = "Create Account",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Join MedAssist Community",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // First Name Field
        OutlinedTextField(
            value = registerUiState.firstName,
            onValueChange = { authViewModel.updateRegisterField("firstName", it) },
            label = { Text("First Name") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "First Name"
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            enabled = !registerUiState.isLoading
        )

        // Last Name Field
        OutlinedTextField(
            value = registerUiState.lastName,
            onValueChange = { authViewModel.updateRegisterField("lastName", it) },
            label = { Text("Last Name") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Last Name"
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            enabled = !registerUiState.isLoading
        )

        // Username Field
        OutlinedTextField(
            value = registerUiState.username,
            onValueChange = { authViewModel.updateRegisterField("username", it) },
            label = { Text("Username") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Username"
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            enabled = !registerUiState.isLoading
        )

        // Email Field
        OutlinedTextField(
            value = registerUiState.email,
            onValueChange = { authViewModel.updateRegisterField("email", it) },
            label = { Text("Email") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = "Email"
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            enabled = !registerUiState.isLoading
        )

        // Phone Number Field
        OutlinedTextField(
            value = registerUiState.phoneNumber,
            onValueChange = { authViewModel.updateRegisterField("phoneNumber", it) },
            label = { Text("Phone Number (Optional)") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "Phone Number"
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            enabled = !registerUiState.isLoading
        )

        // Password Field
        OutlinedTextField(
            value = registerUiState.password,
            onValueChange = { authViewModel.updateRegisterField("password", it) },
            label = { Text("Password") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Password"
                )
            },
            trailingIcon = {
                IconButton(
                    onClick = { authViewModel.togglePasswordVisibility(false) }
                ) {
                    Icon(
                        imageVector = if (registerUiState.showPassword) {
                            Icons.Default.VisibilityOff
                        } else {
                            Icons.Default.Visibility
                        },
                        contentDescription = if (registerUiState.showPassword) {
                            "Hide password"
                        } else {
                            "Show password"
                        }
                    )
                }
            },
            visualTransformation = if (registerUiState.showPassword) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            enabled = !registerUiState.isLoading
        )

        // Confirm Password Field
        OutlinedTextField(
            value = registerUiState.confirmPassword,
            onValueChange = { authViewModel.updateRegisterField("confirmPassword", it) },
            label = { Text("Confirm Password") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Confirm Password"
                )
            },
            trailingIcon = {
                IconButton(
                    onClick = { authViewModel.toggleConfirmPasswordVisibility() }
                ) {
                    Icon(
                        imageVector = if (registerUiState.showConfirmPassword) {
                            Icons.Default.VisibilityOff
                        } else {
                            Icons.Default.Visibility
                        },
                        contentDescription = if (registerUiState.showConfirmPassword) {
                            "Hide confirm password"
                        } else {
                            "Show confirm password"
                        }
                    )
                }
            },
            visualTransformation = if (registerUiState.showConfirmPassword) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (isFormValid(registerUiState)) {
                        authViewModel.register(
                            registerUiState.username,
                            registerUiState.email,
                            registerUiState.password,
                            registerUiState.firstName,
                            registerUiState.lastName,
                            registerUiState.phoneNumber.ifBlank { null }
                        )
                    }
                }
            ),
            enabled = !registerUiState.isLoading,
            isError = !registerUiState.passwordsMatch
        )

        // Password Match Error
        if (!registerUiState.passwordsMatch) {
            Text(
                text = "Passwords do not match",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        }

        // Error Message
        registerUiState.error?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Register Button
        Button(
            onClick = {
                if (isFormValid(registerUiState)) {
                    authViewModel.register(
                        registerUiState.username,
                        registerUiState.email,
                        registerUiState.password,
                        registerUiState.firstName,
                        registerUiState.lastName,
                        registerUiState.phoneNumber.ifBlank { null }
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = !registerUiState.isLoading && isFormValid(registerUiState)
        ) {
            if (registerUiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    text = "Register",
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Login Link
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Already have an account? ",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(
                onClick = onNavigateToLogin,
                enabled = !registerUiState.isLoading
            ) {
                Text(
                    text = "Login",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun isFormValid(registerUiState: RegisterUiState): Boolean {
    return registerUiState.username.isNotBlank() &&
           registerUiState.email.isNotBlank() &&
           registerUiState.password.isNotBlank() &&
           registerUiState.confirmPassword.isNotBlank() &&
           registerUiState.firstName.isNotBlank() &&
           registerUiState.lastName.isNotBlank() &&
           registerUiState.passwordsMatch
}
