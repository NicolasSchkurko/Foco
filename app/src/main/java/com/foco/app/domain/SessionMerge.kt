package com.foco.app.domain

import com.foco.app.model.UsageSession
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Joins sessions of the same app that meet at the same local HH:mm on the same day, transitively.
 * Different apps and different local days never meet.
 */
fun mergeContiguousSessions(
    sessions: List<UsageSession>,
    zone: ZoneId,
): List<UsageSession> =
    sessions
        .groupBy { it.packageName }
        .values
        .flatMap { sameApp ->
            val merged = mutableListOf<UsageSession>()
            sameApp.sortedBy { it.start }.forEach { session ->
                val previous = merged.lastOrNull()
                if (previous != null && meets(previous, session, zone)) {
                    merged[merged.lastIndex] = previous.copy(end = maxOf(previous.end, session.end))
                } else {
                    merged += session
                }
            }
            merged
        }.sortedBy { it.start }

private fun meets(
    previous: UsageSession,
    next: UsageSession,
    zone: ZoneId,
): Boolean {
    if (previous.packageName != next.packageName) return false
    val previousEnd = Instant.ofEpochMilli(previous.end).atZone(zone)
    val nextStart = Instant.ofEpochMilli(next.start).atZone(zone)
    return previousEnd.toLocalDate() == nextStart.toLocalDate() &&
        previousEnd.toLocalTime().truncatedTo(ChronoUnit.MINUTES) ==
        nextStart.toLocalTime().truncatedTo(ChronoUnit.MINUTES)
}
