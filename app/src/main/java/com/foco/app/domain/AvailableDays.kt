package com.foco.app.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * Days the detail screen's selector offers: today and every day behind it, down to the oldest
 * stored day inside the raw retention ([windowDays] entries at most, newest first).
 */
fun availableDays(
    today: LocalDate,
    storedDays: Set<LocalDate>,
    windowDays: Int = 7,
): List<LocalDate> {
    require(windowDays >= 1) { "windowDays must be at least 1" }
    val windowFloor = today.minusDays((windowDays - 1).toLong())
    val oldest = storedDays.filter { it in windowFloor..today.minusDays(1) }.minOrNull() ?: today
    return (0..ChronoUnit.DAYS.between(oldest, today)).map { today.minusDays(it) }
}

/** Reclamp: same date when still present, nearest valid otherwise, today when empty. */
fun reclampDayIndex(
    selectedIndex: Int,
    previousDays: List<LocalDate>,
    days: List<LocalDate>,
    today: LocalDate,
): Int {
    if (days.isEmpty()) return 0
    val previousDate = previousDays.getOrNull(selectedIndex)
    if (previousDate != null) {
        val kept = days.indexOf(previousDate)
        if (kept >= 0) return kept
        val closest = days.minByOrNull { abs(ChronoUnit.DAYS.between(it, previousDate)) }
        if (closest != null) return days.indexOf(closest)
    }
    val todayIndex = days.indexOf(today)
    return if (todayIndex >= 0) todayIndex else 0
}

/** The days whose data is final: the same window without today, which is still being built. */
fun finishedDays(
    today: LocalDate,
    windowDays: Int = 7,
): List<LocalDate> {
    require(windowDays >= 1) { "windowDays must be at least 1" }
    return (1 until windowDays).map { today.minusDays(it.toLong()) }
}
