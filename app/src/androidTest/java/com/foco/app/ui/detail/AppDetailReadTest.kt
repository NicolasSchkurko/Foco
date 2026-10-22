package com.foco.app.ui.detail

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.foco.app.model.UsageSession
import com.foco.app.ui.common.readOr
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.time.LocalDate

/** The screen's degraded read on device: it answers the fallback, and cancellation still propagates. */
@RunWith(AndroidJUnit4::class)
class AppDetailReadTest {
    @Test
    fun aFailedReadIsAnsweredWithTheFallback() =
        runBlocking {
            val storeBusy = IOException("the store is busy")

            assertEquals(emptySet<LocalDate>(), readOr(emptySet<LocalDate>()) { throw storeBusy })
            assertEquals(emptyList<UsageSession>(), readOr(emptyList<UsageSession>()) { throw storeBusy })
        }

    @Test
    fun cancellationStillPropagates() {
        var propagated = false

        try {
            runBlocking { readOr(0) { throw CancellationException("the screen is gone") } }
        } catch (cancelled: CancellationException) {
            propagated = true
        }

        assertTrue("cancellation must not be swallowed", propagated)
    }

    @Test
    fun aFailedStreamIsAnsweredWithTheFallback() =
        runBlocking {
            val storeBusy = IOException("the store is busy")
            val failing =
                flow {
                    emit(setOf(LocalDate.of(2026, 10, 18)))
                    throw storeBusy
                }

            assertEquals(
                listOf(setOf(LocalDate.of(2026, 10, 18)), emptySet<LocalDate>()),
                failing.readOr(emptySet<LocalDate>()).toList(),
            )
        }

    /** Regression: CancellationException thrown from the upstream flow propagates, not swallowed as a fallback. */
    @Test
    fun cancellationExceptionInFlowPropagates() {
        var propagated = false
        val failing =
            flow<Set<LocalDate>> {
                emit(setOf(LocalDate.of(2026, 10, 18)))
                throw CancellationException("cancelled")
            }
        try {
            runBlocking {
                failing.readOr(emptySet<LocalDate>()).toList()
            }
        } catch (cancelled: CancellationException) {
            propagated = true
        }
        assertTrue("CancellationException must propagate through Flow.readOr", propagated)
    }

    @Test
    fun aCancelledCollectionIsNotAnsweredWithTheFallback() =
        runBlocking {
            val seen = mutableListOf<Set<LocalDate>>()
            val first = CompletableDeferred<Unit>()
            val ticker =
                flow {
                    emit(setOf(LocalDate.of(2026, 10, 18)))
                    awaitCancellation()
                }

            val job =
                launch {
                    ticker.readOr(emptySet<LocalDate>()).collect {
                        seen += it
                        first.complete(Unit)
                    }
                }
            first.await()
            job.cancelAndJoin()

            assertEquals(
                "a cancelled collection must not be answered with the fallback",
                listOf(setOf(LocalDate.of(2026, 10, 18))),
                seen,
            )
        }
}
