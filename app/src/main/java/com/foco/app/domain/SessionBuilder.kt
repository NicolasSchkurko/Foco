package com.foco.app.domain

import com.foco.app.model.UsageEvent
import com.foco.app.model.UsageEventType
import com.foco.app.model.UsageSession

/**
 * Pure fold over [events], which must be chronological: a switch closes the previous one at that
 * instant, a stop closes its own package's, ties included; zero-length sessions are not emitted.
 */
fun buildSessions(
    events: List<UsageEvent>,
    now: Long,
): List<UsageSession> {
    val sessions = mutableListOf<UsageSession>()

    /** Emits the session unless it is zero-length: two events in the same ms do not count. */
    fun emit(
        packageName: String,
        start: Long,
        end: Long,
    ) {
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
                    // Direct switch: closes the previous one at the resume's instant, ties included.
                    open != event.packageName && event.timestamp >= openStart -> {
                        emit(open, openStart, event.timestamp)
                        openPackage = event.packageName
                        openStart = event.timestamp
                    }
                    // Same app (internal activity transition): the session is not cut.
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

    // App in the foreground right now: the session stays open until "now".
    openPackage?.let { open -> emit(open, openStart, now) }

    return sessions
}

/**
 * Clamps sessions to a day's window: one open before the day counts from there and the
 * neighbouring day keeps its own slice; what falls outside is dropped.
 */
fun clampSessionsToDay(
    sessions: List<UsageSession>,
    window: DayWindow,
): List<UsageSession> =
    sessions.mapNotNull { session ->
        val start = maxOf(session.start, window.startOfDay)
        val end = minOf(session.end, window.dayEndExclusive)
        if (end > start) UsageSession(session.packageName, start, end) else null
    }
