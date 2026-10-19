package com.foco.app.ui.metrics

import com.foco.app.model.UsageSession
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** The day's total screen time. */
fun dayTotal(sessions: List<UsageSession>): Long =
    sessions.sumOf { it.end - it.start }

/**
 * Ranking by app, longest first; excluded apps stay out of the ranking but their time still
 * counts in the total and in the hours.
 */
fun appRanking(
    sessions: List<UsageSession>,
    excludedPackages: Set<String> = emptySet()
): List<AppUsageEntry> =
    sessions
        .filter { it.packageName !in excludedPackages }
        .groupBy { it.packageName }
        .map { (packageName, list) ->
            AppUsageEntry(
                packageName = packageName,
                totalMillis = list.sumOf { it.end - it.start },
                lastUsedAt = list.maxOf { it.end },
                sessions = list.size
            )
        }
        .sortedByDescending { it.totalMillis }

/** Hourly distribution, splitting the sessions that cross an hour boundary. */
fun hourlyDistribution(sessions: List<UsageSession>, zone: ZoneId): List<HourBucket> {
    val buckets = LongArray(24)
    sessions.forEach { session ->
        var cursor = session.start
        while (cursor < session.end) {
            val zoned = Instant.ofEpochMilli(cursor).atZone(zone)
            val nextHour = zoned.truncatedTo(ChronoUnit.HOURS).plusHours(1).toInstant().toEpochMilli()
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
    excludedPackages: Set<String> = emptySet()
): DayMetrics = DayMetrics(
    totalMillis = dayTotal(sessions),
    entries = appRanking(sessions, excludedPackages),
    hourly = hourlyDistribution(sessions, zone)
)
