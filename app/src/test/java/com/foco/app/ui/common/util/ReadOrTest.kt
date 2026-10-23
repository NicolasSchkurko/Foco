package com.foco.app.ui.common.util

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ReadOrTest {
    @Test
    fun `a successful read answers the value`() =
        runBlocking {
            assertEquals("ok", readOr("fallback") { "ok" })
        }

    @Test
    fun `a failed read answers the fallback`() =
        runBlocking {
            val result =
                readOr("fallback") {
                    throw IllegalStateException("boom")
                }
            assertEquals("fallback", result)
        }

    @Test
    fun `a failed stream answers the fallback`() =
        runBlocking {
            val stream =
                flow<String> {
                    throw IllegalStateException("boom")
                }.readOr("fallback")

            assertEquals(listOf("fallback"), stream.toList())
        }

    @Test
    fun `cancellation is not swallowed by the fallback`() =
        runBlocking {
            val started = CompletableDeferred<Unit>()
            var cancelled = false

            val job =
                launch {
                    try {
                        readOr("fallback") {
                            started.complete(Unit)
                            kotlinx.coroutines.awaitCancellation()
                        }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        cancelled = true
                        throw e
                    }
                }

            started.await()
            job.cancel()
            job.join()

            assertEquals(true, cancelled)
        }
}
