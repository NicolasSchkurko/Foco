package com.foco.app.data

import com.foco.app.data.db.ConsolidatedDay
import com.foco.app.data.db.DailyAggregate
import com.foco.app.data.db.SessionRecord
import com.foco.app.model.UsageSession
import com.foco.app.model.day.DayData
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryRetentionTest {

    private val today = LocalDate.of(2026, 10, 19)
    private val zone = ZoneId.systemDefault()
    private val platform = mutableMapOf<LocalDate, DayData>()
    private val sessions = FakeSessionRecordDao()
    private val aggregates = FakeDailyAggregateDao()
    private val markers = FakeConsolidatedDayDao()
    private val store =
        SessionHistoryStore(sessions, aggregates, markers, { platform[it] ?: emptyDay })

    @Test
    fun `pruning deletes every row of a day older than the window`() = runBlocking {
        val oldDay = today.minusDays(8)
        val start = at(oldDay, 60_000)
        sessions.rows["com.old" to start] = SessionRecord("com.old", start, start + 5_000)
        aggregates.rows["com.old" to oldDay.toString()] =
            DailyAggregate("com.old", oldDay.toString(), 5_000)
        markers.rows[oldDay.toString()] = ConsolidatedDay(oldDay.toString(), 3)

        store.consolidate(today)

        assertTrue(sessions.rows.isEmpty())
        assertTrue(aggregates.rows.isEmpty())
        assertTrue(markers.rows.isEmpty())
    }

    @Test
    fun `the guard day keeps its rows in all three tables`() = runBlocking {
        val guardDay = today.minusDays(7)
        val start = at(guardDay, 60_000)
        sessions.rows["com.guard" to start] = SessionRecord("com.guard", start, start + 5_000)
        aggregates.rows["com.guard" to guardDay.toString()] =
            DailyAggregate("com.guard", guardDay.toString(), 5_000)
        markers.rows[guardDay.toString()] = ConsolidatedDay(guardDay.toString(), 2)

        store.consolidate(today)

        assertEquals(1, sessions.rows.size)
        assertEquals(1, aggregates.rows.size)
        assertEquals(1, markers.rows.size)
    }

    @Test
    fun `a raw row inside the window is untouched`() = runBlocking {
        val inside = at(today.minusDays(6), 1_000)
        sessions.rows["com.inside" to inside] = SessionRecord("com.inside", inside, inside + 1_000)

        store.consolidate(today)

        assertEquals(1, sessions.rows.size)
    }

    @Test
    fun `an empty day is retried on the next run`() = runBlocking {
        val yesterday = today.minusDays(1)
        store.consolidate(today)
        assertEquals(0, sessions.rows.size)

        platform[yesterday] = DayData(
            sessions = listOf(UsageSession("com.a", at(yesterday, 60_000), at(yesterday, 120_000))),
            unlockTimes = emptyList()
        )
        store.consolidate(today)

        assertEquals(1, sessions.rows.size)
        assertEquals(60_000L, aggregates.rows.getValue("com.a" to yesterday.toString()).totalMillis)
    }

    private fun at(day: LocalDate, millisIntoDay: Long): Long =
        day.atStartOfDay(zone).toInstant().toEpochMilli() + millisIntoDay

    private companion object {
        val emptyDay = DayData(sessions = emptyList(), unlockTimes = emptyList())
    }
}
