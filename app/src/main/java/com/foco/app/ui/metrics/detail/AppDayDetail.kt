package com.foco.app.ui.metrics.detail

import com.foco.app.model.UsageSession
import com.foco.app.ui.metrics.day.HourBucket

/** One day's summary for ONE app: totals, extremes, averages, hours and its sessions (chronological). */
data class AppDayDetail(
    val packageName: String,
    val totalMillis: Long,
    val sessionCount: Int,
    val firstStart: Long,
    val lastEnd: Long,
    val averageMillis: Long,
    val longestMillis: Long,
    val hourly: List<HourBucket>,
    val sessions: List<UsageSession>,
)
