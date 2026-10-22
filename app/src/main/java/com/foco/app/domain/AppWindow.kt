package com.foco.app.domain

/**
 * A recurring daily use window: minutes from local midnight, end exclusive. Both ends sit on the
 * 15-minute grid and differ: equal ends would be a window that is always open and never blocks.
 */
data class AppWindow(
    val startMinute: Int,
    val endMinute: Int,
) {
    init {
        require(isWindow(startMinute, endMinute)) { "not a window: $startMinute-$endMinute" }
    }

    companion object {
        /** The window for two minutes of the day, or null when the pair is not a window. */
        fun of(
            startMinute: Int,
            endMinute: Int,
        ): AppWindow? =
            if (isWindow(startMinute, endMinute)) {
                AppWindow(startMinute, endMinute)
            } else {
                null
            }

        /** Whether the pair is a window: both ends on the grid and different from each other. */
        fun isWindow(
            startMinute: Int,
            endMinute: Int,
        ): Boolean =
            AppLimitWindowRules.isSelectable(startMinute) &&
                AppLimitWindowRules.isSelectable(endMinute) &&
                startMinute != endMinute
    }
}
