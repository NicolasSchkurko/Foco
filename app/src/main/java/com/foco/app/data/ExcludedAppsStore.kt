package com.foco.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Lista persistente de paquetes excluidos del ranking (solo datos: sin política). */
class ExcludedAppsStore(private val context: Context) {

    private val key = stringSetPreferencesKey("excluded_packages")

    val excludedPackages: Flow<Set<String>> =
        context.appPreferencesDataStore.data.map { preferences ->
            preferences[key] ?: emptySet()
        }

    suspend fun add(packageName: String) {
        context.appPreferencesDataStore.edit { preferences ->
            preferences[key] = (preferences[key] ?: emptySet()) + packageName
        }
    }

    suspend fun remove(packageName: String) {
        context.appPreferencesDataStore.edit { preferences ->
            preferences[key] = (preferences[key] ?: emptySet()) - packageName
        }
    }
}
