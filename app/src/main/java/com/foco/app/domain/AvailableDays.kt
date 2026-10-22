package com.foco.app.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** The days the ring and the selector offer: today and the six days behind it. */
const val DAY_WINDOW_DAYS = 7

/**
 * Days the detail screen's selector offers: today and every day behind it, down to the oldest
 * stored day inside the raw retention ([windowDays] entries at most, newest first).
 */
fun availableDays(
    today: LocalDate,
    storedDays: Set<LocalDate>,
    windowDays: Int = DAY_WINDOW_DAYS,
): List<LocalDate> {
    val windowFloor = recentDays(today, windowDays).last()
    val oldest = storedDays.filter { it in windowFloor..today.minusDays(1) }.minOrNull() ?: today
    return (0..ChronoUnit.DAYS.between(oldest, today)).map { today.minusDays(it) }
}

/** The days whose data is final: the same window without today, which is still being built. */
fun finishedDays(
    today: LocalDate,
    windowDays: Int = DAY_WINDOW_DAYS,
): List<LocalDate> = recentDays(today, windowDays).drop(1)

/** The last [windowDays] days, newest first: today and the days behind it. */
fun recentDays(
    today: LocalDate,
    windowDays: Int = DAY_WINDOW_DAYS,
): List<LocalDate> {
    require(windowDays >= 1) { "windowDays must be at least 1" }
    return (0 until windowDays).map { today.minusDays(it.toLong()) }
}

/** The dates the window covers: today and the days behind it, oldest first. */
fun dayWindowRange(today: LocalDate): ClosedRange<LocalDate> = today.minusDays((DAY_WINDOW_DAYS - 1).toLong())..today
