package com.foco.app.domain.history

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/** Reclamp: same date when still present, nearest valid otherwise, today when empty. */
fun reclampDayIndex(
    selectedIndex: Int,
    previousDays: List<LocalDate>,
    days: List<LocalDate>,
    today: LocalDate,
): Int {
    if (days.isEmpty()) return 0
    val clamped = reclampDay(previousDays.getOrNull(selectedIndex), days, today)
    return days.indexOf(clamped).takeIf { it >= 0 } ?: 0
}

/** Reclamp a date: the same date when it is still offered, the nearest one otherwise. */
fun reclampDay(
    previousDate: LocalDate?,
    days: List<LocalDate>,
    today: LocalDate,
): LocalDate {
    if (days.isEmpty()) return today
    if (previousDate != null) {
        if (previousDate in days) return previousDate
        val closest = days.minByOrNull { abs(ChronoUnit.DAYS.between(it, previousDate)) }
        if (closest != null) return closest
    }
    return days.firstOrNull { it == today } ?: days.first()
}
