package com.foco.app.ui.detail

import com.foco.app.domain.limits.AppWindow
import com.foco.app.model.UsageSession
import com.foco.app.ui.metrics.detail.AppDayDetail
import java.time.LocalDate

/** UI state for the app detail screen. */
data class AppDetailUiState(
    val packageName: String,
    val selectedDate: LocalDate,
    val shownDate: LocalDate = selectedDate,
    val effectiveIndex: Int = 0,
    val days: List<LocalDate> = emptyList(),
    val slideDirection: Int = 1,
    val dayDetails: Map<LocalDate, Pair<List<UsageSession>, AppDayDetail?>> = emptyMap(),
    val limitMinutes: Int? = null,
    val window: AppWindow? = null,
    val alertOn: Boolean = false,
    val selectorText: String = "",
) {
    val currentPair: Pair<List<UsageSession>, AppDayDetail?>? get() = dayDetails[shownDate]
    val sessions: List<UsageSession>? get() = currentPair?.first
    val detail: AppDayDetail? get() = currentPair?.second
}
