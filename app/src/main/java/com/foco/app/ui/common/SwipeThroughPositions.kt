package com.foco.app.ui.common

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/** The drag that has to travel before the screen moves to another position. */
private val SwipeThreshold = 64.dp

/**
 * The horizontal swipe that walks positions the way the home's ring walks its days: left to the
 * next one, right to the previous one, as soon as the finger crosses the threshold.
 */
@Composable
fun Modifier.swipeThroughPositions(
    onNext: () -> Unit,
    onPrevious: () -> Unit,
): Modifier {
    val threshold = with(LocalDensity.current) { SwipeThreshold.toPx() }
    // The callbacks are read through a state, so a recomposition in the middle of the gesture
    // neither restarts the pointer input nor calls a stale walk.
    val next by rememberUpdatedState(onNext)
    val previous by rememberUpdatedState(onPrevious)
    return this.pointerInput(Unit) {
        var drag = 0f
        var moved = false
        detectHorizontalDragGestures(
            onDragStart = {
                drag = 0f
                moved = false
            },
            onHorizontalDrag = { change, amount ->
                // The drag belongs to the walk from here on: the list under the finger must not
                // also scroll sideways.
                change.consume()
                if (!moved) {
                    drag += amount
                    when {
                        drag <= -threshold -> {
                            moved = true
                            next()
                        }
                        drag >= threshold -> {
                            moved = true
                            previous()
                        }
                    }
                }
            },
            onDragEnd = {
                drag = 0f
                moved = false
            },
            onDragCancel = {
                drag = 0f
                moved = false
            },
        )
    }
}
