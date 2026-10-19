package com.foco.app.data

import androidx.appcompat.app.AppCompatDelegate
import com.foco.app.model.AppearanceOption

/**
 * Lleva la elección al tema de la activity y al fondo de la ventana (no solo al `darkTheme`), y la
 * expone como modo nocturno de appcompat: recurso en memoria del proceso, leído de `foco_settings`.
 */
object AppAppearance {

    fun applyToSystem(option: AppearanceOption) {
        AppCompatDelegate.setDefaultNightMode(nightModeFor(option))
    }

    /** Modo nocturno de appcompat que corresponde a la apariencia elegida. */
    fun nightModeFor(option: AppearanceOption): Int = when (option) {
        AppearanceOption.AUTO -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        AppearanceOption.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
        AppearanceOption.DARK -> AppCompatDelegate.MODE_NIGHT_YES
    }
}
