package com.foco.app.data

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.foco.app.model.LanguageOption

/**
 * Applies the chosen language to the system (appcompat backport); Automatic is an empty list.
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
