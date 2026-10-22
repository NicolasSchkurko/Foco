package com.foco.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * One-shot migration: `language`/`appearance` move from the legacy store (`excluded_apps`) to their own
 * (`foco_settings`); the marker makes it run once, off the main thread.
 */
suspend fun migrateSettingsFromLegacyDataStore(context: Context) {
    withContext(Dispatchers.IO) {
        copySettingsFromLegacyStore(context.excludedAppsDataStore, context.settingsDataStore)
    }
}

internal suspend fun copySettingsFromLegacyStore(
    legacy: DataStore<Preferences>,
    target: DataStore<Preferences>,
) {
    if (target.data.first()[migrationDoneKey] == true) return
    val stored = legacy.data.first()
    val language = stored[languageKey]
    val appearance = stored[appearanceKey]
    // The marker lands with the copy: after it, the legacy file is never read again.
    target.edit { preferences ->
        if (language != null && preferences[languageKey] == null) preferences[languageKey] = language
        if (appearance != null && preferences[appearanceKey] == null) preferences[appearanceKey] = appearance
        preferences[migrationDoneKey] = true
    }
    if (language == null && appearance == null) return
    legacy.edit { preferences ->
        preferences.remove(languageKey)
        preferences.remove(appearanceKey)
    }
}

/** The marker that says the migration already ran; the target store keeps it. */
internal val migrationDoneKey = booleanPreferencesKey("legacy_migration_done")
