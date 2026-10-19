package com.foco.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Paquetes excluidos del ranking (solo datos, sin política). Recurso: archivo `excluded_apps`
 * (Preferences DataStore) privado; vive lo que la app.
 */
class ExcludedAppsStore(private val context: Context) {

    private val key = stringSetPreferencesKey("excluded_packages")

    val excludedPackages: Flow<Set<String>> =
        context.excludedAppsDataStore.data.map { preferences ->
            preferences[key] ?: emptySet()
        }

    /** Excluye `packageName` del ranking. */
    suspend fun exclude(packageName: String) {
        context.excludedAppsDataStore.edit { preferences ->
            preferences[key] = (preferences[key] ?: emptySet()) + packageName
        }
    }

    /** Devuelve `packageName` al ranking. */
    suspend fun restore(packageName: String) {
        context.excludedAppsDataStore.edit { preferences ->
            preferences[key] = (preferences[key] ?: emptySet()) - packageName
        }
    }
}

internal val Context.excludedAppsDataStore by preferencesDataStore(name = "excluded_apps")
