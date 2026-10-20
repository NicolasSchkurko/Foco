package com.foco.app.domain

import com.foco.app.model.UsageSession
import java.time.Instant
import java.time.ZoneId

/**
 * The recurring-window rule: the 15-minute grid, inside/outside, overnight ranges, the next
 * boundary and the in-window portion of a session. Pure: the caller supplies the zone and clock.
 */
object AppLimitWindowRules {
    const val STEP_MINUTES = 15
    const val MINUTES_PER_DAY = 24 * 60

    /** Every selectable minute from local midnight to 23:45. */
    fun options(): List<Int> = (0 until MINUTES_PER_DAY step STEP_MINUTES).toList()

    /** A minute on the 15-minute grid inside one local day. */
    fun isSelectable(minute: Int): Boolean = minute in 0 until MINUTES_PER_DAY && minute % STEP_MINUTES == 0

    /** A window is valid when both ends are selectable and differ; equal ends are not all day. */
    fun isValid(
        startMinute: Int,
        endMinute: Int,
    ): Boolean = isSelectable(startMinute) && isSelectable(endMinute) && startMinute != endMinute

    /** Whether [nowMillis] falls inside [window]; a start after the end crosses midnight. */
    fun isInside(
        nowMillis: Long,
        window: AppWindow,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Boolean {
        val secondOfDay =
            Instant
                .ofEpochMilli(nowMillis)
                .atZone(zone)
                .toLocalTime()
                .toSecondOfDay()
        val start = window.startMinute * 60
        val end = window.endMinute * 60
        return if (window.startMinute < window.endMinute) {
            secondOfDay >= start && secondOfDay < end
        } else {
            secondOfDay >= start || secondOfDay < end
        }
    }

    /** The next local instant at which [window] opens or closes after [nowMillis]. */
    fun nextBoundaryMillis(
        nowMillis: Long,
        window: AppWindow,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long {
        val zoned = Instant.ofEpochMilli(nowMillis).atZone(zone)
        val inside = isInside(nowMillis, window, zone)
        val boundaryMinute = if (inside) window.endMinute else window.startMinute
        val today = zoned.toLocalDate().atStartOfDay(zone)
        var boundary = today.plusMinutes(boundaryMinute.toLong()).toInstant().toEpochMilli()
        if (boundary <= nowMillis) {
            boundary =
                today
                    .plusDays(1)
                    .plusMinutes(boundaryMinute.toLong())
                    .toInstant()
                    .toEpochMilli()
        }
        return boundary
    }

    /** The milliseconds of [session] that fall inside [window] on the session's local day. */
    fun inWindowMillis(
        session: UsageSession,
        window: AppWindow,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long {
        val day = Instant.ofEpochMilli(session.start).atZone(zone).toLocalDate()
        val dayStart = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val dayEnd =
            day
                .plusDays(1)
                .atStartOfDay(zone)
                .toInstant()
                .toEpochMilli()
        val start = dayStart + window.startMinute * 60_000L
        val end = dayStart + window.endMinute * 60_000L
        val intervals =
            if (window.startMinute < window.endMinute) {
                listOf(start to end)
            } else {
                listOf(start to dayEnd, dayStart to end)
            }
        return intervals.sumOf { (intervalStart, intervalEnd) ->
            val overlapStart = maxOf(session.start, intervalStart)
            val overlapEnd = minOf(session.end, intervalEnd)
            (overlapEnd - overlapStart).coerceAtLeast(0L)
        }
    }

    /** The in-window seconds of [packageName] in [sessions]. */
    fun usedSeconds(
        sessions: List<UsageSession>,
        packageName: String,
        window: AppWindow,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long =
        sessions
            .filter { it.packageName == packageName }
            .sumOf { inWindowMillis(it, window, zone) / 1_000L }
}
