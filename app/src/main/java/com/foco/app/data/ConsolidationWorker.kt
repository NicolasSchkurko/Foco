package com.foco.app.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.foco.app.domain.ConsolidationOutcome
import java.time.LocalDate

/** The nightly job: consolidates through the shared runner and asks for a retry when it fails. */
class ConsolidationWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result =
        when (runnerFor(applicationContext).run(LocalDate.now())) {
            is ConsolidationOutcome.Done -> Result.success()
            is ConsolidationOutcome.Failed -> Result.retry()
        }

    companion object {
        /** The device test replaces this with a runner over a seeded store. */
        @Volatile
        var runnerFor: (Context) -> ConsolidationRunner = { ConsolidationRunner.forApp(it) }
    }
}
