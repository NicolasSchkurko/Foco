package com.foco.app.data

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
    private val store =
        SessionHistoryStore(sessions, aggregates, FakeDailyUnlockDao(), { platform[it] ?: emptyDay })

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

    private companion object {
        val emptyDay = DayData(sessions = emptyList(), unlockTimes = emptyList())
    }
}
