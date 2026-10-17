package com.foco.app.model

/** Idioma de la UI elegido por el usuario; `AUTO` sigue al dispositivo. */
enum class LanguageOption(val wire: String) {
    AUTO("auto"),
    ES("es"),
    EN("en");

    companion object {
        fun fromWire(value: String?): LanguageOption =
            entries.firstOrNull { it.wire == value } ?: AUTO
    }
}
