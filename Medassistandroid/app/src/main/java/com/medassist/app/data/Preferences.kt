package com.medassist.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Small non-secret settings: the branch a staff member works at, the theme. */
class AppPreferences(private val dataStore: DataStore<Preferences>) {

    private val branchKey = stringPreferencesKey("branch_id")
    private val themeKey = stringPreferencesKey("theme")

    val branchId: Flow<String?> = dataStore.data.map { it[branchKey] }
    val theme: Flow<ThemeMode> = dataStore.data.map { p ->
        p[themeKey]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
    }

    suspend fun setBranch(id: String?) = dataStore.edit { if (id == null) it.remove(branchKey) else it[branchKey] = id }

    suspend fun setTheme(mode: ThemeMode) = dataStore.edit { it[themeKey] = mode.name }
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }
