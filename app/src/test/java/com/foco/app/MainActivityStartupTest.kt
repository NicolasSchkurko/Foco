package com.foco.app

import com.foco.app.domain.consolidation.withStartupTimeout
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class MainActivityStartupTest {
    @Test
    fun `a slow startup returns the fallback`() =
        runBlocking {
            val result =
                withStartupTimeout(10, { "fallback" }) {
                    delay(100)
                    "loaded"
                }

            assertEquals("fallback", result)
        }

    @Test
    fun `a fast startup returns the loaded value`() =
        runBlocking {
            val result =
                withStartupTimeout(10_000, { "fallback" }) {
                    "loaded"
                }

            assertEquals("loaded", result)
        }

    @Test
    fun `a startup failure returns the fallback`() =
        runBlocking {
            val result =
                withStartupTimeout<String>(10_000, { "fallback" }) {
                    error("settings unavailable")
                }

            assertEquals("fallback", result)
        }
}
