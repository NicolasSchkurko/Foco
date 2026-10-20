package com.foco.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Packages excluded from the ranking (data only, no policy). Resource: the `excluded_apps` file
 * (Preferences DataStore) in private storage; lives as long as the app.
 */
class ExcludedAppsStore(
    private val context: Context,
) {
    private val key = stringSetPreferencesKey("excluded_packages")

    val excludedPackages: Flow<Set<String>> =
        context.excludedAppsDataStore.data.map { preferences ->
            preferences[key] ?: emptySet()
        }

    /** Excludes `packageName` from the ranking. */
    suspend fun exclude(packageName: String) {
        context.excludedAppsDataStore.edit { preferences ->
            preferences[key] = (preferences[key] ?: emptySet()) + packageName
        }
    }

    /** Returns `packageName` to the ranking. */
    suspend fun restore(packageName: String) {
        context.excludedAppsDataStore.edit { preferences ->
            preferences[key] = (preferences[key] ?: emptySet()) - packageName
        }
    }
}

internal val Context.excludedAppsDataStore by preferencesDataStore(name = "excluded_apps")
