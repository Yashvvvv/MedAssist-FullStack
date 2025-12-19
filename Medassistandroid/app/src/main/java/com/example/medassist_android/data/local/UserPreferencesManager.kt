package com.example.medassist_android.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

@Singleton
class UserPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.settingsDataStore

    // Preference Keys
    private object PreferencesKeys {
        // Notification Settings
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val REMINDER_SOUND_ENABLED = booleanPreferencesKey("reminder_sound_enabled")
        val REMINDER_VIBRATION_ENABLED = booleanPreferencesKey("reminder_vibration_enabled")
        val REMINDER_SNOOZE_DURATION = intPreferencesKey("reminder_snooze_duration") // in minutes
        val QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
        val QUIET_HOURS_START = stringPreferencesKey("quiet_hours_start") // "22:00"
        val QUIET_HOURS_END = stringPreferencesKey("quiet_hours_end") // "07:00"
        
        // Appearance Settings
        val THEME_MODE = stringPreferencesKey("theme_mode") // "system", "light", "dark"
        val USE_DYNAMIC_COLORS = booleanPreferencesKey("use_dynamic_colors")
        
        // Language Settings
        val LANGUAGE_CODE = stringPreferencesKey("language_code") // "en", "es", "fr", etc.
        
        // Privacy Settings
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val AUTO_LOCK_TIMEOUT = intPreferencesKey("auto_lock_timeout") // in minutes, 0 = disabled
        
        // Medicine Tracking Settings
        val DEFAULT_REMINDER_TIME = stringPreferencesKey("default_reminder_time") // "09:00"
        val SHOW_MEDICINE_IMAGES = booleanPreferencesKey("show_medicine_images")
        val TRACK_SIDE_EFFECTS = booleanPreferencesKey("track_side_effects")
        val TRACK_MOOD = booleanPreferencesKey("track_mood")
        
        // Data & Sync
        val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        val LAST_SYNC_TIME = longPreferencesKey("last_sync_time")
        
        // Onboarding
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val FIRST_LAUNCH_TIME = longPreferencesKey("first_launch_time")
    }

    // Data class for all user preferences
    data class UserPreferences(
        // Notifications
        val notificationsEnabled: Boolean = true,
        val reminderSoundEnabled: Boolean = true,
        val reminderVibrationEnabled: Boolean = true,
        val reminderSnoozeDuration: Int = 10, // minutes
        val quietHoursEnabled: Boolean = false,
        val quietHoursStart: String = "22:00",
        val quietHoursEnd: String = "07:00",
        
        // Appearance
        val themeMode: ThemeMode = ThemeMode.SYSTEM,
        val useDynamicColors: Boolean = true,
        
        // Language
        val languageCode: String = "en",
        
        // Privacy
        val biometricEnabled: Boolean = false,
        val autoLockTimeout: Int = 0, // 0 = disabled
        
        // Medicine Tracking
        val defaultReminderTime: String = "09:00",
        val showMedicineImages: Boolean = true,
        val trackSideEffects: Boolean = true,
        val trackMood: Boolean = false,
        
        // Data & Sync
        val autoBackupEnabled: Boolean = false,
        val lastSyncTime: Long = 0L,
        
        // Onboarding
        val onboardingCompleted: Boolean = false,
        val firstLaunchTime: Long = 0L
    )

    enum class ThemeMode {
        SYSTEM, LIGHT, DARK
    }

    // Flow to observe all preferences
    val userPreferencesFlow: Flow<UserPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            UserPreferences(
                notificationsEnabled = preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true,
                reminderSoundEnabled = preferences[PreferencesKeys.REMINDER_SOUND_ENABLED] ?: true,
                reminderVibrationEnabled = preferences[PreferencesKeys.REMINDER_VIBRATION_ENABLED] ?: true,
                reminderSnoozeDuration = preferences[PreferencesKeys.REMINDER_SNOOZE_DURATION] ?: 10,
                quietHoursEnabled = preferences[PreferencesKeys.QUIET_HOURS_ENABLED] ?: false,
                quietHoursStart = preferences[PreferencesKeys.QUIET_HOURS_START] ?: "22:00",
                quietHoursEnd = preferences[PreferencesKeys.QUIET_HOURS_END] ?: "07:00",
                themeMode = preferences[PreferencesKeys.THEME_MODE]?.let { ThemeMode.valueOf(it) } ?: ThemeMode.SYSTEM,
                useDynamicColors = preferences[PreferencesKeys.USE_DYNAMIC_COLORS] ?: true,
                languageCode = preferences[PreferencesKeys.LANGUAGE_CODE] ?: "en",
                biometricEnabled = preferences[PreferencesKeys.BIOMETRIC_ENABLED] ?: false,
                autoLockTimeout = preferences[PreferencesKeys.AUTO_LOCK_TIMEOUT] ?: 0,
                defaultReminderTime = preferences[PreferencesKeys.DEFAULT_REMINDER_TIME] ?: "09:00",
                showMedicineImages = preferences[PreferencesKeys.SHOW_MEDICINE_IMAGES] ?: true,
                trackSideEffects = preferences[PreferencesKeys.TRACK_SIDE_EFFECTS] ?: true,
                trackMood = preferences[PreferencesKeys.TRACK_MOOD] ?: false,
                autoBackupEnabled = preferences[PreferencesKeys.AUTO_BACKUP_ENABLED] ?: false,
                lastSyncTime = preferences[PreferencesKeys.LAST_SYNC_TIME] ?: 0L,
                onboardingCompleted = preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false,
                firstLaunchTime = preferences[PreferencesKeys.FIRST_LAUNCH_TIME] ?: 0L
            )
        }

    // ==================== Notification Settings ====================
    
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }
    
    suspend fun setReminderSoundEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.REMINDER_SOUND_ENABLED] = enabled
        }
    }
    
    suspend fun setReminderVibrationEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.REMINDER_VIBRATION_ENABLED] = enabled
        }
    }
    
    suspend fun setReminderSnoozeDuration(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.REMINDER_SNOOZE_DURATION] = minutes
        }
    }
    
    suspend fun setQuietHours(enabled: Boolean, start: String? = null, end: String? = null) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.QUIET_HOURS_ENABLED] = enabled
            start?.let { preferences[PreferencesKeys.QUIET_HOURS_START] = it }
            end?.let { preferences[PreferencesKeys.QUIET_HOURS_END] = it }
        }
    }

    // ==================== Appearance Settings ====================
    
    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }
    
    suspend fun setUseDynamicColors(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_DYNAMIC_COLORS] = enabled
        }
    }

    // ==================== Language Settings ====================
    
    suspend fun setLanguageCode(code: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LANGUAGE_CODE] = code
        }
    }

    // ==================== Privacy Settings ====================
    
    suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.BIOMETRIC_ENABLED] = enabled
        }
    }
    
    suspend fun setAutoLockTimeout(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_LOCK_TIMEOUT] = minutes
        }
    }

    // ==================== Medicine Tracking Settings ====================
    
    suspend fun setDefaultReminderTime(time: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_REMINDER_TIME] = time
        }
    }
    
    suspend fun setShowMedicineImages(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_MEDICINE_IMAGES] = show
        }
    }
    
    suspend fun setTrackSideEffects(track: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.TRACK_SIDE_EFFECTS] = track
        }
    }
    
    suspend fun setTrackMood(track: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.TRACK_MOOD] = track
        }
    }

    // ==================== Data & Sync ====================
    
    suspend fun setAutoBackupEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_BACKUP_ENABLED] = enabled
        }
    }
    
    suspend fun updateLastSyncTime() {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_SYNC_TIME] = System.currentTimeMillis()
        }
    }

    // ==================== Onboarding ====================
    
    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
            if (completed && preferences[PreferencesKeys.FIRST_LAUNCH_TIME] == null) {
                preferences[PreferencesKeys.FIRST_LAUNCH_TIME] = System.currentTimeMillis()
            }
        }
    }

    // ==================== Clear All Settings ====================
    
    suspend fun clearAllSettings() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
