package com.foco.app.ui.metrics

import com.foco.app.model.UsageSession

/** Resumen de un día para UNA app: totales, extremos, promedios, horas y sus sesiones (cronológicas). */
data class AppDayDetail(
    val packageName: String,
    val totalMillis: Long,
    val sessionCount: Int,
    val firstStart: Long,
    val lastEnd: Long,
    val averageMillis: Long,
    val longestMillis: Long,
    val hourly: List<HourBucket>,
    val sessions: List<UsageSession>
)
