package com.foco.app.data.stores

import com.foco.app.data.db.history.sessionrecord.SessionRecord
import com.foco.app.domain.history.finishedDays
import com.foco.app.model.UsageSession
import com.foco.app.model.day.DayData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class HistoryConsolidationTest {
    private val today = LocalDate.of(2026, 10, 19)
    private val yesterday = today.minusDays(1)
    private val platform = mutableMapOf<LocalDate, DayData>()
    private val sessions = FakeSessionRecordDao()
    private val aggregates = FakeDailyAggregateDao()
    private val markers = FakeConsolidatedDayDao()
    private val store =
        SessionHistoryStore(sessions, aggregates, markers, { platform[it] ?: emptyDay })

    @Test
    fun `consolidating a day writes its raw sessions, aggregates and marker`() =
        runBlocking {
            val session = UsageSession("app.a", rawStart(yesterday), rawStart(yesterday) + 3_600_000L)
            platform[yesterday] = DayData(listOf(session), listOf(rawStart(yesterday) + 1_000L))

            store.consolidate(today)

            assertEquals(
                listOf(SessionRecord("app.a", session.start, session.end)),
                sessions.rows.values.toList(),
            )
            assertEquals(3_600_000L, aggregates.rows["app.a" to yesterday.toString()]?.totalMillis)
            assertEquals(1, markers.rows[yesterday.toString()]?.count)
        }

    @Test
    fun `already consolidated days are not re-read from the platform`() =
        runBlocking {
            var reads = 0
            val countingStore =
                SessionHistoryStore(sessions, aggregates, markers, {
                    reads++
                    DayData(listOf(UsageSession("app.a", 0L, 1000L)), emptyList())
                })

            countingStore.consolidate(today)
            val firstReads = reads

            countingStore.consolidate(today)

            assertEquals("second run must not re-read already marked days", firstReads, reads)
        }

    @Test
    fun `a retried day replaces raw rows instead of appending them`() =
        runBlocking {
            val session1 = UsageSession("app.a", rawStart(yesterday), rawStart(yesterday) + 1_000L)
            platform[yesterday] = DayData(listOf(session1), emptyList())

            markers.failNextWrite = true
            runCatching { store.consolidate(today) }

            val session2 = UsageSession("app.a", rawStart(yesterday), rawStart(yesterday) + 2_000L)
            platform[yesterday] = DayData(listOf(session2), emptyList())

            store.consolidate(today)

            assertEquals(1, sessions.rows.size)
            assertEquals(
                session2.end,
                sessions.rows.values
                    .first()
                    .end,
            )
        }

    @Test
    fun `storedDaysFlow emits the set of consolidated dates`() =
        runBlocking {
            platform[yesterday] = DayData(listOf(UsageSession("app.a", 0L, 1000L)), emptyList())
            store.consolidate(today)

            val emitted =
                store
                    .storedDaysFlow()
                    .take(1)
                    .toList()
                    .single()

            assertEquals(setOf(yesterday), emitted)
        }

    @Test
    fun `storedDaysFlow can be read alongside concurrent writes without deadlock`() =
        runBlocking {
            val finished = finishedDays(today)
            finished.forEach { day ->
                platform[day] = DayData(listOf(UsageSession("app.a", rawStart(day), rawStart(day) + 1_000L)), emptyList())
            }

            val readJob =
                async(Dispatchers.Default) {
                    withTimeout(2_000) {
                        store.storedDaysFlow().first { days -> days.size == finished.size }
                    }
                }

            store.consolidate(today)
            readJob.await()
            assertTrue("stream collected all finished days", true)
        }

    private fun rawStart(day: LocalDate): Long = day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private companion object {
        val emptyDay = DayData(emptyList(), emptyList())
    }
}
