package com.foco.app.model

/** Evento crudo de primer plano, mapeado desde los eventos del sistema. */
data class UsageEvent(
    val packageName: String,
    val timestamp: Long,
    val type: UsageEventType
)
