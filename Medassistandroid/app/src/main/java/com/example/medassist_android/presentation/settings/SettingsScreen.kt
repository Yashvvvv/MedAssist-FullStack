package com.example.medassist_android.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.medassist_android.data.local.UserPreferencesManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToChangePassword: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showReminderAdvanceDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Settings") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Appearance Section
            SettingsSection(title = "Appearance") {
                SettingsItem(
                    icon = Icons.Default.DarkMode,
                    title = "Theme",
                    subtitle = when (preferences.themeMode) {
                        UserPreferencesManager.ThemeMode.DARK -> "Dark"
                        UserPreferencesManager.ThemeMode.LIGHT -> "Light"
                        UserPreferencesManager.ThemeMode.SYSTEM -> "System"
                    },
                    onClick = { showThemeDialog = true }
                )
            }

            // Notifications Section
            SettingsSection(title = "Notifications") {
                SettingsSwitchItem(
                    icon = Icons.Default.Notifications,
                    title = "Enable Notifications",
                    subtitle = "Receive medicine reminders",
                    checked = preferences.notificationsEnabled,
                    onCheckedChange = { viewModel.updateNotificationsEnabled(it) }
                )

                if (preferences.notificationsEnabled) {
                    SettingsSwitchItem(
                        icon = Icons.Default.VolumeUp,
                        title = "Reminder Sound",
                        subtitle = "Play sound for reminders",
                        checked = preferences.reminderSoundEnabled,
                        onCheckedChange = { viewModel.updateReminderSound(it) }
                    )

                    SettingsSwitchItem(
                        icon = Icons.Default.Vibration,
                        title = "Vibration",
                        subtitle = "Vibrate for reminders",
                        checked = preferences.reminderVibrationEnabled,
                        onCheckedChange = { viewModel.updateVibrate(it) }
                    )

                    SettingsItem(
                        icon = Icons.Default.Timer,
                        title = "Snooze Duration",
                        subtitle = "${preferences.reminderSnoozeDuration} minutes",
                        onClick = { showReminderAdvanceDialog = true }
                    )
                }
            }

            // Account Section
            SettingsSection(title = "Account") {
                SettingsItem(
                    icon = Icons.Default.Lock,
                    title = "Change Password",
                    subtitle = "Update your password",
                    onClick = onNavigateToChangePassword
                )
            }

            // Language Section
            SettingsSection(title = "Language") {
                SettingsItem(
                    icon = Icons.Default.Language,
                    title = "Language",
                    subtitle = getLanguageName(preferences.languageCode),
                    onClick = { showLanguageDialog = true }
                )
            }

            // About Section
            SettingsSection(title = "About") {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "About MedAssist",
                    subtitle = "Version 1.0.0",
                    onClick = { showAboutDialog = true }
                )

                SettingsItem(
                    icon = Icons.Default.Description,
                    title = "Privacy Policy",
                    subtitle = "View our privacy policy",
                    onClick = { /* Open privacy policy */ }
                )

                SettingsItem(
                    icon = Icons.Default.Gavel,
                    title = "Terms of Service",
                    subtitle = "View terms and conditions",
                    onClick = { /* Open terms of service */ }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Theme Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Select Theme") },
            text = {
                Column {
                    listOf(
                        UserPreferencesManager.ThemeMode.LIGHT to "Light",
                        UserPreferencesManager.ThemeMode.DARK to "Dark",
                        UserPreferencesManager.ThemeMode.SYSTEM to "System"
                    ).forEach { (mode, name) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateThemeMode(mode)
                                    showThemeDialog = false
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = preferences.themeMode == mode,
                                onClick = {
                                    viewModel.updateThemeMode(mode)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Language Dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Select Language") },
            text = {
                Column {
                    listOf(
                        "en" to "English",
                        "es" to "Español",
                        "fr" to "Français",
                        "de" to "Deutsch",
                        "ar" to "العربية"
                    ).forEach { (code, name) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateLanguage(code)
                                    showLanguageDialog = false
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = preferences.languageCode == code,
                                onClick = {
                                    viewModel.updateLanguage(code)
                                    showLanguageDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Reminder Advance Dialog
    if (showReminderAdvanceDialog) {
        AlertDialog(
            onDismissRequest = { showReminderAdvanceDialog = false },
            title = { Text("Snooze Duration") },
            text = {
                Column {
                    listOf(5, 10, 15, 30, 60).forEach { minutes ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateReminderAdvanceMinutes(minutes)
                                    showReminderAdvanceDialog = false
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = preferences.reminderSnoozeDuration == minutes,
                                onClick = {
                                    viewModel.updateReminderAdvanceMinutes(minutes)
                                    showReminderAdvanceDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                if (minutes == 60) "1 hour" 
                                else "$minutes minutes"
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showReminderAdvanceDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About MedAssist") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Medication,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "MedAssist",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "Version 1.0.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Your personal medicine assistant. Track your medications, set reminders, and stay healthy.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

private fun getLanguageName(code: String): String {
    return when (code) {
        "en" -> "English"
        "es" -> "Español"
        "fr" -> "Français"
        "de" -> "Deutsch"
        "ar" -> "العربية"
        else -> "English"
    }
}
