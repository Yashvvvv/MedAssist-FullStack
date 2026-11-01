package com.medassist.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.medassist.app.core.auth.AuthTokens
import com.medassist.app.core.auth.TokenStore
import com.medassist.app.core.model.AppJson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The session as one encrypted JSON blob in DataStore. */
class KeystoreTokenStore(
    private val dataStore: DataStore<Preferences>,
    private val cipher: KeystoreCipher,
) : TokenStore {

    private val key = stringPreferencesKey("session")

    override val tokens: Flow<AuthTokens?> = dataStore.data.map { prefs ->
        prefs[key]?.let(cipher::decrypt)?.let { runCatching { AppJson.decodeFromString(AuthTokens.serializer(), it) }.getOrNull() }
    }

    override suspend fun save(tokens: AuthTokens) {
        val blob = cipher.encrypt(AppJson.encodeToString(AuthTokens.serializer(), tokens))
        dataStore.edit { it[key] = blob }
    }

    override suspend fun clear() {
        dataStore.edit { it.remove(key) }
    }
}
