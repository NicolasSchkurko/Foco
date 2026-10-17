package com.foco.app.data

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.foco.app.model.LanguageOption

/**
 * Aplica el idioma elegido al sistema con el backport de appcompat; en Android 13+
 * la elección también aparece en la lista de idiomas por app de los Ajustes.
 * Automático = lista vacía = idioma del dispositivo.
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
