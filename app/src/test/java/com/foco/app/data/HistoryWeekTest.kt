package com.foco.app.data

import com.foco.app.data.db.DailyAggregate
import com.foco.app.data.db.SessionRecord
import com.foco.app.model.day.DayData
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

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

    @Test
    fun `the range read buckets every session under its own day`() =
        runBlocking {
            val sessions = FakeSessionRecordDao()
            val rangeStore =
                SessionHistoryStore(
                    sessions,
                    aggregates,
                    FakeConsolidatedDayDao(),
                    { DayData(sessions = emptyList(), unlockTimes = emptyList()) },
                )
            val zone = ZoneId.systemDefault()
            val yesterday = today.minusDays(1)
            val twoDaysAgo = today.minusDays(2)
            sessions.rows["com.a" to startOf(zone, yesterday)] =
                SessionRecord("com.a", startOf(zone, yesterday), startOf(zone, yesterday) + 60_000)
            sessions.rows["com.b" to startOf(zone, twoDaysAgo)] =
                SessionRecord("com.b", startOf(zone, twoDaysAgo), startOf(zone, twoDaysAgo) + 60_000)

            val byDay = rangeStore.sessionsBetween(today.minusDays(6), today)

            assertEquals(setOf(yesterday, twoDaysAgo), byDay.keys)
            assertEquals("com.a", byDay.getValue(yesterday).single().packageName)
            assertEquals("com.b", byDay.getValue(twoDaysAgo).single().packageName)
        }

    private fun startOf(
        zone: ZoneId,
        day: LocalDate,
    ): Long = day.atStartOfDay(zone).toInstant().toEpochMilli()
}
