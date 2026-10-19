package com.foco.app.model

/** Usage session of an app in the foreground: [start, end) inferred from resume/stop events. */
data class UsageSession(
    val packageName: String,
    val start: Long,
    val end: Long
)
