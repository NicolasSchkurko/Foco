package com.foco.app.data

import com.foco.app.domain.ConsolidationOutcome
import com.foco.app.data.db.ConsolidatedDay
import com.foco.app.data.db.DailyAggregate
import com.foco.app.data.db.SessionRecord
import com.foco.app.model.UsageSession
import com.foco.app.model.day.DayData
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConsolidationRetryTest {

    private val today = LocalDate.of(2026, 10, 19)
    private val oldDay = today.minusDays(10)

    @Test
    fun `a read that throws fails the run and leaves the stored rows and the raw rows alone`() =
        runBlocking {
            val sessions = FakeSessionRecordDao()
            val aggregates = FakeDailyAggregateDao()
            val markers = FakeConsolidatedDayDao()
            val oldRow = SessionRecord("app.a", oldDay.toEpochDay() * 86_400_000L, 0L)
            sessions.rows[oldRow.packageName to oldRow.start] = oldRow
            aggregates.rows["app.a" to oldDay.toString()] =
                DailyAggregate("app.a", oldDay.toString(), 1_000)
            markers.rows[oldDay.toString()] = ConsolidatedDay(oldDay.toString(), 2)
            val store = SessionHistoryStore(sessions, aggregates, markers) {
                throw IllegalStateException("platform busy")
            }
            val runner = ConsolidationRunner({ day -> store.consolidate(day) })

            val outcome = runner.run(today)

            assertTrue(outcome is ConsolidationOutcome.Failed)
            assertTrue("no marker may be pruned", markers.rows.isNotEmpty())
            assertTrue("no totals may be pruned", aggregates.rows.isNotEmpty())
            assertTrue("no raw row may be pruned", sessions.rows.isNotEmpty())
        }

    @Test
    fun `a successful run prunes only after the day is stored`() = runBlocking {
        val sessions = FakeSessionRecordDao()
        val aggregates = FakeDailyAggregateDao()
        val markers = FakeConsolidatedDayDao()
        val oldRow = SessionRecord("app.a", oldDay.toEpochDay() * 86_400_000L, 0L)
        sessions.rows[oldRow.packageName to oldRow.start] = oldRow
        aggregates.rows["app.a" to oldDay.toString()] =
            DailyAggregate("app.a", oldDay.toString(), 1_000)
        markers.rows[oldDay.toString()] = ConsolidatedDay(oldDay.toString(), 2)
        val yesterday = today.minusDays(1)
        val start = yesterday.toEpochDay() * 86_400_000L
        val store = SessionHistoryStore(sessions, aggregates, markers) { day ->
            if (day == yesterday) {
                DayData(listOf(UsageSession("app.a", start, start + 60_000L)), emptyList())
            } else {
                DayData(emptyList(), emptyList())
            }
        }
        val runner = ConsolidationRunner({ day -> store.consolidate(day) })

        val outcome = runner.run(today)

        assertEquals(ConsolidationOutcome.Done, outcome)
        assertEquals(setOf("app.a" to yesterday.toString()), aggregates.rows.keys)
        assertTrue("the old marker is pruned", oldDay.toString() !in markers.rows.keys)
        assertEquals(setOf(yesterday.toString()), markers.rows.keys)
        assertTrue("the raw row older than the window is pruned", oldRow !in sessions.rows.values)
        assertTrue(
            "the day's own raw row stays",
            sessions.rows.values.any { it.start == start }
        )
    }
}
