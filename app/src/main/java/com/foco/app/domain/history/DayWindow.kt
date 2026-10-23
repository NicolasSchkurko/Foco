package com.foco.app.domain.history

import java.time.LocalDate
import java.time.ZoneId

/**
 * A day's query window: start, exclusive end, where it looks from ([lookbackStart], one day earlier)
 * and how far it reaches ([effectiveEnd]); pure: date and clock come in as parameters.
 */
data class DayWindow(
    val startOfDay: Long,
    val dayEndExclusive: Long,
    val lookbackStart: Long,
    val effectiveEnd: Long,
)

/** Builds the day's window for [date] in [zone] with the clock [now]. */
fun dayWindow(
    date: LocalDate,
    zone: ZoneId,
    now: Long,
): DayWindow {
    val startOfDay = date.atStartOfDay(zone).toInstant().toEpochMilli()
    val dayEndExclusive =
        date
            .plusDays(1)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()
    return DayWindow(
        startOfDay = startOfDay,
        dayEndExclusive = dayEndExclusive,
        lookbackStart =
            date
                .minusDays(1)
                .atStartOfDay(zone)
                .toInstant()
                .toEpochMilli(),
        effectiveEnd = minOf(dayEndExclusive, now),
    )
}
