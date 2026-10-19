package com.foco.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.foco.app.model.AppearanceOption
import com.foco.app.model.LanguageOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Language and appearance preferences. Resource: the `foco_settings` file
 * (Preferences DataStore) in the app's private storage; lives as long as the app.
 */
class SettingsStore(private val context: Context) {

    val language: Flow<LanguageOption> =
        context.settingsDataStore.data.map { LanguageOption.fromWire(it[languageKey]) }

    val appearance: Flow<AppearanceOption> =
        context.settingsDataStore.data.map { AppearanceOption.fromWire(it[appearanceKey]) }

    suspend fun setLanguage(option: LanguageOption) {
        context.settingsDataStore.edit { it[languageKey] = option.wire }
    }

    suspend fun setAppearance(option: AppearanceOption) {
        context.settingsDataStore.edit { it[appearanceKey] = option.wire }
    }
}

internal val Context.settingsDataStore by preferencesDataStore(name = "foco_settings")

/** Preference keys; shared with the migration from the legacy store. */
internal val languageKey = stringPreferencesKey("language")
internal val appearanceKey = stringPreferencesKey("appearance")
