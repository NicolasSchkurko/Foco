package com.foco.app.ui.metrics.day

import com.foco.app.model.UsageSession
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** A session shorter than this is not visible in app views; the day's totals still count it. */
const val MIN_VISIBLE_SESSION_MILLIS = 1_000L

/** The sessions an app view shows: a sub-second session stays in the day, never in an app row. */
fun visibleSessions(sessions: List<UsageSession>): List<UsageSession> = sessions.filter { it.end - it.start >= MIN_VISIBLE_SESSION_MILLIS }

/** The day's total screen time. */
fun dayTotal(sessions: List<UsageSession>): Long = sessions.sumOf { it.end - it.start }

/**
 * Ranking by app, longest first; excluded apps stay out of the ranking but their time still
 * counts in the total and in the hours.
 */
fun appRanking(
    sessions: List<UsageSession>,
    excludedPackages: Set<String> = emptySet(),
): List<AppUsageEntry> =
    visibleSessions(sessions)
        .filter { it.packageName !in excludedPackages }
        .groupBy { it.packageName }
        .map { (packageName, list) ->
            AppUsageEntry(
                packageName = packageName,
                totalMillis = list.sumOf { it.end - it.start },
                lastUsedAt = list.maxOf { it.end },
                sessions = list.size,
            )
        }.sortedByDescending { it.totalMillis }

/** Hourly distribution, splitting the sessions that cross an hour boundary. */
fun hourlyDistribution(
    sessions: List<UsageSession>,
    zone: ZoneId,
): List<HourBucket> {
    val buckets = LongArray(24)
    sessions.forEach { session ->
        var cursor = session.start
        while (cursor < session.end) {
            val zoned = Instant.ofEpochMilli(cursor).atZone(zone)
            val nextHour =
                zoned
                    .truncatedTo(ChronoUnit.HOURS)
                    .plusHours(1)
                    .toInstant()
                    .toEpochMilli()
            val chunkEnd = minOf(session.end, nextHour)
            buckets[zoned.hour] += chunkEnd - cursor
            cursor = chunkEnd
        }
    }
    return buckets.mapIndexed { hour, millis -> HourBucket(hour, millis) }
}

/** The day's full metrics; [excludedPackages] only affects the ranking. */
fun deriveDayMetrics(
    sessions: List<UsageSession>,
    zone: ZoneId,
    excludedPackages: Set<String> = emptySet(),
): DayMetrics =
    DayMetrics(
        totalMillis = dayTotal(sessions),
        entries = appRanking(sessions, excludedPackages),
        hourly = hourlyDistribution(sessions, zone),
    )
