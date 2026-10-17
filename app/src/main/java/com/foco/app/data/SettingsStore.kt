package com.foco.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.foco.app.model.AppearanceOption
import com.foco.app.model.LanguageOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Preferencias de idioma y apariencia, persistidas en el DataStore de la app. */
class SettingsStore(private val context: Context) {

    val language: Flow<LanguageOption> =
        context.appPreferencesDataStore.data.map { LanguageOption.fromWire(it[languageKey]) }

    val appearance: Flow<AppearanceOption> =
        context.appPreferencesDataStore.data.map { AppearanceOption.fromWire(it[appearanceKey]) }

    suspend fun setLanguage(option: LanguageOption) {
        context.appPreferencesDataStore.edit { it[languageKey] = option.wire }
    }

    suspend fun setAppearance(option: AppearanceOption) {
        context.appPreferencesDataStore.edit { it[appearanceKey] = option.wire }
    }

    private companion object {
        val languageKey = stringPreferencesKey("language")
        val appearanceKey = stringPreferencesKey("appearance")
    }
}
