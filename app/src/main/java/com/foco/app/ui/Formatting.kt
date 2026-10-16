package com.foco.app.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** 3 h 24 min / 12 min / 40 s según magnitud. */
fun formatDuration(millis: Long): String {
    if (millis < 60_000) return "${millis / 1000} s"
    val totalMinutes = millis / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "$hours h $minutes min" else "$minutes min"
}

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

fun formatTimeOfDay(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    Instant.ofEpochMilli(epochMillis).atZone(zone).format(timeFormatter)
