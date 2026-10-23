package com.foco.app.ui.metrics.week

/** An app's usage over the week's window: its total and how many days it was used. */
data class AppWeekTotal(
    val packageName: String,
    val totalMillis: Long,
    val daysUsed: Int,
)
