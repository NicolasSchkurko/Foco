package com.foco.app.data

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.foco.app.model.LanguageOption

/**
 * OS: the system locale list (AppCompatDelegate); where: the app's per-app language setting;
 * lifecycle: written when the user picks a language — an empty list means Automatic.
 */
object AppLanguage {
    fun applyToSystem(option: LanguageOption) {
        AppCompatDelegate.setApplicationLocales(
            when (option) {
                LanguageOption.AUTO -> LocaleListCompat.getEmptyLocaleList()
                LanguageOption.ES -> LocaleListCompat.forLanguageTags("es")
                LanguageOption.EN -> LocaleListCompat.forLanguageTags("en")
            },
        )
    }
}
