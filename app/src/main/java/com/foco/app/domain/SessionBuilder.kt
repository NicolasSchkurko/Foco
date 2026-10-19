package com.foco.app.domain

import com.foco.app.model.UsageEvent
import com.foco.app.model.UsageEventType
import com.foco.app.model.UsageSession

/**
 * Fold puro: construye las sesiones del día a partir de eventos resumen/parada.
 *
 * - RESUMED abre una sesión; un cambio directo a otra app cierra la anterior en el instante del nuevo
 *   resume, aunque caigan en el mismo milisegundo.
 * - STOPPED (pause o stop) cierra la sesión abierta de su propio paquete, también en el mismo milisegundo.
 * - Una sesión aún abierta en [now] se cierra en [now]; las transiciones internas de la misma app no
 *   cortan la sesión, y una sesión de longitud cero no se emite.
 */
fun buildSessions(events: List<UsageEvent>, now: Long): List<UsageSession> {
    val sessions = mutableListOf<UsageSession>()

    /** Emite la sesión salvo que sea de longitud cero: dos eventos en el mismo ms no cuentan. */
    fun emit(packageName: String, start: Long, end: Long) {
        if (end > start) sessions += UsageSession(packageName, start, end)
    }

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
                    // Cambio directo: cierra la anterior en el instante del resume, aunque empaten.
                    open != event.packageName && event.timestamp >= openStart -> {
                        emit(open, openStart, event.timestamp)
                        openPackage = event.packageName
                        openStart = event.timestamp
                    }
                    // Misma app (transición interna de activities): no cortamos la sesión.
                }
            }

            UsageEventType.STOPPED -> {
                val open = openPackage
                if (open != null && event.packageName == open && event.timestamp >= openStart) {
                    emit(open, openStart, event.timestamp)
                    openPackage = null
                }
            }
        }
    }

    // App en foreground en este momento: sesión abierta hasta "now".
    openPackage?.let { open -> emit(open, openStart, now) }

    return sessions
}

/**
 * Recorta las sesiones a la ventana de un día. Una sesión abierta antes de [dayStart]
 * cuenta desde [dayStart], así el día vecino conserva su propia parte; lo que queda
 * fuera de la ventana se descarta.
 */
fun clampSessionsToDay(
    sessions: List<UsageSession>,
    dayStart: Long,
    dayEndExclusive: Long
): List<UsageSession> = sessions.mapNotNull { session ->
    val start = maxOf(session.start, dayStart)
    val end = minOf(session.end, dayEndExclusive)
    if (end > start) UsageSession(session.packageName, start, end) else null
}
