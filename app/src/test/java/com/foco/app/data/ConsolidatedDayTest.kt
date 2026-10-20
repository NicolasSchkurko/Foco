package com.foco.app.data

import com.foco.app.model.UsageSession
import com.foco.app.model.day.DayData
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The marker of a finished day: written with the day's totals, readable back without the platform,
 * and untouched by the pruning that removes the raw rows.
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
    fun `a consolidated day keeps its unlock count`() = runBlocking {
        platform[yesterday] = dayWithUnlocks(3)

        store.consolidate(today)

        assertEquals(3, markers.rows.getValue(yesterday.toString()).count)
        assertEquals(3, store.unlocksFor(yesterday))
    }

    @Test
    fun `a consolidated day with no unlocks answers zero`() = runBlocking {
        platform[yesterday] = dayWithUnlocks(0)

        store.consolidate(today)

        assertEquals(0, markers.rows.getValue(yesterday.toString()).count)
        assertEquals(0, store.unlocksFor(yesterday))
    }

    @Test
    fun `a day never consolidated has no stored count`() = runBlocking {
        assertNull(store.unlocksFor(yesterday))
    }

    @Test
    fun `consolidating the same day again keeps one marker`() = runBlocking {
        platform[yesterday] = dayWithUnlocks(3)
        store.consolidate(today)
        // The marker is what makes the day pending again; the totals alone no longer do.
        markers.rows.clear()

        store.consolidate(today)

        assertEquals(1, markers.rows.size)
        assertEquals(3, store.unlocksFor(yesterday))
    }

    @Test
    fun `the count outlives the raw rows`() = runBlocking {
        platform[yesterday] = dayWithUnlocks(4)
        store.consolidate(today)

        // A later consolidation prunes the raw rows of the previous day and keeps its marker.
        store.consolidate(today.plusDays(8))

        assertEquals(0, sessions.rows.size)
        assertEquals(4, store.unlocksFor(yesterday))
    }

    private fun dayWithUnlocks(count: Int): DayData {
        val start = yesterday.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return DayData(
            sessions = listOf(UsageSession("com.a", start, start + 60_000)),
            unlockTimes = List(count) { start + it * 1_000L }
        )
    }

    private companion object {
        val emptyDay = DayData(sessions = emptyList(), unlockTimes = emptyList())
    }
}
