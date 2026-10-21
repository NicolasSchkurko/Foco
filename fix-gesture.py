p = 'app/src/main/java/com/foco/app/ui/detail/AppDetailScreen.kt'
t = open(p, encoding='utf-8').read()

old = '''    Scaffold { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            change.consume()
                            if (dragAmount > SWIPE_CLOSE_THRESHOLD_PX) backDispatcher?.onBackPressed()
                        }
                    },
        ) {'''
new = '''    Scaffold { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .pointerInput(Unit) {
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
                    },
        ) {'''
assert old in t, 'gesture old not found'
t = t.replace(old, new, 1)
open(p, 'w', encoding='utf-8').write(t)
print('gesture fixed')
