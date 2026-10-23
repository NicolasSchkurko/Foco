package com.foco.app.ui.common.ring

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset

/**
 * Reusable container that handles horizontal swipe gestures and soft animated horizontal slide
 * transitions for ring positions across screens (e.g. Dashboard and App Detail).
 */
@Composable
fun <T> PositionRingContainer(
    targetState: T,
    slideDirection: Int,
    softMotion: Boolean,
    motionScale: Float,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "position-ring",
    content: @Composable (T) -> Unit,
) {
    Box(
        modifier =
            modifier
                .swipeThroughPositions(
                    onNext = onNext,
                    onPrevious = onPrevious,
                ),
    ) {
        AnimatedContent(
            targetState = targetState,
            transitionSpec = {
                val forward = slideDirection >= 0
                when {
                    motionScale <= 0f ->
                        EnterTransition.None togetherWith ExitTransition.None

                    softMotion -> {
                        val duration = RingMotion.durationMillis(motionScale)
                        val slide =
                            tween<IntOffset>(
                                durationMillis = duration,
                                easing = RingMotion.softEasing,
                            )
                        val fade =
                            tween<Float>(
                                durationMillis = duration,
                                easing = RingMotion.softEasing,
                            )
                        val enter =
                            slideInHorizontally(animationSpec = slide) { width ->
                                if (forward) {
                                    width / RingMotion.SOFT_SLIDE_DIVISOR
                                } else {
                                    -width / RingMotion.SOFT_SLIDE_DIVISOR
                                }
                            } + fadeIn(animationSpec = fade)
                        val exit =
                            slideOutHorizontally(animationSpec = slide) { width ->
                                if (forward) {
                                    -width / RingMotion.SOFT_SLIDE_DIVISOR
                                } else {
                                    width / RingMotion.SOFT_SLIDE_DIVISOR
                                }
                            } + fadeOut(animationSpec = fade)
                        enter togetherWith exit
                    }

                    else -> {
                        val enter =
                            slideInHorizontally { width ->
                                if (forward) width / 4 else -width / 4
                            } + fadeIn()
                        val exit =
                            slideOutHorizontally { width ->
                                if (forward) -width / 4 else width / 4
                            } + fadeOut()
                        enter togetherWith exit
                    }
                }
            },
            label = label,
            modifier = Modifier.fillMaxSize(),
        ) { state ->
            content(state)
        }
    }
}
