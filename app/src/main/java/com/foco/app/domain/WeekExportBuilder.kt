package com.foco.app.domain

import com.foco.app.model.UsageSession
import com.foco.app.model.export.ExportApp
import com.foco.app.model.export.ExportDay
import com.foco.app.model.export.ExportSession
import com.foco.app.model.export.WeekExport
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Builds the export: raw sessions win, a stored total without rows survives, and no visibility or
 * exclusion filter applies — this is a record, not the ranking.
 */
fun buildWeekExport(
    weekStart: LocalDate,
    weekEnd: LocalDate,
    storedTotalsByDay: Map<LocalDate, Map<String, Long>>,
    sessionsByDay: Map<LocalDate, List<UsageSession>>,
    todaySessions: List<UsageSession>,
    labels: (String) -> String,
    zone: ZoneId
): WeekExport {
    val days = generateSequence(weekStart) { it.plusDays(1) }
        .takeWhile { !it.isAfter(weekEnd) }
        .map { date ->
            val sessions = mergeContiguousSessions(
                if (date == weekEnd) todaySessions else sessionsByDay[date].orEmpty(),
                zone
            )
            val stored = storedTotalsByDay[date].orEmpty()
            val apps = (stored.keys + sessions.map { it.packageName })
                .sorted()
                .map { packageName ->
                    val mine = sessions.filter { it.packageName == packageName }.sortedBy { it.start }
                    val total = if (mine.isEmpty()) {
                        stored[packageName] ?: 0L
                    } else {
                        mine.sumOf { it.end - it.start }
                    }
                    ExportApp(
                        packageName = packageName,
                        appName = labels(packageName),
                        totalMillis = total,
                        sessionsAvailable = mine.isNotEmpty(),
                        sessions = mine.map { session ->
                            ExportSession(
                                start = isoTimestamp(session.start, zone),
                                end = isoTimestamp(session.end, zone),
                                durationMillis = session.end - session.start
                            )
                        }
                    )
                }
                .sortedWith(compareByDescending<ExportApp> { it.totalMillis }.thenBy { it.packageName })
            ExportDay(
                date = date.toString(),
                totalMillis = apps.sumOf { it.totalMillis },
                apps = apps
            )
        }
        .toList()
    return WeekExport(weekStart = weekStart.toString(), weekEnd = weekEnd.toString(), days = days)
}

/** ISO-8601 with the local offset, e.g. `2026-10-19T09:00:01.234+02:00`. */
fun isoTimestamp(millis: Long, zone: ZoneId): String =
    Instant.ofEpochMilli(millis).atZone(zone).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
