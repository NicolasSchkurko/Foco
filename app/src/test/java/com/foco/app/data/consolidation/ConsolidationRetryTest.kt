package com.foco.app.data.consolidation

import com.foco.app.data.db.history.consolidatedday.ConsolidatedDay
import com.foco.app.data.db.history.dailyaggregate.DailyAggregate
import com.foco.app.data.db.history.sessionrecord.SessionRecord
import com.foco.app.data.stores.FakeConsolidatedDayDao
import com.foco.app.data.stores.FakeDailyAggregateDao
import com.foco.app.data.stores.FakeSessionRecordDao
import com.foco.app.data.stores.SessionHistoryStore
import com.foco.app.domain.consolidation.ConsolidationOutcome
import com.foco.app.model.UsageSession
import com.foco.app.model.day.DayData
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

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
            val store =
                SessionHistoryStore(sessions, aggregates, markers, {
                    throw IllegalStateException("read failed")
                })
            val runner = ConsolidationRunner({ day -> store.consolidate(day) })

            val outcome = runner.run(today)

            assertTrue("a failed read returns Failed", outcome is ConsolidationOutcome.Failed)
            assertEquals("raw rows beyond the cutoff are preserved on failure", 1, sessions.rows.size)
            assertEquals("aggregates beyond the cutoff are preserved on failure", 1, aggregates.rows.size)
            assertEquals("markers beyond the cutoff are preserved on failure", 1, markers.rows.size)
        }

    @Test
    fun `a transaction failure in the store returns Failed and rolls back the raw write`() =
        runBlocking {
            val sessions = FakeSessionRecordDao()
            val aggregates = FakeDailyAggregateDao()
            val markers = FakeConsolidatedDayDao()
            val yesterday = today.minusDays(1)
            val dayData = DayData(listOf(UsageSession("app.a", 0L, 1000L)), emptyList())
            sessions.failNextWrite = true
            var rolledBack = false
            val store =
                SessionHistoryStore(
                    sessions,
                    aggregates,
                    markers,
                    { if (it == yesterday) dayData else DayData(emptyList(), emptyList()) },
                    transaction = { body ->
                        try {
                            body()
                        } catch (t: Throwable) {
                            rolledBack = true
                            throw t
                        }
                    },
                )
            val runner = ConsolidationRunner({ day -> store.consolidate(day) })

            val outcome = runner.run(today)

            assertTrue("a transaction failure is reported as Failed", outcome is ConsolidationOutcome.Failed)
            assertTrue("the transaction block is aborted on error", rolledBack)
            assertTrue("failed run stores no aggregates", aggregates.rows.isEmpty())
            assertTrue("failed run stores no marker", markers.rows.isEmpty())
        }
}
