package com.foco.app.domain

import com.foco.app.domain.history.DayWindow
import com.foco.app.domain.sessions.buildSessions
import com.foco.app.domain.sessions.clampSessionsToDay
import com.foco.app.model.UsageEvent
import com.foco.app.model.UsageEventType
import com.foco.app.model.UsageSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val MINUTE = 60_000L
private const val HOUR = 60 * MINUTE

class SessionBuilderTest {
    private val base = 10 * HOUR

    /** The window of the day that starts at [dayStart]. */
    private fun dayWindow(dayStart: Long): DayWindow =
        DayWindow(
            startOfDay = dayStart,
            dayEndExclusive = dayStart + 24 * HOUR,
            lookbackStart = dayStart - 24 * HOUR,
            effectiveEnd = dayStart + 24 * HOUR,
        )

    @Test
    fun `direct switch between apps closes the previous session at the new start`() {
        val sessions =
            buildSessions(
                listOf(
                    UsageEvent("app.a", base, UsageEventType.RESUMED),
                    UsageEvent("app.b", base + 5 * MINUTE, UsageEventType.RESUMED),
                ),
                now = base + 30 * MINUTE,
            )

        assertEquals(
            listOf(
                UsageSession("app.a", base, base + 5 * MINUTE),
                UsageSession("app.b", base + 5 * MINUTE, base + 30 * MINUTE),
            ),
            sessions,
        )
    }

    @Test
    fun `session open at query time ends at now`() {
        val sessions =
            buildSessions(
                listOf(UsageEvent("app.a", base, UsageEventType.RESUMED)),
                now = base + 12 * MINUTE,
            )

        assertEquals(listOf(UsageSession("app.a", base, base + 12 * MINUTE)), sessions)
    }

    @Test
    fun `stop closes the open session and a later resume opens a new one`() {
        val sessions =
            buildSessions(
                listOf(
                    UsageEvent("app.a", base, UsageEventType.RESUMED),
                    UsageEvent("app.a", base + 3 * MINUTE, UsageEventType.STOPPED),
                    UsageEvent("app.a", base + 10 * MINUTE, UsageEventType.RESUMED),
                ),
                now = base + 15 * MINUTE,
            )

        assertEquals(
            listOf(
                UsageSession("app.a", base, base + 3 * MINUTE),
                UsageSession("app.a", base + 10 * MINUTE, base + 15 * MINUTE),
            ),
            sessions,
        )
    }

    @Test
    fun `transitions inside the same app do not split the session`() {
        val sessions =
            buildSessions(
                listOf(
                    UsageEvent("app.a", base, UsageEventType.RESUMED),
                    // Internal activity change inside the same app.
                    UsageEvent("app.a", base + 4 * MINUTE, UsageEventType.RESUMED),
                    UsageEvent("app.a", base + 8 * MINUTE, UsageEventType.STOPPED),
                ),
                now = base + 20 * MINUTE,
            )

        assertEquals(listOf(UsageSession("app.a", base, base + 8 * MINUTE)), sessions)
    }

    @Test
    fun `no activity yields no sessions`() {
        assertTrue(buildSessions(events = emptyList(), now = base).isEmpty())
    }

    @Test
    fun `an app already open at the day's start counts from the day's start`() {
        val dayStart = 24 * HOUR
        val resumedYesterday = dayStart - 10 * MINUTE
        val folded =
            buildSessions(
                listOf(
                    UsageEvent("app.a", resumedYesterday, UsageEventType.RESUMED),
                    UsageEvent("app.a", dayStart + 6 * MINUTE, UsageEventType.STOPPED),
                ),
                now = dayStart + 6 * MINUTE,
            )

        assertEquals(
            listOf(UsageSession("app.a", resumedYesterday, dayStart + 6 * MINUTE)),
            folded,
        )
        assertEquals(
            listOf(UsageSession("app.a", dayStart, dayStart + 6 * MINUTE)),
            clampSessionsToDay(folded, dayWindow(dayStart)),
        )
    }

    @Test
    fun `an app still open across midnight ends at the query time`() {
        val dayStart = 24 * HOUR
        val folded =
            buildSessions(
                listOf(UsageEvent("app.a", dayStart - 10 * MINUTE, UsageEventType.RESUMED)),
                now = dayStart + 6 * MINUTE,
            )

        assertEquals(
            listOf(UsageSession("app.a", dayStart, dayStart + 6 * MINUTE)),
            clampSessionsToDay(folded, dayWindow(dayStart)),
        )
    }

    @Test
    fun `the previous day keeps its own slice and the two do not overlap`() {
        val dayStart = 24 * HOUR
        val resumedYesterday = dayStart - 10 * MINUTE
        val stoppedToday = dayStart + 6 * MINUTE
        val events =
            listOf(
                UsageEvent("app.a", resumedYesterday, UsageEventType.RESUMED),
                UsageEvent("app.a", stoppedToday, UsageEventType.STOPPED),
            )
        val yesterday =
            clampSessionsToDay(
                buildSessions(events, now = dayStart),
                dayWindow(dayStart - 24 * HOUR),
            )
        val today =
            clampSessionsToDay(
                buildSessions(events, now = stoppedToday),
                dayWindow(dayStart),
            )

        assertEquals(listOf(UsageSession("app.a", resumedYesterday, dayStart)), yesterday)
        assertEquals(listOf(UsageSession("app.a", dayStart, stoppedToday)), today)
        assertEquals(yesterday.single().end, today.single().start)
    }

    @Test
    fun `sessions from the lookback and a stop without an open session are dropped`() {
        val dayStart = 24 * HOUR
        val olderThanTheDay = listOf(UsageSession("app.a", dayStart - 5 * HOUR, dayStart - 4 * HOUR))
        val orphanStop =
            buildSessions(
                listOf(UsageEvent("app.a", dayStart + HOUR, UsageEventType.STOPPED)),
                now = dayStart + 2 * HOUR,
            )

        assertTrue(clampSessionsToDay(olderThanTheDay, dayWindow(dayStart)).isEmpty())
        assertTrue(orphanStop.isEmpty())
        assertTrue(clampSessionsToDay(orphanStop, dayWindow(dayStart)).isEmpty())
    }

    @Test
    fun `a session inside the day is untouched by the clamp`() {
        val dayStart = 24 * HOUR
        val inside =
            buildSessions(
                listOf(
                    UsageEvent("app.a", dayStart + HOUR, UsageEventType.RESUMED),
                    UsageEvent("app.a", dayStart + 2 * HOUR, UsageEventType.STOPPED),
                ),
                now = dayStart + 3 * HOUR,
            )

        assertEquals(inside, clampSessionsToDay(inside, dayWindow(dayStart)))
    }

    @Test
    fun `an event older than the open session is ignored`() {
        val sessions =
            buildSessions(
                listOf(
                    UsageEvent("app.a", base, UsageEventType.RESUMED),
                    // Out of order: older than the open session, so it opens nothing.
                    UsageEvent("app.b", base - 5 * MINUTE, UsageEventType.RESUMED),
                ),
                now = base + 30 * MINUTE,
            )

        assertEquals(listOf(UsageSession("app.a", base, base + 30 * MINUTE)), sessions)
    }

    @Test
    fun `a direct switch in the same millisecond closes the previous session at that instant`() {
        val sessions =
            buildSessions(
                listOf(
                    UsageEvent("app.a", base, UsageEventType.RESUMED),
                    UsageEvent("app.b", base, UsageEventType.RESUMED),
                ),
                now = base + 30 * MINUTE,
            )

        // A contributes no time (zero-length session, not emitted) and B stays open until "now".
        assertEquals(listOf(UsageSession("app.b", base, base + 30 * MINUTE)), sessions)
    }

    @Test
    fun `a stop in the same millisecond as the resume emits no session and leaves nothing open`() {
        val sessions =
            buildSessions(
                listOf(
                    UsageEvent("app.a", base, UsageEventType.RESUMED),
                    UsageEvent("app.a", base, UsageEventType.STOPPED),
                ),
                now = base + 30 * MINUTE,
            )

        assertTrue("a zero-length session is not emitted", sessions.isEmpty())
    }

    @Test
    fun `a resume and a stop in the same millisecond leave the next package open`() {
        val sessions =
            buildSessions(
                listOf(
                    UsageEvent("app.a", base, UsageEventType.RESUMED),
                    UsageEvent("app.a", base, UsageEventType.STOPPED),
                    UsageEvent("app.b", base, UsageEventType.RESUMED),
                ),
                now = base + 5 * MINUTE,
            )

        assertEquals(listOf(UsageSession("app.b", base, base + 5 * MINUTE)), sessions)
    }
}
