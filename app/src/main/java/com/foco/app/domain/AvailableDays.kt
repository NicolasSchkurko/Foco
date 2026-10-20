package com.foco.app.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

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

/** The days whose data is final: the same window without today, which is still being built. */
fun finishedDays(
    today: LocalDate,
    windowDays: Int = 7,
): List<LocalDate> {
    require(windowDays >= 1) { "windowDays must be at least 1" }
    return (1 until windowDays).map { today.minusDays(it.toLong()) }
}
