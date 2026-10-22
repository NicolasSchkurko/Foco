package com.foco.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DayRingTest {
    private val today = LocalDate.of(2026, 10, 19)
    private val oldest = today.minusDays(3)
    private val yesterday = today.minusDays(1)
    private val ring = dayRing(today, setOf(oldest, today.minusDays(2), yesterday))

    @Test
    fun `the ring runs today week oldest then the finished days`() {
        assertEquals(
            listOf(
                DayStop.Day(today),
                DayStop.Week,
                DayStop.Day(oldest),
                DayStop.Day(today.minusDays(2)),
                DayStop.Day(yesterday),
            ),
            ring,
        )
    }

    @Test
    fun `a marker stored for today yields a single today position`() {
        val ringWithTodayMarker = dayRing(today, setOf(oldest, today.minusDays(2), yesterday, today))

        assertEquals(1, ringWithTodayMarker.count { it == DayStop.Day(today) })
        assertEquals(DayStop.Week, DayStop.Day(today).step(ringWithTodayMarker, forward = true))
    }

    @Test
    fun `forward from today is the week and forward from the week is the oldest day`() {
        assertEquals(DayStop.Week, DayStop.Day(today).step(ring, forward = true))
        assertEquals(DayStop.Day(oldest), DayStop.Week.step(ring, forward = true))
    }

    @Test
    fun `back from the oldest day is the week and forward from yesterday wraps to today`() {
        assertEquals(DayStop.Week, DayStop.Day(oldest).step(ring, forward = false))
        assertEquals(DayStop.Day(today), DayStop.Day(yesterday).step(ring, forward = true))
    }

    @Test
    fun `a stored day outside the seven-day window is kept but not offered`() {
        val guard = today.minusDays(7)
        val capped = dayRing(today, setOf(guard, today.minusDays(6), yesterday))

        assertEquals(
            listOf(
                DayStop.Day(today),
                DayStop.Week,
                DayStop.Day(today.minusDays(6)),
                DayStop.Day(yesterday),
            ),
            capped,
        )
        assertEquals(DayStop.Day(today.minusDays(6)), DayStop.Week.step(capped, forward = true))
    }

    @Test
    fun `an empty store is today and the week, so the week wraps to today`() {
        val empty = dayRing(today, emptySet())

        assertEquals(listOf(DayStop.Day(today), DayStop.Week), empty)
        assertEquals(DayStop.Day(today), DayStop.Week.step(empty, forward = true))
        assertEquals(DayStop.Week, DayStop.Day(today).step(empty, forward = false))
    }

    @Test
    fun `a step pushes the previous position and back pops it`() {
        val week = DayRingState(DayStop.Day(today)).step(ring, forward = true)
        val oldestState = week.step(ring, forward = true)

        assertEquals(DayStop.Week, week.current)
        assertEquals(listOf(DayStop.Day(today)), week.history)
        assertEquals(DayStop.Day(oldest), oldestState.current)
        assertEquals(listOf(DayStop.Day(today), DayStop.Week), oldestState.history)

        val backToWeek = oldestState.back()
        assertNotNull(backToWeek)
        assertEquals(DayStop.Week, backToWeek!!.current)
        assertEquals(listOf(DayStop.Day(today)), backToWeek.history)

        val backToToday = backToWeek.back()
        assertNotNull(backToToday)
        assertEquals(DayStop.Day(today), backToToday!!.current)
        assertNull(backToToday.back())
    }

    @Test
    fun `a bar tap pushes like a step`() {
        val fromWeek = DayRingState(DayStop.Week).moveTo(DayStop.Day(oldest), ring)

        assertEquals(DayStop.Day(oldest), fromWeek.current)
        assertEquals(listOf(DayStop.Week), fromWeek.history)
        assertEquals(DayStop.Week, fromWeek.back()!!.current)
    }

    @Test
    fun `a vanished current position falls back to today and leaves the valid history`() {
        val vanished =
            DayRingState(
                current = DayStop.Day(oldest),
                history = listOf(DayStop.Week, DayStop.Day(oldest)),
            ).retain(dayRing(today, emptySet()), { false }, DayStop.Day(today))

        assertEquals(DayStop.Day(today), vanished.current)
        assertEquals(listOf(DayStop.Week), vanished.history)
    }

    @Test
    fun `keys round trip a day and the week`() {
        val day = DayStop.Day(today)
        assertEquals(day, dayStopFromKey(day.key()))
        assertEquals(DayStop.Week, dayStopFromKey(DayStop.Week.key()))
        assertNull(dayStopFromKey("not-a-stop"))
    }
}
