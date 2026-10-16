package com.foco.app.data

/** Uso agregado de una app en el día. */
data class AppUsageEntry(
    val packageName: String,
    val label: String,
    val totalMillis: Long,
    val lastUsedAt: Long,
    val sessions: Int
)

/** Uso del dispositivo dentro de una franja horaria (0..23). */
data class HourBucket(
    val hour: Int,
    val millis: Long
)

/** Resumen de un día completo. */
data class DayUsage(
    val totalMillis: Long,
    val entries: List<AppUsageEntry>,
    val hourly: List<HourBucket>
)
