package com.foco.app.data

import com.foco.app.data.db.DailyAggregate
import com.foco.app.data.db.DailyAggregateDao
import com.foco.app.data.db.SessionRecord
import com.foco.app.data.db.SessionRecordDao
import com.foco.app.model.day.DayData
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryRetentionTest {

    private val today = LocalDate.of(2026, 10, 19)
    private val zone = ZoneId.systemDefault()
    private val platform = mutableMapOf<LocalDate, DayData>()
    private val sessions = FakeSessionRecordDao()
    private val aggregates = FakeDailyAggregateDao()
    private val store = SessionHistoryStore(sessions, aggregates, { platform[it] ?: emptyDay }, zone)

    @Test
    fun `pruning deletes raw rows older than seven days and keeps their totals`() = runBlocking {
        val oldDay = today.minusDays(8)
        val start = oldDay.atStartOfDay(zone).toInstant().toEpochMilli() + 60_000
        sessions.rows["com.old" to start] = SessionRecord("com.old", start, start + 5_000)
        aggregates.rows["com.old" to oldDay.toString()] =
            DailyAggregate("com.old", oldDay.toString(), 5_000)

        store.consolidate(today)

        assertEquals(0, sessions.rows.size)
        assertEquals(
            5_000L,
            aggregates.rows.getValue("com.old" to oldDay.toString()).totalMillis
        )
    }

    @Test
    fun `a raw row inside the window is untouched`() = runBlocking {
        val inside = today.minusDays(6).atStartOfDay(zone).toInstant().toEpochMilli() + 1_000
        sessions.rows["com.inside" to inside] = SessionRecord("com.inside", inside, inside + 1_000)

        store.consolidate(today)

        assertEquals(1, sessions.rows.size)
    }

    @Test
    fun `a day without usage is left for the next run`() = runBlocking {
        store.consolidate(today)

        assertEquals(0, sessions.rows.size)
        assertEquals(0, aggregates.rows.size)
    }

    private class FakeSessionRecordDao : SessionRecordDao {

        val rows = mutableMapOf<Pair<String, Long>, SessionRecord>()

        override suspend fun upsert(records: List<SessionRecord>) {
            records.forEach { rows[it.packageName to it.start] = it }
        }

        override suspend fun sessionsBetween(from: Long, to: Long): List<SessionRecord> =
            rows.values.filter { it.start >= from && it.start < to }.sortedBy { it.start }

        override suspend fun pruneBefore(cutoff: Long): Int {
            val before = rows.size
            rows.values.removeIf { it.end < cutoff }
            return before - rows.size
        }
    }

    private class FakeDailyAggregateDao : DailyAggregateDao {

        val rows = mutableMapOf<Pair<String, String>, DailyAggregate>()

        override suspend fun upsert(rows: List<DailyAggregate>) {
            rows.forEach { this.rows[it.packageName to it.day] = it }
        }

        override suspend fun storedDays(): List<String> = rows.keys.map { it.second }.distinct()

        override suspend fun totalsFor(from: String, to: String): List<DailyAggregate> =
            rows.values.filter { it.day in from..to }.sortedWith(compareBy({ it.day }, { it.packageName }))
    }

    private companion object {
        val emptyDay = DayData(sessions = emptyList(), unlockTimes = emptyList())
    }
}
