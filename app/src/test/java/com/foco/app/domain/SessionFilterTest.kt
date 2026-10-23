package com.foco.app.domain

import com.foco.app.domain.sessions.filterSessions
import com.foco.app.model.UsageSession
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionFilterTest {
    private val session = UsageSession("app.a", 0L, 60_000L)

    @Test
    fun `non-launchable packages are dropped`() {
        val kept =
            filterSessions(
                sessions =
                    listOf(
                        session,
                        UsageSession("com.internal.service", 100L, 200L),
                    ),
                allowedPackages = setOf("app.a"),
                excludedPackages = emptySet(),
            )

        assertEquals(listOf(session), kept)
    }

    @Test
    fun `home and self are dropped`() {
        val home = UsageSession("com.android.launcher", 10L, 20L)
        val self = UsageSession("com.foco.app", 30L, 40L)

        val kept =
            filterSessions(
                sessions = listOf(session, home, self),
                allowedPackages = setOf("app.a", "com.android.launcher", "com.foco.app"),
                excludedPackages = setOf("com.android.launcher", "com.foco.app"),
            )

        assertEquals(listOf(session), kept)
    }
}
