package com.foco.app.model

/** Sesión de uso de una app en primer plano: [start, end) inferido de eventos de resumen/parada. */
data class UsageSession(
    val packageName: String,
    val start: Long,
    val end: Long
)
