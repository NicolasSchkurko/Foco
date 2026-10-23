package com.foco.app.domain.history

import java.time.LocalDate

/**
 * The read rule for one day: today is [DaySource.Live] (never consolidated, so the live read
 * wins), a day the store has is [DaySource.Stored], anything else is [DaySource.Absent].
 */
fun daySource(
    date: LocalDate,
    today: LocalDate,
    storedDays: Set<LocalDate>,
): DaySource =
    when {
        date == today -> DaySource.Live
        date in storedDays -> DaySource.Stored
        else -> DaySource.Absent
    }
