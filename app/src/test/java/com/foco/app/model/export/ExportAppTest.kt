package com.foco.app.model.export

import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportAppTest {
    private val session =
        ExportSession(
            start = "2026-10-19T09:00:00+02:00",
            end = "2026-10-19T09:30:00+02:00",
            durationMillis = 1_800_000L,
        )

    @Test
    fun `the sessions flag must match the sessions the app carries`() {
        assertThrows(IllegalArgumentException::class.java) {
            ExportApp("app.a", "App A", 1_800_000L, sessionsAvailable = false, sessions = listOf(session))
        }
        assertThrows(IllegalArgumentException::class.java) {
            ExportApp("app.a", "App A", 0L, sessionsAvailable = true, sessions = emptyList())
        }
    }

    @Test
    fun `an app whose flag matches its sessions is accepted`() {
        val withSessions =
            ExportApp("app.a", "App A", 1_800_000L, sessionsAvailable = true, sessions = listOf(session))
        val withoutSessions =
            ExportApp("app.a", "App A", 0L, sessionsAvailable = false, sessions = emptyList())

        assertTrue(withSessions.sessionsAvailable)
        assertFalse(withoutSessions.sessionsAvailable)
    }
}
