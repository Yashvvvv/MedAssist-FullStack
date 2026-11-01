package com.medassist.app.ui.auth

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.rounded.LocalPharmacy
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.medassist.app.ui.common.PrimaryButton

@Composable
fun AuthNavHost() {
    val nav = rememberNavController()
    NavHost(nav, startDestination = "welcome") {
        composable("welcome") {
            WelcomeScreen(
                onPatient = { nav.navigate("register-patient") },
                onPharmacy = { nav.navigate("register-pharmacy") },
                onLogin = { nav.navigate("login") },
            )
        }
        composable("login") { LoginScreen(onBack = nav::popBackStack, onRegister = { nav.navigate("welcome") { popUpTo("welcome") } }) }
        composable("register-patient") { RegisterPatientScreen(onBack = nav::popBackStack, onLogin = { nav.navigate("login") }) }
        composable("register-pharmacy") { RegisterPharmacyScreen(onBack = nav::popBackStack, onLogin = { nav.navigate("login") }) }
    }
}

@Composable
private fun WelcomeScreen(onPatient: () -> Unit, onPharmacy: () -> Unit, onLogin: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(scheme.primaryContainer, scheme.background)))
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Spacer(Modifier.height(32.dp))
        Surface(shape = RoundedCornerShape(24.dp), color = scheme.primary, modifier = Modifier.size(72.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.LocalPharmacy, null, tint = scheme.onPrimary, modifier = Modifier.size(40.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("MedAssist", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Your neighbourhood pharmacy, online. Find medicines nearby and order for pickup or delivery, " +
                "or run your whole pharmacy from your phone.",
            style = MaterialTheme.typography.bodyLarge,
            color = scheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(40.dp))
        Text("Get started", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        RoleCard(Icons.Rounded.Person, "I need medicines", "Find pharmacies near you, check stock, order and track.", onPatient)
        Spacer(Modifier.height(12.dp))
        RoleCard(Icons.Rounded.Storefront, "I run a pharmacy", "GST billing, stock and expiry, online orders, reports. 14-day free trial.", onPharmacy)
        Spacer(Modifier.weight(1f, fill = false))
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text("Already have an account?", color = scheme.onSurfaceVariant)
            TextButton(onClick = onLogin) { Text("Log in") }
        }
    }
}

@Composable
private fun RoleCard(icon: ImageVector, title: String, body: String, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthScaffold(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = { Text(title) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
        )
        Column(
            Modifier
                .fillMaxSize()
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) { content() }
    }
}

@Composable
private fun LoginScreen(onBack: () -> Unit, onRegister: () -> Unit, vm: AuthViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    AuthScaffold("Log in", onBack) {
        Text("Welcome back", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(4.dp))
        Text("Pharmacy staff and customers use the same login.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        FormField(email, { email = it }, "Email", keyboardType = KeyboardType.Email)
        PasswordField(password, { password = it }, onDone = { vm.login(email, password) })
        Messages(state)
        Spacer(Modifier.height(16.dp))
        PrimaryButton("Log in", { vm.login(email, password) }, Modifier.fillMaxWidth(), loading = state.busy)
        TextButton(onClick = { vm.forgotPassword(email) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Forgot password?")
        }
        Spacer(Modifier.height(24.dp))
        DemoCard(onUse = { e -> email = e; password = "Demo@12345" })
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onRegister, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("New here? Create an account") }
    }
}

/** Recruiters and testers can try both sides without signing up. */
@Composable
private fun DemoCard(onUse: (String) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(16.dp)) {
            Text("Just looking?", style = MaterialTheme.typography.titleSmall)
            Text(
                "Try the demo pharmacy in Noida, or the demo customer account.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onUse("demo@medassist.app") }) { Text("Pharmacy demo") }
                OutlinedButton(onClick = { onUse("patient@medassist.app") }) { Text("Customer demo") }
            }
        }
    }
}

@Composable
private fun RegisterPatientScreen(onBack: () -> Unit, onLogin: () -> Unit, vm: AuthViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    AuthScaffold("Create account", onBack) {
        Text("Order medicines from pharmacies near you", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(20.dp))
        FormField(name, { name = it }, "Full name", error = state.fieldErrors["fullName"])
        FormField(email, { email = it }, "Email", keyboardType = KeyboardType.Email, error = state.fieldErrors["email"])
        FormField(phone, { phone = it }, "Mobile number", keyboardType = KeyboardType.Phone, error = state.fieldErrors["phone"],
            supporting = "Pharmacies call this number about your order")
        PasswordField(password, { password = it }, error = state.fieldErrors["password"], onDone = { vm.registerPatient(name, email, phone, password) })
        Messages(state)
        Spacer(Modifier.height(16.dp))
        PrimaryButton("Create account", { vm.registerPatient(name, email, phone, password) }, Modifier.fillMaxWidth(), loading = state.busy)
        TextButton(onClick = onLogin, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("I already have an account") }
    }
}

@Composable
private fun RegisterPharmacyScreen(onBack: () -> Unit, onLogin: () -> Unit, vm: AuthViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val f by vm.pharmacy.collectAsStateWithLifecycle()
    val e = state.fieldErrors
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.useCurrentLocation() }
    AuthScaffold("Register your pharmacy", onBack) {
        Text("Set up in two minutes", style = MaterialTheme.typography.titleLarge)
        Text("14 days of PRO free. No card needed.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        Text("You", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        FormField(f.ownerName, { v -> vm.updatePharmacy { it.copy(ownerName = v) } }, "Your name", error = e["fullName"] ?: e["owner.fullName"])
        FormField(f.email, { v -> vm.updatePharmacy { it.copy(email = v) } }, "Email", keyboardType = KeyboardType.Email, error = e["email"] ?: e["owner.email"])
        FormField(f.phone, { v -> vm.updatePharmacy { it.copy(phone = v) } }, "Mobile number", keyboardType = KeyboardType.Phone, error = e["phone"])
        PasswordField(f.password, { v -> vm.updatePharmacy { it.copy(password = v) } }, error = e["password"] ?: e["owner.password"])
        Spacer(Modifier.height(12.dp))
        Text("Pharmacy", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        FormField(f.pharmacyName, { v -> vm.updatePharmacy { it.copy(pharmacyName = v) } }, "Pharmacy name", error = e["pharmacyName"])
        FormField(f.gstin, { v -> vm.updatePharmacy { it.copy(gstin = v.uppercase()) } }, "GSTIN (optional)", error = e["gstin"] ?: e["organization.gstin"],
            supporting = "Printed on GST invoices")
        FormField(f.drugLicenseNo, { v -> vm.updatePharmacy { it.copy(drugLicenseNo = v) } }, "Drug licence no. (optional)")
        FormField(f.address, { v -> vm.updatePharmacy { it.copy(address = v) } }, "Shop address", error = e["address"])
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FormField(f.city, { v -> vm.updatePharmacy { it.copy(city = v) } }, "City", Modifier.weight(1f), error = e["city"])
            FormField(f.pincode, { v -> vm.updatePharmacy { it.copy(pincode = v.filter(Char::isDigit).take(6)) } }, "PIN code",
                Modifier.weight(1f), keyboardType = KeyboardType.Number, error = e["pincode"])
        }
        FormField(f.state, { v -> vm.updatePharmacy { it.copy(state = v) } }, "State", error = e["state"])
        OutlinedButton(
            onClick = { permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Rounded.MyLocation, null)
            Spacer(Modifier.width(8.dp))
            Text(if (f.latitude != null) "Shop location saved ✓" else "I'm at the shop: use my location")
        }
        Text(
            "Puts you on the map, so customers nearby can find you and order.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Home delivery", style = MaterialTheme.typography.bodyLarge)
                Text("Customers can choose delivery as well as pickup", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(f.delivery, { v -> vm.updatePharmacy { it.copy(delivery = v) } })
        }
        Messages(state)
        Spacer(Modifier.height(16.dp))
        PrimaryButton("Create pharmacy", vm::registerPharmacy, Modifier.fillMaxWidth(), loading = state.busy)
        TextButton(onClick = onLogin, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("I already have an account") }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Messages(state: AuthUiState) {
    state.error?.let {
        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
    }
    state.info?.let {
        Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
fun FormField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    error: String? = null,
    supporting: String? = null,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        singleLine = singleLine,
        isError = error != null,
        supportingText = (error ?: supporting)?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
        shape = MaterialTheme.shapes.medium,
    )
}

@Composable
private fun PasswordField(value: String, onChange: (String) -> Unit, error: String? = null, onDone: () -> Unit = {}) {
    var visible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text("Password") },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, if (visible) "Hide password" else "Show password")
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { onDone() }),
        shape = MaterialTheme.shapes.medium,
    )
}
