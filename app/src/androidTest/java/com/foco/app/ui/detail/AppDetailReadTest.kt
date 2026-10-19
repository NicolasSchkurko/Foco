package com.foco.app.ui.detail

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.foco.app.model.UsageSession
import com.foco.app.ui.common.readOr
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** The screen's degraded read on device: it answers the fallback, and cancellation still propagates. */
@RunWith(AndroidJUnit4::class)
class AppDetailReadTest {

    @Test
    fun aFailedReadIsAnsweredWithTheFallback() = runBlocking {
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
}
