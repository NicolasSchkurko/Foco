package com.foco.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first

/**
 * One-shot migration: `language`/`appearance` move from the legacy store (`excluded_apps`) to their own
 * (`foco_settings`); with no keys in the legacy store it does nothing (idempotent).
 */
suspend fun migrateSettingsFromLegacyDataStore(context: Context) {
    copySettingsFromLegacyStore(context.excludedAppsDataStore, context.settingsDataStore)
}

internal suspend fun copySettingsFromLegacyStore(
    legacy: DataStore<Preferences>,
    target: DataStore<Preferences>
) {
    val stored = legacy.data.first()
    val language = stored[languageKey]
    val appearance = stored[appearanceKey]
    if (language == null && appearance == null) return

    target.edit { preferences ->
        if (language != null) preferences[languageKey] = language
        if (appearance != null) preferences[appearanceKey] = appearance
    }
    legacy.edit { preferences ->
        preferences.remove(languageKey)
        preferences.remove(appearanceKey)
    }
}
