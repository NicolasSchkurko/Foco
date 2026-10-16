package com.foco.app.ui.metrics

/** Uso agregado de una app, derivado de sus sesiones (salida del ranking). */
data class AppUsageEntry(
    val packageName: String,
    val totalMillis: Long,
    val lastUsedAt: Long,
    val sessions: Int
)
