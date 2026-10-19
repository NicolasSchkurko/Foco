package com.foco.app.domain

import java.time.LocalDate
import java.time.ZoneId

/**
 * Ventana de consulta de un día: inicio, fin exclusivo, desde dónde mira ([lookbackStart], un día antes)
 * y hasta dónde llega ([effectiveEnd]); pura, la fecha y el reloj entran por parámetro.
 */
data class DayWindow(
    val startOfDay: Long,
    val dayEndExclusive: Long,
    val lookbackStart: Long,
    val effectiveEnd: Long
)

/** Arma la ventana del día [date] en [zone] con el reloj [now]. */
fun dayWindow(date: LocalDate, zone: ZoneId, now: Long): DayWindow {
    val startOfDay = date.atStartOfDay(zone).toInstant().toEpochMilli()
    val dayEndExclusive = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    return DayWindow(
        startOfDay = startOfDay,
        dayEndExclusive = dayEndExclusive,
        lookbackStart = date.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(),
        effectiveEnd = minOf(dayEndExclusive, now)
    )
}
