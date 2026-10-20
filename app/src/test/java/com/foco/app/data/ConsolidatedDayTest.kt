package com.foco.app.data

import com.foco.app.data.db.ConsolidatedDay
import com.foco.app.data.db.DailyAggregate
import com.foco.app.model.UsageSession
import com.foco.app.model.day.DayData
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

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
    fun `a day inside the window keeps its count while older days are pruned`() = runBlocking {
        platform[yesterday] = dayWithUnlocks(4)
        store.consolidate(today)
        val oldDay = today.minusDays(9)
        markers.rows[oldDay.toString()] = ConsolidatedDay(oldDay.toString(), 1)
        aggregates.rows["com.old" to oldDay.toString()] =
            DailyAggregate("com.old", oldDay.toString(), 1_000)

        store.consolidate(today.plusDays(1))

        assertEquals(4, store.unlocksFor(yesterday))
        assertNull(store.unlocksFor(oldDay))
        assertTrue(aggregates.rows.keys.none { it.second == oldDay.toString() })
    }

    @Test
    fun `a day outside the window loses its marker and count`() = runBlocking {
        platform[yesterday] = dayWithUnlocks(4)
        store.consolidate(today)

        store.consolidate(today.plusDays(8))

        assertNull(store.unlocksFor(yesterday))
        assertTrue(markers.rows.isEmpty())
        assertTrue(aggregates.rows.isEmpty())
        assertTrue(sessions.rows.isEmpty())
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
