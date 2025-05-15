package com.example.nomnom

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "user_prefs")

object RememberMeManager {
    private val REMEMBER_ME_KEY = booleanPreferencesKey("remember_me")

    fun getRememberMe(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[REMEMBER_ME_KEY] ?: false
        }
    }

    suspend fun setRememberMe(context: Context, value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[REMEMBER_ME_KEY] = value
        }
    }
}
