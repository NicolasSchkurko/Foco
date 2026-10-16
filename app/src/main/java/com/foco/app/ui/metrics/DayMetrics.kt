package com.foco.app.ui.metrics

/** Todo lo que muestra el dashboard para un día, derivado de sus sesiones. */
data class DayMetrics(
    val totalMillis: Long,
    val entries: List<AppUsageEntry>,
    val hourly: List<HourBucket>
)
