package com.foco.app.domain

import com.foco.app.model.UsageSession
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
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
    ): Boolean = AppWindow.isWindow(startMinute, endMinute)

    /** Whether [nowMillis] falls inside [window]; a start after the end crosses midnight. */
    fun isInside(
        nowMillis: Long,
        window: AppWindow,
        zone: ZoneId,
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

    /** Whether [nowMillis] is outside [window]; no window never blocks. */
    fun blocksAt(
        nowMillis: Long,
        window: AppWindow?,
        zone: ZoneId,
    ): Boolean = window != null && !isInside(nowMillis, window, zone)

    /** The next local instant at which [window] opens or closes after [nowMillis]. */
    fun nextBoundaryMillis(
        nowMillis: Long,
        window: AppWindow,
        zone: ZoneId,
    ): Long {
        val zoned = Instant.ofEpochMilli(nowMillis).atZone(zone)
        val inside = isInside(nowMillis, window, zone)
        val boundaryMinute = if (inside) window.endMinute else window.startMinute
        val today = zoned.toLocalDate()
        var boundary = boundaryMillis(today, boundaryMinute, zone)
        if (boundary <= nowMillis) {
            boundary = boundaryMillis(today.plusDays(1), boundaryMinute, zone)
        }
        return boundary
    }

    /** The milliseconds of [session] that fall inside [window], over every day it touches. */
    fun inWindowMillis(
        session: UsageSession,
        window: AppWindow,
        zone: ZoneId,
    ): Long = inWindowMillisByDay(session, window, zone).values.sum()

    /**
     * The in-window milliseconds of [session] per local day: a session crossing midnight splits
     * its in-window minutes between the days they belong to.
     */
    fun inWindowMillisByDay(
        session: UsageSession,
        window: AppWindow,
        zone: ZoneId,
    ): Map<LocalDate, Long> {
        if (session.end <= session.start) return emptyMap()
        val firstDay = Instant.ofEpochMilli(session.start).atZone(zone).toLocalDate()
        val lastDay = Instant.ofEpochMilli(session.end - 1).atZone(zone).toLocalDate()
        val millis = mutableMapOf<LocalDate, Long>()
        var day = firstDay
        while (!day.isAfter(lastDay)) {
            val inWindow = inWindowMillisOnDay(session, day, window, zone)
            if (inWindow > 0) millis[day] = inWindow
            day = day.plusDays(1)
        }
        return millis
    }

    /** The in-window seconds of [packageName] in [sessions]; the total is truncated once. */
    fun usedSeconds(
        sessions: List<UsageSession>,
        packageName: String,
        window: AppWindow,
        zone: ZoneId,
    ): Long =
        sessions
            .filter { it.packageName == packageName }
            .sumOf { inWindowMillis(it, window, zone) } / 1_000L

    /** The milliseconds of [session] that fall inside [window] on [day] alone. */
    private fun inWindowMillisOnDay(
        session: UsageSession,
        day: LocalDate,
        window: AppWindow,
        zone: ZoneId,
    ): Long {
        val dayStart = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val dayEnd =
            day
                .plusDays(1)
                .atStartOfDay(zone)
                .toInstant()
                .toEpochMilli()
        val from = maxOf(session.start, dayStart)
        val to = minOf(session.end, dayEnd)
        val start = boundaryMillis(day, window.startMinute, zone)
        val end = boundaryMillis(day, window.endMinute, zone)
        val intervals =
            if (window.startMinute < window.endMinute) {
                listOf(start to end)
            } else {
                listOf(start to dayEnd, dayStart to end)
            }
        return intervals.sumOf { (intervalStart, intervalEnd) ->
            (minOf(to, intervalEnd) - maxOf(from, intervalStart)).coerceAtLeast(0L)
        }
    }

    /** The instant a stored minute of the day names on [date] in [zone] — the wall clock,
     *  not epoch arithmetic: on a day the offset changes the same minute is a different
     *  instant, and a minute the zone skips resolves to the first instant after the gap. */
    private fun boundaryMillis(
        date: LocalDate,
        minuteOfDay: Int,
        zone: ZoneId,
    ): Long =
        LocalTime
            .of(minuteOfDay / 60, minuteOfDay % 60)
            .atDate(date)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
}
