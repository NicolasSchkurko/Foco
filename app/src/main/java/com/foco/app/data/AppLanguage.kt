package com.foco.app.data

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.foco.app.model.LanguageOption

/**
 * Aplica el idioma elegido al sistema (backport de appcompat); Automático es lista vacía.
 */
object AppLanguage {

    fun applyToSystem(option: LanguageOption) {
        AppCompatDelegate.setApplicationLocales(
            when (option) {
                LanguageOption.AUTO -> LocaleListCompat.getEmptyLocaleList()
                LanguageOption.ES -> LocaleListCompat.forLanguageTags("es")
                LanguageOption.EN -> LocaleListCompat.forLanguageTags("en")
            }
        )
    }
}
