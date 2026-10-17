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
 * Preferencias de idioma y apariencia. Recurso físico: archivo `foco_settings`
 * (Preferences DataStore) en el almacenamiento privado de la app; vive lo que la app.
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

/** Claves de las preferencias; compartidas con la migración desde el store legado. */
internal val languageKey = stringPreferencesKey("language")
internal val appearanceKey = stringPreferencesKey("appearance")
