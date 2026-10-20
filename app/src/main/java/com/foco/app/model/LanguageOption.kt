package com.foco.app.model

/** The UI language the user picked; `AUTO` follows the device. */
enum class LanguageOption(
    val wire: String,
) {
    AUTO("auto"),
    ES("es"),
    EN("en"),
    ;

    companion object {
        fun fromWire(value: String?): LanguageOption = entries.firstOrNull { it.wire == value } ?: AUTO
    }
}
