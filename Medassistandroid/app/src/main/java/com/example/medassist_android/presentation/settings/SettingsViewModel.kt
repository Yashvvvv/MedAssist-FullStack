package com.example.medassist_android.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medassist_android.data.local.UserPreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: UserPreferencesManager
) : ViewModel() {

    val preferences = preferencesManager.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferencesManager.UserPreferences()
        )

    fun updateThemeMode(mode: UserPreferencesManager.ThemeMode) {
        viewModelScope.launch {
            preferencesManager.setThemeMode(mode)
        }
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setNotificationsEnabled(enabled)
        }
    }

    fun updateReminderSound(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setReminderSoundEnabled(enabled)
        }
    }

    fun updateVibrate(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setReminderVibrationEnabled(enabled)
        }
    }

    fun updateLanguage(language: String) {
        viewModelScope.launch {
            preferencesManager.setLanguageCode(language)
        }
    }

    fun updateReminderAdvanceMinutes(minutes: Int) {
        viewModelScope.launch {
            preferencesManager.setReminderSnoozeDuration(minutes)
        }
    }
}
