package com.example.medassist_android.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    companion object {
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val USERNAME_KEY = stringPreferencesKey("username")
        private val EMAIL_KEY = stringPreferencesKey("email")
        private val IS_LOGGED_IN_KEY = stringPreferencesKey("is_logged_in")
    }

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        try {
            dataStore.edit { preferences ->
                preferences[ACCESS_TOKEN_KEY] = accessToken
                preferences[REFRESH_TOKEN_KEY] = refreshToken
                preferences[IS_LOGGED_IN_KEY] = "true"
            }
            Timber.d("Tokens saved successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to save tokens")
        }
    }

    suspend fun saveUserInfo(userId: String, username: String, email: String) {
        try {
            dataStore.edit { preferences ->
                preferences[USER_ID_KEY] = userId
                preferences[USERNAME_KEY] = username
                preferences[EMAIL_KEY] = email
            }
            Timber.d("User info saved successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to save user info")
        }
    }

    suspend fun getAccessToken(): String? {
        return try {
            dataStore.data.first()[ACCESS_TOKEN_KEY]
        } catch (e: Exception) {
            Timber.e(e, "Failed to get access token")
            null
        }
    }

    suspend fun getRefreshToken(): String? {
        return try {
            dataStore.data.first()[REFRESH_TOKEN_KEY]
        } catch (e: Exception) {
            Timber.e(e, "Failed to get refresh token")
            null
        }
    }

    suspend fun getUserId(): String? {
        return try {
            dataStore.data.first()[USER_ID_KEY]
        } catch (e: Exception) {
            Timber.e(e, "Failed to get user ID")
            null
        }
    }

    suspend fun getUsername(): String? {
        return try {
            dataStore.data.first()[USERNAME_KEY]
        } catch (e: Exception) {
            Timber.e(e, "Failed to get username")
            null
        }
    }

    suspend fun getEmail(): String? {
        return try {
            dataStore.data.first()[EMAIL_KEY]
        } catch (e: Exception) {
            Timber.e(e, "Failed to get email")
            null
        }
    }

    suspend fun isLoggedIn(): Boolean {
        return try {
            dataStore.data.first()[IS_LOGGED_IN_KEY] == "true"
        } catch (e: Exception) {
            Timber.e(e, "Failed to check login status")
            false
        }
    }

    val isLoggedInFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[IS_LOGGED_IN_KEY] == "true"
    }

    suspend fun clearTokens() {
        try {
            dataStore.edit { preferences ->
                preferences.remove(ACCESS_TOKEN_KEY)
                preferences.remove(REFRESH_TOKEN_KEY)
                preferences.remove(USER_ID_KEY)
                preferences.remove(USERNAME_KEY)
                preferences.remove(EMAIL_KEY)
                preferences[IS_LOGGED_IN_KEY] = "false"
            }
            Timber.d("Tokens cleared successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear tokens")
        }
    }

    suspend fun clearAllData() {
        try {
            dataStore.edit { preferences ->
                preferences.clear()
            }
            Timber.d("All data cleared successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear all data")
        }
    }
}
