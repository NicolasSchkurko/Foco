package com.foco.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first

/**
 * Migración one-shot de las preferencias: `language`/`appearance` pasan del store
 * legado (`excluded_apps`, donde vivían junto a las exclusiones) al propio
 * (`foco_settings`). Idempotente: sin claves en el legado, no hace nada.
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
