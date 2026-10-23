package com.foco.app.domain

import com.foco.app.domain.ring.stepCyclicIndex
import org.junit.Assert.assertEquals
import org.junit.Test

/** The cyclic walk of the day's positions: forward, back and around the ends. */
class StepCyclicIndexTest {
    @Test
    fun `a step forward moves one position`() {
        assertEquals(1, stepCyclicIndex(0, 3, 1))
        assertEquals(2, stepCyclicIndex(1, 3, 1))
    }

    @Test
    fun `a step back moves one position`() {
        assertEquals(1, stepCyclicIndex(2, 3, -1))
        assertEquals(0, stepCyclicIndex(1, 3, -1))
    }

    @Test
    fun `the walk cycles at the ends`() {
        // Past the last one it starts again.
        assertEquals(0, stepCyclicIndex(2, 3, 1))
        // Before the first one it ends at the last.
        assertEquals(2, stepCyclicIndex(0, 3, -1))
    }

    @Test
    fun `an empty list stays at zero`() {
        assertEquals(0, stepCyclicIndex(0, 0, 1))
        assertEquals(0, stepCyclicIndex(3, 0, -1))
    }
}
