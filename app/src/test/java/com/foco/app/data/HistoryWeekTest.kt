package com.foco.app.data

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
            (0..8).forEach { ago ->
                val day = today.minusDays(ago.toLong()).toString()
                aggregates.rows["com.a" to day] = DailyAggregate("com.a", day, 1_000)
            }

            val days = store.week(today).map { it.day }

            assertEquals((6 downTo 0).map { today.minusDays(it.toLong()).toString() }, days)
        }

    @Test
    fun `an empty store yields nothing`() =
        runBlocking {
            assertEquals(emptyList<DailyAggregate>(), store.week(today))
        }
}
