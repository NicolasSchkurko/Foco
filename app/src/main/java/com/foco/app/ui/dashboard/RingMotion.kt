package com.foco.app.ui.dashboard

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import kotlin.math.roundToInt

/** The soft motion used by swipe and arrow moves. */
object RingMotion {
    const val SOFT_DURATION_MILLIS = 320
    const val SOFT_SLIDE_DIVISOR = 8

    val softEasing: Easing = FastOutSlowInEasing

    /** The 320 ms base scaled by the system animator duration scale, never zero. */
    fun durationMillis(scale: Float): Int =
        (SOFT_DURATION_MILLIS * scale).roundToInt().coerceAtLeast(1)
}
