package com.foco.app.data

import androidx.appcompat.app.AppCompatDelegate
import com.foco.app.model.AppearanceOption

/**
 * Lleva la apariencia elegida al modo nocturno global de appcompat: el tema de la
 * activity, sus recursos y el fondo estático de la ventana pasan a seguir la elección
 * (Automático = seguir al sistema), no solo el `darkTheme` de Compose. Recurso físico:
 * el modo vive en memoria del proceso mientras la app vive; la elección se lee del
 * DataStore `foco_settings` al arrancar.
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
