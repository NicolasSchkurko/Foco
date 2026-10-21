p = 'app/src/main/java/com/foco/app/ui/detail/AppDetailScreen.kt'
t = open(p, encoding='utf-8').read()

old = '''                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            var tracking = false
                            var accumulated = 0f
                            var vertical = false
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                when (event.type) {
                                    PointerEventType.Press -> {
                                        tracking = true
                                        accumulated = 0f
                                        vertical = false
                                    }
                                    PointerEventType.Move -> {
                                        if (!tracking) continue
                                        val change = event.changes.firstOrNull() ?: continue
                                        val dx = change.position.x - change.previousPosition.x
                                        val dy = change.position.y - change.previousPosition.y
                                        if (kotlin.math.abs(dy) > kotlin.math.abs(dx) && kotlin.math.abs(dy) > 4f) {
                                            // Vertical scroll: stop tracking, let the list handle it.
                                            tracking = false
                                            accumulated = 0f
                                        } else if (tracking) {
                                            accumulated += dx
                                            if (accumulated > size.width * 0.25f) {
                                                backDispatcher?.onBackPressed()
                                                tracking = false
                                                accumulated = 0f
                                            } else if (accumulated < -size.width * 0.25f) {
                                                tracking = false
                                                accumulated = 0f
                                            }
                                        }
                                    }
                                    PointerEventType.Release, PointerEventType.Exit -> {
                                        tracking = false
                                        accumulated = 0f
                                    }
                                }
                                if (tracking && kotlin.math.abs(accumulated) > size.width * 0.1f) {
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        }
                    },'''

new = '''                    .pointerInput(Unit) {
                        // Intercepted in the Initial pass so a horizontal swipe reaches the screen
                        // even when the touch starts on the scrollable list.
                        awaitPointerEventScope {
                            var startX = 0f
                            var startY = 0f
                            var tracking = false
                            var horizontal = false
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val change = event.changes.firstOrNull() ?: continue
                                when (event.type) {
                                    PointerEventType.Press -> {
                                        tracking = true
                                        horizontal = false
                                        startX = change.position.x
                                        startY = change.position.y
                                    }
                                    PointerEventType.Move -> {
                                        if (!tracking) continue
                                        val dx = change.position.x - startX
                                        val dy = change.position.y - startY
                                        if (!horizontal) {
                                            if (kotlin.math.abs(dx) > kotlin.math.abs(dy) && kotlin.math.abs(dx) > 24f) {
                                                horizontal = true
                                            } else if (kotlin.math.abs(dy) > kotlin.math.abs(dx) && kotlin.math.abs(dy) > 24f) {
                                                // Vertical scroll: the list keeps the gesture.
                                                tracking = false
                                            }
                                        }
                                        if (horizontal) {
                                            change.consume()
                                            if (dx > size.width * 0.25f) {
                                                backDispatcher?.onBackPressed()
                                                tracking = false
                                                horizontal = false
                                            }
                                        }
                                    }
                                    PointerEventType.Release, PointerEventType.Exit -> {
                                        tracking = false
                                        horizontal = false
                                    }
                                }
                            }
                        }
                    },'''

assert old in t, 'gesture old not found'
t = t.replace(old, new, 1)
open(p, 'w', encoding='utf-8').write(t)
print('gesture rewritten')
