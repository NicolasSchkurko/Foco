package com.foco.app.domain

import com.foco.app.domain.history.dayWindow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * The day's window: the four timestamps the query asks the system for, pinned here.
 * A zone without daylight saving, so "one day" is exactly 24 h.
 */
class DayWindowTest {
    private val zone = ZoneId.of("America/Argentina/Buenos_Aires")
    private val date = LocalDate.of(2026, 10, 18)

    private fun at(
        day: LocalDate,
        hour: Int = 0,
        minute: Int = 0,
    ): Long = day.atStartOfDay(zone).toInstant().toEpochMilli() + hour * HOUR + minute * MINUTE

    @Test
    fun `the day boundaries are the requested date in the zone`() {
        val window = dayWindow(date, zone, now = at(date, hour = 12))

        assertEquals(at(date), window.startOfDay)
        assertEquals(at(date.plusDays(1)), window.dayEndExclusive)
    }

    @Test
    fun `the effective end is the clock while the day runs and the day end once it is past`() {
        val running = dayWindow(date, zone, now = at(date, hour = 12))
        val past = dayWindow(date, zone, now = at(date.plusDays(3)))

        assertEquals(at(date, hour = 12), running.effectiveEnd)
        assertEquals(at(date.plusDays(1)), past.effectiveEnd)
    }

    @Test
    fun `the lookback is exactly one day before the day start`() {
        val window = dayWindow(date, zone, now = at(date, hour = 12))

        assertEquals(at(date.minusDays(1)), window.lookbackStart)
        assertEquals(
            window.dayEndExclusive - window.startOfDay,
            window.startOfDay - window.lookbackStart,
        )
    }

    @Test
    fun `the window of a session open across midnight covers the opening event and the stop`() {
        val stop = at(date, minute = 6)
        val window = dayWindow(date, zone, now = stop)

        assertTrue("the query looks before the start of the day", window.lookbackStart < window.startOfDay)
        assertTrue("today's close is inside the window", window.effectiveEnd >= stop)
    }
}

private const val MINUTE = 60_000L
private const val HOUR = 60 * MINUTE
