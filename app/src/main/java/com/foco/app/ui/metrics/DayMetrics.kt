package com.foco.app.ui.metrics

/** Everything the dashboard shows for a day, derived from its sessions. */
data class DayMetrics(
    val totalMillis: Long,
    val entries: List<AppUsageEntry>,
    val hourly: List<HourBucket>,
)
