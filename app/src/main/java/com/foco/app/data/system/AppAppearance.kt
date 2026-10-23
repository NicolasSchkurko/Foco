package com.foco.app.data.system

import androidx.appcompat.app.AppCompatDelegate
import com.foco.app.model.AppearanceOption

/**
 * Carries the choice to the activity theme and the window backdrop (not only Compose's `darkTheme`), and it
 * exposes it as appcompat's night mode: process memory, read from `foco_settings`.
 */
object AppAppearance {
    fun applyToSystem(option: AppearanceOption) {
        AppCompatDelegate.setDefaultNightMode(nightModeFor(option))
    }

    /** The appcompat night mode the chosen appearance maps to. */
    fun nightModeFor(option: AppearanceOption): Int =
        when (option) {
            AppearanceOption.AUTO -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            AppearanceOption.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            AppearanceOption.DARK -> AppCompatDelegate.MODE_NIGHT_YES
        }
}
