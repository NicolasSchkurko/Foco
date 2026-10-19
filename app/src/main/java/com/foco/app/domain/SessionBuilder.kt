package com.foco.app.domain

import com.foco.app.model.UsageEvent
import com.foco.app.model.UsageEventType
import com.foco.app.model.UsageSession

/**
 * Fold puro: un cambio directo cierra la anterior en su instante y una parada cierra la del propio
 * paquete, aunque caigan en el mismo milisegundo; las sesiones de largo cero no se emiten.
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
 * Recorta las sesiones a la ventana de un día: una abierta antes de [dayStart] cuenta desde ahí y el día
 * vecino conserva su parte; lo que queda fuera se descarta.
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
