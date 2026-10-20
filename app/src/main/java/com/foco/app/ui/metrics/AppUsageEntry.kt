package com.foco.app.ui.metrics

/** An app's aggregated usage, derived from its sessions (the ranking's output). */
data class AppUsageEntry(
    val packageName: String,
    val totalMillis: Long,
    val lastUsedAt: Long,
    val sessions: Int,
)
