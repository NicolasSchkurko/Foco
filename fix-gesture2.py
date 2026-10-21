p = 'app/src/main/java/com/foco/app/ui/detail/AppDetailScreen.kt'
t = open(p, encoding='utf-8').read()

old = '''                    .pointerInput(Unit) {
                        var accumulated = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { accumulated = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                accumulated += dragAmount
                                if (accumulated > size.width * 0.3f) {
                                    backDispatcher?.onBackPressed()
                                    accumulated = 0f
                                }
                            },
                            onDragEnd = { accumulated = 0f },
                            onDragCancel = { accumulated = 0f },
                        )
                    },'''

new = '''                    .pointerInput(Unit) {
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

assert old in t, 'old not found'
t = t.replace(old, new, 1)

# Update imports
if 'import androidx.compose.ui.input.pointer.PointerEventPass' not in t:
    t = t.replace('import androidx.compose.ui.input.pointer.pointerInput\n', 'import androidx.compose.ui.input.pointer.PointerEventPass\nimport androidx.compose.ui.input.pointer.PointerEventType\nimport androidx.compose.ui.input.pointer.awaitPointerEventScope\nimport androidx.compose.ui.input.pointer.pointerInput\n', 1)

open(p, 'w', encoding='utf-8').write(t)
print('done')
