package com.foco.app.data

import com.foco.app.data.db.DailyAggregate
import com.foco.app.data.db.DailyAggregateDao
import com.foco.app.data.db.SessionRecord
import com.foco.app.data.db.SessionRecordDao
import com.foco.app.model.UsageSession
import com.foco.app.model.day.DayData
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryConsolidationTest {

    private val today = LocalDate.of(2026, 10, 19)
    private val yesterday = today.minusDays(1)
    private val platform = mutableMapOf<LocalDate, DayData>()
    private val sessions = FakeSessionRecordDao()
    private val aggregates = FakeDailyAggregateDao()
    private val store = SessionHistoryStore(sessions, aggregates, { platform[it] ?: emptyDay })

    @Test
    fun `a consolidated day is stored and read back with the same total`() = runBlocking {
        platform[yesterday] = dayWith("com.instagram.android" to minutes(yesterday, 0L, 1L))

        store.consolidate(today)

        val stored = sessions.rows.values.single()
        assertEquals("com.instagram.android", stored.packageName)
        assertEquals(60_000L, stored.end - stored.start)
        assertEquals(
            60_000L,
            aggregates.rows.getValue("com.instagram.android" to yesterday.toString()).totalMillis
        )
    }

    @Test
    fun `a day the platform no longer serves still yields its data`() = runBlocking {
        platform[yesterday] = dayWith("com.instagram.android" to minutes(yesterday, 0L, 1L))
        store.consolidate(today)
        platform.clear()

        store.consolidate(today)

        assertEquals(1, sessions.rows.size)
        assertEquals(
            60_000L,
            aggregates.rows.getValue("com.instagram.android" to yesterday.toString()).totalMillis
        )
    }

    @Test
    fun `consolidating twice leaves identical rows`() = runBlocking {
        platform[yesterday] = dayWith("com.instagram.android" to minutes(yesterday, 0L, 1L))

        store.consolidate(today)
        val firstSessions = sessions.rows.values.toList()
        val firstTotals = aggregates.rows.values.toList()
        store.consolidate(today)

        assertEquals(firstSessions, sessions.rows.values.toList())
        assertEquals(firstTotals, aggregates.rows.values.toList())
    }

    @Test
    fun `today is never consolidated`() = runBlocking {
        platform[today] = dayWith("com.instagram.android" to minutes(today, 0L, 1L))

        store.consolidate(today)

        assertEquals(0, sessions.rows.size)
    }

    private fun minutes(day: LocalDate, fromMinute: Long, toMinute: Long): Pair<Long, Long> {
        val startOfDay = day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return startOfDay + fromMinute * 60_000 to startOfDay + toMinute * 60_000
    }

    private fun dayWith(vararg sessionsOf: Pair<String, Pair<Long, Long>>) = DayData(
        sessions = sessionsOf.map { UsageSession(it.first, it.second.first, it.second.second) },
        unlockTimes = emptyList()
    )

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
