package com.foco.app.data.stores

import com.foco.app.model.UsageSession
import com.foco.app.model.day.DayData
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * The marker of a finished day: written with the day's totals and readable back without the
 * platform, until its day leaves the retention window and the next run deletes it.
 */
class ConsolidatedDayTest {
    private val today = LocalDate.of(2026, 10, 19)
    private val yesterday = today.minusDays(1)
    private val platform = mutableMapOf<LocalDate, DayData>()
    private val sessions = FakeSessionRecordDao()
    private val aggregates = FakeDailyAggregateDao()
    private val markers = FakeConsolidatedDayDao()
    private val store =
        SessionHistoryStore(sessions, aggregates, markers, { platform[it] ?: emptyDay })

    @Test
    fun `a consolidated day keeps its unlock count`() =
        runBlocking {
            platform[yesterday] =
                DayData(
                    sessions = listOf(UsageSession("app.a", 0L, 1_000L)),
                    unlockTimes = listOf(100L, 200L, 300L),
                )

            store.consolidate(today)

            assertEquals(3, store.unlocksFor(yesterday))
            assertNull("unconsolidated today has no marker", store.unlocksFor(today))
        }

    @Test
    fun `a day with no usage gets no marker`() =
        runBlocking {
            platform[yesterday] = emptyDay

            store.consolidate(today)

            assertNull(store.unlocksFor(yesterday))
            assertTrue(markers.rows.isEmpty())
        }

    private companion object {
        val emptyDay = DayData(emptyList(), emptyList())
    }
}
