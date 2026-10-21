package com.foco.app.data

import android.content.Context
import com.foco.app.domain.ConsolidationOutcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

/**
 * The one door to `SessionHistoryStore.consolidate`, shared by the app start and the nightly job:
 * a waiting trigger joins the run, whose own scope keeps it alive when a caller is cancelled.
 */
class ConsolidationRunner(
    private val consolidate: suspend (LocalDate) -> Unit,
    private val gate: Mutex = Mutex(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private var inFlight: Deferred<ConsolidationOutcome>? = null

    suspend fun run(today: LocalDate): ConsolidationOutcome {
        val job =
            gate.withLock {
                inFlight ?: scope
                    .async {
                        try {
                            consolidate(today)
                            ConsolidationOutcome.Done
                        } catch (cancelled: CancellationException) {
                            // A cancelled underlying run is reported as Failed so the worker retries
                            // instead of propagating the cancellation (see spec delta).
                            ConsolidationOutcome.Failed(cancelled)
                        } catch (failure: Throwable) {
                            ConsolidationOutcome.Failed(failure)
                        } finally {
                            gate.withLock { inFlight = null }
                        }
                    }.also { inFlight = it }
            }
        return job.await()
    }

    companion object {
        @Volatile
        private var shared: ConsolidationRunner? = null

        /** The process-wide runner over the app's store, reading the days from the platform. */
        fun forApp(context: Context): ConsolidationRunner =
            shared ?: synchronized(this) {
                shared ?: ConsolidationRunner(consolidate = { day ->
                    val repository = UsageRepository(context.applicationContext)
                    SessionHistoryStore
                        .forApp(context.applicationContext) { date -> repository.loadDay(date) }
                        .consolidate(day)
                }).also { shared = it }
            }
    }
}
