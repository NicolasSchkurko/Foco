package com.foco.app.ui.dashboard

import com.foco.app.data.db.DailyAggregate
import com.foco.app.model.day.DayData

/** What one ring position renders once its data is ready; the home caches one entry per position. */
sealed interface RingContent {

    data class Day(val data: DayData, val unlocks: Int) : RingContent

    data class Week(val totals: List<DailyAggregate>) : RingContent
}
