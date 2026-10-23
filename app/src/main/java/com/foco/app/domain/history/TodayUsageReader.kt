package com.foco.app.domain.history

/**
 * Reads how long a package used the screen today. The platform adapter lives in the data
 * layer; the port exists so the limit decision can be exercised without the platform.
 */
interface TodayUsageReader {
    /** The milliseconds [packageName] was in the foreground today, up to [now]. */
    fun todayUsageMillis(
        packageName: String,
        now: Long,
    ): Long
}
