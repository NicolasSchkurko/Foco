package com.foco.app.ui.dashboard

import com.foco.app.domain.DayStop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DashboardStateTest {
    private val today = LocalDate.of(2026, 10, 20)
    private val yesterday = today.minusDays(1)
    private val ring = listOf(DayStop.Day(today), DayStop.Week, DayStop.Day(yesterday))

    @Test
    fun `moveTo pushes and goBack pops the previous stop`() {
        val state = DashboardState(DayStop.Day(today))

        assertTrue(state.moveTo(DayStop.Week, ring, isShortcut = false))
        assertEquals(DayStop.Week, state.value.ring.current)
        assertEquals(listOf(DayStop.Day(today)), state.value.ring.history)

        assertTrue(state.goBack())
        assertEquals(DayStop.Day(today), state.value.ring.current)
        assertTrue(
            state.value.ring.history
                .isEmpty(),
        )
    }

    @Test
    fun `a target outside the ring is rejected`() {
        val state = DashboardState(DayStop.Day(today))
        val outside = DayStop.Day(today.minusDays(4))

        assertFalse(state.moveTo(outside, ring, isShortcut = false))
        assertEquals(DayStop.Day(today), state.value.ring.current)
        assertTrue(
            state.value.ring.history
                .isEmpty(),
        )
    }

    @Test
    fun `a step sets the direction and the soft motion`() {
        val state = DashboardState(DayStop.Day(today))

        assertTrue(state.moveStep(ring, forward = true))
        assertEquals(DayStop.Week, state.value.ring.current)
        assertEquals(1, state.value.slideDirection)
        assertTrue(state.value.softMotion)

        assertTrue(state.moveStep(ring, forward = false))
        assertEquals(DayStop.Day(today), state.value.ring.current)
        assertEquals(-1, state.value.slideDirection)
        assertTrue(state.value.softMotion)
    }

    @Test
    fun `a shortcut day outside the ring can be opened and kept`() {
        val state = DashboardState(DayStop.Day(today))
        val shortcut = DayStop.Day(today.minusDays(2))

        assertTrue(state.moveTo(shortcut, ring, isShortcut = true))
        assertEquals(shortcut, state.value.ring.current)
        state.retain(ring, isAllowed = { it == shortcut }, fallback = DayStop.Day(today))
        assertEquals(shortcut, state.value.ring.current)
    }
}
