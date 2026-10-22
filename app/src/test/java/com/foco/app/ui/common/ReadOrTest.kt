package com.foco.app.ui.common

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

/** Cancellation travels through the read helpers unchanged; it never becomes the fallback value. */
class ReadOrTest {
    @Test
    fun cancellationMidStreamPropagates() {
        val stream: Flow<Int> = flow<Int> { throw CancellationException("collect cancelled") }.readOr(-1)

        val thrown = runCatching { runBlocking { stream.toList() } }.exceptionOrNull()

        assertTrue("cancellation must propagate instead of emitting -1", thrown is CancellationException)
    }

    @Test
    fun cancellationInASuspendReadPropagates() {
        val thrown =
            runCatching {
                runBlocking { readOr(-1) { throw CancellationException("read cancelled") } }
            }.exceptionOrNull()

        assertTrue("cancellation must propagate instead of answering -1", thrown is CancellationException)
    }
}
