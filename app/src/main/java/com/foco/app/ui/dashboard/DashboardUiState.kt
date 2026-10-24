package com.foco.app.ui.dashboard

import com.foco.app.domain.ring.DayRingState
import com.foco.app.domain.ring.DayStop
import com.foco.app.domain.ring.key
import java.time.LocalDate

/** UI state for the home dashboard. */
data class DashboardUiState(
    val shown: DayStop,
    val ringState: DayRingState,
    val slideDirection: Int = 1,
    val softMotion: Boolean = false,
    val ringContent: Map<String, RingContent> = emptyMap(),
    val excludedPackages: Set<String> = emptySet(),
    val storedDays: Set<LocalDate> = emptySet(),
    val today: LocalDate = LocalDate.now(),
) {
    val requested: DayStop get() = ringState.current
    val requestedKey: String get() = requested.key()
    val shownKey: String get() = shown.key()
}
