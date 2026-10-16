package com.foco.app.domain

import com.foco.app.model.UsageEvent
import com.foco.app.model.UsageEventType
import com.foco.app.model.UsageSession

/**
 * Fold puro: construye las sesiones del día a partir de eventos resumen/parada.
 *
 * - RESUMED abre una sesión; un cambio directo a otra app cierra la anterior al inicio de la nueva.
 * - STOPPED (pause o stop) cierra la sesión abierta.
 * - Una sesión aún abierta en [now] se cierra en [now].
 * - Transiciones internas de la misma app (cambio de activity) no cortan la sesión.
 */
fun buildSessions(events: List<UsageEvent>, now: Long): List<UsageSession> {
    val sessions = mutableListOf<UsageSession>()
    var openPackage: String? = null
    var openStart = 0L

    for (event in events) {
        when (event.type) {
            UsageEventType.RESUMED -> {
                val open = openPackage
                when {
                    open == null -> {
                        openPackage = event.packageName
                        openStart = event.timestamp
                    }
                    open != event.packageName && event.timestamp > openStart -> {
                        sessions += UsageSession(open, openStart, event.timestamp)
                        openPackage = event.packageName
                        openStart = event.timestamp
                    }
                    // Misma app (transición interna de activities): no cortamos la sesión.
                }
            }

            UsageEventType.STOPPED -> {
                val open = openPackage
                if (open != null && event.packageName == open && event.timestamp > openStart) {
                    sessions += UsageSession(open, openStart, event.timestamp)
                    openPackage = null
                }
            }
        }
    }

    // App en foreground en este momento: sesión abierta hasta "now".
    openPackage?.let { open ->
        if (now > openStart) sessions += UsageSession(open, openStart, now)
    }

    return sessions
}
