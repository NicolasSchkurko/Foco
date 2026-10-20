package com.foco.app.ui

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** 3 h 24 min / 12 min / 40 s depending on magnitude. */
fun formatDuration(millis: Long): String {
    if (millis < 60_000) return "${millis / 1000} s"
    val totalMinutes = millis / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "$hours h $minutes min" else "$minutes min"
}

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

fun formatTimeOfDay(
    epochMillis: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): String = Instant.ofEpochMilli(epochMillis).atZone(zone).format(timeFormatter)

private val weekdayFormatter = DateTimeFormatter.ofPattern("EEE")

/** The week bar's label: the device locale's short weekday (e.g. Mon / lun). */
fun formatWeekday(date: LocalDate): String = weekdayFormatter.format(date)

private val dayFormatter = DateTimeFormatter.ofPattern("EEE d MMM")

/**
 * Day selector label: today/yesterday use the active language's; the rest, the abbreviated date
 * according to the device's locale.
 */
fun formatDayLabel(
    date: LocalDate,
    today: LocalDate,
    todayLabel: String,
    yesterdayLabel: String,
): String =
    when (date) {
        today -> todayLabel
        today.minusDays(1) -> yesterdayLabel
        else -> dayFormatter.format(date)
    }
