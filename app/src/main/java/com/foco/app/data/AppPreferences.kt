package com.foco.app.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

/**
 * DataStore Preferences compartido de la app: almacenamiento privado, archivo
 * `excluded_apps` (nombre histórico; se conserva para no perder lo ya guardado).
 * Vive lo que la app. Lo consumen [ExcludedAppsStore] (exclusiones) y
 * [SettingsStore] (idioma y apariencia).
 */
internal val Context.appPreferencesDataStore by preferencesDataStore(name = "excluded_apps")
