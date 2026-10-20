package com.foco.app.domain

import com.foco.app.model.UsageSession

/** The daily-limit rule: the selectable grid, and when a package has used its budget. */
object AppLimitRules {

    const val BLOCK_IMMEDIATELY_MINUTES = 0
    const val STEP_MINUTES = 15
    const val MAX_MINUTES = 24 * 60

    /** The picker's values: block at open, then 15-minute steps up to 24 h. */
    fun options(): List<Int> =
        listOf(BLOCK_IMMEDIATELY_MINUTES) + (STEP_MINUTES..MAX_MINUTES step STEP_MINUTES)

    /** Zero or a 15-minute step up to 24 h; no limit is the absence of a value, not a value. */
    fun isSelectable(minutes: Int): Boolean =
        minutes == BLOCK_IMMEDIATELY_MINUTES ||
            (minutes in STEP_MINUTES..MAX_MINUTES && minutes % STEP_MINUTES == 0)

    /** No limit never blocks; zero blocks at once; otherwise at or over N minutes used. */
    fun isAtOrOverLimit(limitMinutes: Int?, usedSeconds: Long): Boolean {
        if (limitMinutes == null) return false
        if (limitMinutes == BLOCK_IMMEDIATELY_MINUTES) return true
        return usedSeconds >= limitMinutes * 60L
    }

    /** The foreground seconds [packageName] accumulated in the day's clamped sessions. */
    fun usedSeconds(sessions: List<UsageSession>, packageName: String): Long =
        sessions.filter { it.packageName == packageName }
            .sumOf { ((it.end - it.start).coerceAtLeast(0L)) / 1_000L }
}
