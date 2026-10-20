package com.foco.app.ui.dashboard

import androidx.compose.animation.core.FastOutSlowInEasing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class RingMotionTest {

    @Test
    fun `the soft spec is fixed at 320 ms and an eighth of the width`() {
        assertEquals(320, RingMotion.SOFT_DURATION_MILLIS)
        assertEquals(8, RingMotion.SOFT_SLIDE_DIVISOR)
        assertSame(FastOutSlowInEasing, RingMotion.softEasing)
    }

    @Test
    fun `the system scale multiplies the base duration`() {
        assertEquals(320, RingMotion.durationMillis(1f))
        assertEquals(160, RingMotion.durationMillis(0.5f))
        assertEquals(1, RingMotion.durationMillis(0f))
    }
}
