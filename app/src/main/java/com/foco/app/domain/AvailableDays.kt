package com.foco.app.domain

import java.time.LocalDate

/**
 * Days the detail screen's selector can offer: today and the previous ones
 * inside the window the system retains (7 by default).
 */
fun availableDays(today: LocalDate, windowDays: Int = 7): List<LocalDate> {
    require(windowDays >= 1) { "windowDays must be at least 1" }
    return (0 until windowDays).map { today.minusDays(it.toLong()) }
}

/** The days whose data is final: the same window without today, which is still being built. */
fun finishedDays(today: LocalDate, windowDays: Int = 7): List<LocalDate> =
    availableDays(today, windowDays).drop(1)
