package com.foco.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Category: background job (API 36). Builds the real worker with `work-testing` and points it at a
 * runner over a lambda, so the run and the retry are proven without touching the platform.
 */
@RunWith(AndroidJUnit4::class)
class ConsolidationWorkerTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @After
    fun restoreRunner() {
        ConsolidationWorker.runnerFor = { ConsolidationRunner.forApp(it) }
    }

    @Test
    fun aRunConsolidatesTheFinishedDaysAndSucceeds() =
        runBlocking {
            val days = mutableListOf<LocalDate>()
            ConsolidationWorker.runnerFor = { ConsolidationRunner({ day -> days += day }) }

            val result = TestListenableWorkerBuilder<ConsolidationWorker>(context).build().doWork()

            assertEquals(ListenableWorker.Result.success(), result)
            assertEquals(1, days.size)
        }

    @Test
    fun aFailingRunAsksForARetry() =
        runBlocking {
            ConsolidationWorker.runnerFor = {
                ConsolidationRunner({ throw IllegalStateException("platform busy") })
            }

            val result = TestListenableWorkerBuilder<ConsolidationWorker>(context).build().doWork()

            assertEquals(ListenableWorker.Result.retry(), result)
        }
}
