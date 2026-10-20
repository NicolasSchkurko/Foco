package com.foco.app.model

/** Raw foreground event, mapped from the system's events. */
data class UsageEvent(
    val packageName: String,
    val timestamp: Long,
    val type: UsageEventType,
)
