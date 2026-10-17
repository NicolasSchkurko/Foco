package com.foco.app.model

/** Apariencia elegida por el usuario; `AUTO` sigue al tema del sistema. */
enum class AppearanceOption(val wire: String) {
    AUTO("auto"),
    LIGHT("light"),
    DARK("dark");

    /** Si `AUTO`, sigue al sistema; si no, la elección manda. */
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
