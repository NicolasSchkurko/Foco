package com.foco.app.domain

/** The current ring stop and the positions the user came through, oldest first. */
data class DayRingState(
    val current: DayStop,
    val history: List<DayStop> = emptyList()
)
