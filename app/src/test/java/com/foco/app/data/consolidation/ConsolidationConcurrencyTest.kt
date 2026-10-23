package com.foco.app.data.consolidation

import com.foco.app.domain.consolidation.ConsolidationOutcome
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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

            delay(50)
            store.release.complete(Unit)

            assertEquals(ConsolidationOutcome.Done, first.await())
            assertEquals(ConsolidationOutcome.Done, second.await())
            assertEquals("a single underlying run answers both callers", 1, store.runs)
        }

    @Test
    fun `a cancelled caller does not cancel the shared run for the other`() =
        runBlocking {
            val store = SlowStore()
            val runner = ConsolidationRunner({ day -> store.consolidate(day) })

            val first = launch(Dispatchers.Default) { runner.run(today) }
            store.started.await()
            val second = async(Dispatchers.Default) { runner.run(today) }

            first.cancelAndJoin()
            store.release.complete(Unit)

            assertEquals(ConsolidationOutcome.Done, second.await())
            assertEquals("the underlying run completes once", 1, store.runs)
        }

    private class SlowStore {
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var runs = 0

        suspend fun consolidate(day: LocalDate) {
            runs++
            started.complete(Unit)
            release.await()
        }
    }
}
