package com.foco.app.model

/** The user's chosen appearance; `AUTO` follows the system theme. */
enum class AppearanceOption(val wire: String) {
    AUTO("auto"),
    LIGHT("light"),
    DARK("dark");

    /** `AUTO` follows the system; otherwise the choice wins. */
    fun isDark(systemInDarkTheme: Boolean): Boolean = when (this) {
        AUTO -> systemInDarkTheme
        LIGHT -> false
        DARK -> true
    }

    companion object {
        fun fromWire(value: String?): AppearanceOption =
            entries.firstOrNull { it.wire == value } ?: AUTO
    }
}
