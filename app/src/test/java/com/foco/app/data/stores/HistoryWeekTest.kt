package com.foco.app.data.stores

import com.foco.app.data.db.DailyAggregate
import com.foco.app.model.day.DayData
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class HistoryWeekTest {
    private val today = LocalDate.of(2026, 10, 19)
    private val aggregates = FakeDailyAggregateDao()
    private val store =
        SessionHistoryStore(
            FakeSessionRecordDao(),
            aggregates,
            FakeConsolidatedDayDao(),
            { DayData(sessions = emptyList(), unlockTimes = emptyList()) },
        )

    @Test
    fun `the window is the seven days ending at the requested day`() =
        runBlocking {
            val startDay = today.minusDays(6)
            val beforeStart = startDay.minusDays(1)
            val afterEnd = today.plusDays(1)

            aggregates.rows["app.a" to beforeStart.toString()] = DailyAggregate("app.a", beforeStart.toString(), 1_000L)
            aggregates.rows["app.a" to startDay.toString()] = DailyAggregate("app.a", startDay.toString(), 2_000L)
            aggregates.rows["app.a" to today.toString()] = DailyAggregate("app.a", today.toString(), 3_000L)
            aggregates.rows["app.a" to afterEnd.toString()] = DailyAggregate("app.a", afterEnd.toString(), 4_000L)

            val weekRows = store.week(today)

            assertEquals(listOf(startDay.toString(), today.toString()), weekRows.map { it.day })
        }
}
