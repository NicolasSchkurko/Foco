package com.foco.app.domain

/** A recurring daily use window: minutes from local midnight, end exclusive. */
data class AppWindow(
    val startMinute: Int,
    val endMinute: Int,
) {
    companion object {
        /** The window for two minutes of the day, or null when the pair is not a window. */
        fun of(
            startMinute: Int,
            endMinute: Int,
        ): AppWindow? =
            if (AppLimitWindowRules.isValid(startMinute, endMinute)) {
                AppWindow(startMinute, endMinute)
            } else {
                null
            }
    }
}
