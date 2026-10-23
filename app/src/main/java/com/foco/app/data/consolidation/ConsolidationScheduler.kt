package com.foco.app.data.consolidation

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Enqueues the one nightly consolidation: 24 h periodic, first run at the next 03:00 local, kept
 * across starts. Resource: the WorkManager database, in the app's private storage.
 */
object ConsolidationScheduler {
    private const val UNIQUE_NAME = "consolidate-and-prune"

    fun schedule(
        context: Context,
        now: ZonedDateTime = ZonedDateTime.now(),
    ) {
        val request =
            PeriodicWorkRequestBuilder<ConsolidationWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delayToNextThree(now))
                .build()
        WorkManager
            .getInstance(context)
            .enqueueUniquePeriodicWork(UNIQUE_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** The wait from [now] to the next 03:00 in its zone; being exactly at 03:00 means tomorrow. */
    fun delayToNextThree(now: ZonedDateTime): Duration {
        val today = now.toLocalDate().atTime(3, 0).atZone(now.zone)
        val next = if (today.isAfter(now)) today else today.plusDays(1)
        return Duration.between(now, next)
    }
}
