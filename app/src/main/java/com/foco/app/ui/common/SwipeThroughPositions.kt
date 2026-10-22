package com.foco.app.ui.common

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/** The drag that has to travel before the screen moves to another position. */
private val SwipeThreshold = 64.dp

/**
 * The horizontal swipe that walks positions, the way the home's ring walks its days: left to
 * the next one, right to the previous one. The position moves with the finger.
 */
@Composable
fun Modifier.swipeThroughPositions(
    onNext: () -> Unit,
    onPrevious: () -> Unit,
): Modifier {
    val threshold = with(LocalDensity.current) { SwipeThreshold.toPx() }
    // The callbacks are read through a state, so the pointer input is not restarted when the
    // screen recomposes in the middle of a swipe.
    val next by rememberUpdatedState(onNext)
    val previous by rememberUpdatedState(onPrevious)
    return this.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val down = awaitPointerEvent(PointerEventPass.Initial)
                if (!down.changes.any { it.changedToDown() }) continue
                val startX = down.changes.first().position.x
                val startY = down.changes.first().position.y
                var totalDrag = 0f
                var decided = false
                while (!decided) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.changes.all { it.changedToUp() }) break
                    val change = event.changes.firstOrNull() ?: continue
                    val dx = change.position.x - startX
                    val dy = change.position.y - startY
                    if (kotlin.math.abs(dy) > kotlin.math.abs(dx) && kotlin.math.abs(dy) > 24f) {
                        // A vertical drag: the list scrolls, the walk waits.
                        break
                    }
                    totalDrag += dx
                    if (totalDrag <= -threshold) {
                        decided = true
                        change.consume()
                        next()
                    } else if (totalDrag >= threshold) {
                        decided = true
                        change.consume()
                        previous()
                    }
                }
            }
        }
    }
}

/**
 * A tap that never consumes the down event, so a parent gesture (a horizontal drag) can still
 * recognise it. A plain `clickable` would swallow the down and the drag would never start.
 */
fun Modifier.tapWithoutConsuming(onClick: () -> Unit): Modifier =
    pointerInput(onClick) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            val up = waitForUpOrCancellation()
            if (up != null) onClick()
        }
    }
