package com.foco.app.data

import com.foco.app.domain.ConsolidationOutcome

import android.content.Context
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The one door to `SessionHistoryStore.consolidate`, shared by the app start and the nightly job: a
 * trigger that finds a run in progress waits for that run instead of starting a second one.
 */
class ConsolidationRunner(
    private val consolidate: suspend (LocalDate) -> Unit,
    private val gate: Mutex = Mutex()
) {
    private var inFlight: Deferred<ConsolidationOutcome>? = null

    suspend fun run(today: LocalDate): ConsolidationOutcome = coroutineScope {
        val job = gate.withLock {
            inFlight ?: async(start = CoroutineStart.LAZY) {
                try {
                    consolidate(today)
                    ConsolidationOutcome.Done
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Throwable) {
                    ConsolidationOutcome.Failed(failure)
                } finally {
                    gate.withLock { inFlight = null }
                }
            }.also {
                inFlight = it
                it.start()
            }
        }
        job.await()
    }

    companion object {
        @Volatile
        private var shared: ConsolidationRunner? = null

        /** The process-wide runner over the app's store, reading the days from the platform. */
        fun forApp(context: Context): ConsolidationRunner = shared ?: synchronized(this) {
            shared ?: ConsolidationRunner(consolidate = { day ->
                val repository = UsageRepository(context.applicationContext)
                SessionHistoryStore.forApp(context.applicationContext) { date -> repository.loadDay(date) }
                    .consolidate(day)
            }).also { shared = it }
        }
    }
}
