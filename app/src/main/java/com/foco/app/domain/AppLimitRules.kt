package com.foco.app.domain

import com.foco.app.model.UsageSession

/** The daily-limit rule: the selectable grid, and when a package has used its budget. */
object AppLimitRules {
    const val BLOCK_IMMEDIATELY_MINUTES = 0

    /** The grid both rules share: the window owns it, the limit steps on it. */
    const val STEP_MINUTES = AppLimitWindowRules.STEP_MINUTES

    /** The minutes of a local day, the last step of the limit grid. */
    const val MAX_MINUTES = AppLimitWindowRules.MINUTES_PER_DAY

    /** The warning tiers, in increasing order; the monitor notifies only the highest crossed. */
    val ALERT_TIERS = listOf(50, 75, 90)

    /** Zero or a 15-minute step up to 24 h; no limit is the absence of a value, not a value. */
    fun isSelectable(minutes: Int): Boolean =
        minutes == BLOCK_IMMEDIATELY_MINUTES ||
            (minutes in STEP_MINUTES..MAX_MINUTES && minutes % STEP_MINUTES == 0)

    /** No limit never blocks; zero blocks at once; otherwise at or over N minutes used. */
    fun isAtOrOverLimit(
        limitMinutes: Int?,
        usedSeconds: Long,
    ): Boolean {
        if (limitMinutes == null) return false
        if (limitMinutes == BLOCK_IMMEDIATELY_MINUTES) return true
        return usedSeconds >= limitMinutes * 60L
    }

    /**
     * The alert tiers crossed at [usedSeconds] for [limitMinutes], lowest first. No limit, a zero
     * limit, no usage and at-or-over 100 % yield none: the block owns the limit itself.
     */
    fun crossedAlertTiers(
        limitMinutes: Int?,
        usedSeconds: Long,
    ): List<Int> {
        if (limitMinutes == null || limitMinutes <= BLOCK_IMMEDIATELY_MINUTES) return emptyList()
        if (usedSeconds <= 0) return emptyList()
        val limitSeconds = limitMinutes * 60L
        if (usedSeconds >= limitSeconds) return emptyList()
        return ALERT_TIERS.filter { usedSeconds * 100 >= limitSeconds * it }
    }

    /**
     * Whether [packageName] was the foreground at [at]: a same-minute reopen merges into the
     * pre-block session, so its start may precede the block; the end instant counts too.
     */
    fun isForegroundAt(
        sessions: List<UsageSession>,
        packageName: String,
        at: Long,
    ): Boolean = sessions.any { it.packageName == packageName && it.start <= at && it.end >= at }

    /** The foreground seconds [packageName] accumulated in the day's clamped sessions. */
    fun usedSeconds(
        sessions: List<UsageSession>,
        packageName: String,
    ): Long =
        sessions
            .filter { it.packageName == packageName }
            .sumOf { (it.end - it.start).coerceAtLeast(0L) } / 1_000L
}
