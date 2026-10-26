package com.foco.app.data.stores

import com.foco.app.data.db.history.consolidatedday.ConsolidatedDay
import com.foco.app.data.db.history.dailyaggregate.DailyAggregate
import com.foco.app.data.db.history.sessionrecord.SessionRecord
import com.foco.app.model.day.DayData
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

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
    fun `raw sessions older than seven days before today are pruned`() =
        runBlocking {
            val keptDay = today.minusDays(7)
            val staleDay = today.minusDays(8)
            val keptStart = rawStart(keptDay)
            val staleStart = rawStart(staleDay)

            val kept = SessionRecord("app.a", keptStart, keptStart + 1_000L)
            val stale = SessionRecord("app.a", staleStart, staleStart + 1_000L)
            sessions.rows[kept.packageName to kept.start] = kept
            sessions.rows[stale.packageName to stale.start] = stale

            store.consolidate(today)

            assertEquals(listOf(kept), sessions.rows.values.toList())
        }

    @Test
    fun `aggregates and markers older than seven days before today are pruned`() =
        runBlocking {
            val keptDay = today.minusDays(7)
            val staleDay = today.minusDays(8)

            aggregates.rows["app.a" to keptDay.toString()] = DailyAggregate("app.a", keptDay.toString(), 1_000L)
            aggregates.rows["app.a" to staleDay.toString()] = DailyAggregate("app.a", staleDay.toString(), 1_000L)
            markers.rows[keptDay.toString()] = ConsolidatedDay(keptDay.toString(), 1)
            markers.rows[staleDay.toString()] = ConsolidatedDay(staleDay.toString(), 1)

            store.consolidate(today)

            assertEquals(
                setOf(keptDay.toString()),
                aggregates.rows.values
                    .map { it.day }
                    .toSet(),
            )
            assertEquals(setOf(keptDay.toString()), markers.rows.keys)
        }

    @Test
    fun `pruning handles gaps and lagging consolidation`() =
        runBlocking {
            val oldDay = today.minusDays(14)
            val oldStart = rawStart(oldDay)
            val oldRecord = SessionRecord("app.a", oldStart, oldStart + 1_000L)
            sessions.rows[oldRecord.packageName to oldRecord.start] = oldRecord
            aggregates.rows["app.a" to oldDay.toString()] = DailyAggregate("app.a", oldDay.toString(), 1_000L)
            markers.rows[oldDay.toString()] = ConsolidatedDay(oldDay.toString(), 1)

            store.consolidate(today)

            assertTrue(sessions.rows.isEmpty())
            assertTrue(aggregates.rows.isEmpty())
            assertTrue(markers.rows.isEmpty())
        }

    private fun rawStart(day: LocalDate): Long = day.atStartOfDay(zone).toInstant().toEpochMilli()

    private companion object {
        val emptyDay = DayData(emptyList(), emptyList())
    }
}
