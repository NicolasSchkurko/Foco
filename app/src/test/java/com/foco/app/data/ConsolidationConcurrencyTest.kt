package com.foco.app.data

import com.foco.app.domain.ConsolidationOutcome
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ConsolidationConcurrencyTest {
    private val today = LocalDate.of(2026, 10, 19)

    @Test
    fun `two overlapping triggers run the store once`() =
        runBlocking {
            val store = SlowStore()
            val runner = ConsolidationRunner({ day -> store.consolidate(day) })

            val first = async(Dispatchers.Default) { runner.run(today) }
            store.started.await()
            val second = async(Dispatchers.Default) { runner.run(today) }

            assertEquals(ConsolidationOutcome.Done, first.await())
            assertEquals(ConsolidationOutcome.Done, second.await())
            assertEquals("the store must be touched once", 1, store.runs)
        }

    @Test
    fun `cancelling the startup wait does not cancel the shared run`() =
        runBlocking {
            val store = SlowStore()
            val runner = ConsolidationRunner({ day -> store.consolidate(day) })

            val startup = launch(Dispatchers.Default) { runner.run(today) }
            store.started.await()
            val worker = async(Dispatchers.Default) { runner.run(today) }
            startup.cancelAndJoin()

            assertEquals(ConsolidationOutcome.Done, worker.await())
            assertEquals("the store must still run once", 1, store.runs)
        }

    @Test
    fun `a run after the previous one finished stores no day twice`() =
        runBlocking {
            val store = SlowStore(delayMillis = 0)
            val runner = ConsolidationRunner({ day -> store.consolidate(day) })

            runner.run(today)
            runner.run(today)

            assertEquals(2, store.runs)
            assertEquals(setOf(today.minusDays(1)), store.days)
        }

    @Test
    fun `a cancelled underlying run is reported as Failed`() =
        runBlocking {
            val runner = ConsolidationRunner({ _: LocalDate -> throw CancellationException("cancelled") })

            val outcome = runner.run(today)

            assertTrue(outcome is ConsolidationOutcome.Failed)
        }

    /** Records how many times it was called and how many distinct days it stored. */
    private class SlowStore(
        private val delayMillis: Long = 200,
    ) {
        val started = CompletableDeferred<Unit>()
        val days = mutableSetOf<LocalDate>()

        @Volatile
        var runs = 0

        suspend fun consolidate(day: LocalDate) {
            runs++
            days += day.minusDays(1)
            started.complete(Unit)
            if (delayMillis > 0) delay(delayMillis)
        }
    }
}
