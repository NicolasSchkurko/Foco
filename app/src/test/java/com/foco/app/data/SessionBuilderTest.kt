package com.foco.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val MINUTE = 60_000L
private const val HOUR = 60 * MINUTE

class SessionBuilderTest {

    private val base = 10 * HOUR

    @Test
    fun `direct switch between apps closes the previous session at the new start`() {
        val sessions = buildSessions(
            listOf(
                UsageEvent("app.a", base, UsageEventType.RESUMED),
                UsageEvent("app.b", base + 5 * MINUTE, UsageEventType.RESUMED)
            ),
            now = base + 30 * MINUTE
        )

        assertEquals(
            listOf(
                UsageSession("app.a", base, base + 5 * MINUTE),
                UsageSession("app.b", base + 5 * MINUTE, base + 30 * MINUTE)
            ),
            sessions
        )
    }

    @Test
    fun `session open at query time ends at now`() {
        val sessions = buildSessions(
            listOf(UsageEvent("app.a", base, UsageEventType.RESUMED)),
            now = base + 12 * MINUTE
        )

        assertEquals(listOf(UsageSession("app.a", base, base + 12 * MINUTE)), sessions)
    }

    @Test
    fun `stop closes the open session and a later resume opens a new one`() {
        val sessions = buildSessions(
            listOf(
                UsageEvent("app.a", base, UsageEventType.RESUMED),
                UsageEvent("app.a", base + 3 * MINUTE, UsageEventType.STOPPED),
                UsageEvent("app.a", base + 10 * MINUTE, UsageEventType.RESUMED)
            ),
            now = base + 15 * MINUTE
        )

        assertEquals(
            listOf(
                UsageSession("app.a", base, base + 3 * MINUTE),
                UsageSession("app.a", base + 10 * MINUTE, base + 15 * MINUTE)
            ),
            sessions
        )
    }

    @Test
    fun `transitions inside the same app do not split the session`() {
        val sessions = buildSessions(
            listOf(
                UsageEvent("app.a", base, UsageEventType.RESUMED),
                // Cambio interno de activity dentro de la misma app.
                UsageEvent("app.a", base + 4 * MINUTE, UsageEventType.RESUMED),
                UsageEvent("app.a", base + 8 * MINUTE, UsageEventType.STOPPED)
            ),
            now = base + 20 * MINUTE
        )

        assertEquals(listOf(UsageSession("app.a", base, base + 8 * MINUTE)), sessions)
    }

    @Test
    fun `no activity yields no sessions`() {
        assertTrue(buildSessions(events = emptyList(), now = base).isEmpty())
    }
}
