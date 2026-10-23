package com.foco.app.ui.metrics.week

import java.time.LocalDate

/** One day of the week view: its date, the day's screen time and where that number came from. */
data class WeekDayBar(
    val date: LocalDate,
    val totalMillis: Long,
    val state: State,
) {
    /** Absence is a case of its own: a day the store has nothing for is not a zero. */
    enum class State { Live, Stored, Absent }
}
