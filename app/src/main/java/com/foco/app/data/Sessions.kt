package com.foco.app.data

/** Sesión de uso de una app en primer plano: [start, end) inferido de eventos de resumen/parada. */
data class UsageSession(
    val packageName: String,
    val start: Long,
    val end: Long
)

/** Evento crudo de primer plano, mapeado desde los eventos del sistema. */
data class UsageEvent(
    val packageName: String,
    val timestamp: Long,
    val type: UsageEventType
)

enum class UsageEventType { RESUMED, STOPPED }

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

/** Política pura: conserva solo las sesiones de paquetes permitidos, menos los excluidos. */
fun filterSessions(
    sessions: List<UsageSession>,
    allowedPackages: Set<String>,
    excludedPackages: Set<String>
): List<UsageSession> = sessions.filter {
    it.packageName in allowedPackages && it.packageName !in excludedPackages
}
