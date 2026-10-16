package com.foco.app.ui.metrics

import com.foco.app.model.UsageSession
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Tiempo total en pantalla del día. */
fun dayTotal(sessions: List<UsageSession>): Long =
    sessions.sumOf { it.end - it.start }

/** Ranking por app, ordenado por duración total descendente. */
fun appRanking(sessions: List<UsageSession>): List<AppUsageEntry> =
    sessions
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

/** Distribución por hora, repartiendo las sesiones que cruzan un límite de hora. */
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

/** Métricas completas del día, a partir de la lista de sesiones. */
fun deriveDayMetrics(sessions: List<UsageSession>, zone: ZoneId): DayMetrics = DayMetrics(
    totalMillis = dayTotal(sessions),
    entries = appRanking(sessions),
    hourly = hourlyDistribution(sessions, zone)
)
