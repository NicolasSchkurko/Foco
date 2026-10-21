p = 'app/src/main/java/com/foco/app/ui/detail/AppDetailScreen.kt'
t = open(p, encoding='utf-8').read()
if 'import androidx.compose.ui.input.pointer.awaitPointerEventScope' not in t:
    t = t.replace('import androidx.compose.ui.input.pointer.pointerInput\n', 'import androidx.compose.ui.input.pointer.awaitPointerEventScope\nimport androidx.compose.ui.input.pointer.pointerInput\n', 1)
open(p, 'w', encoding='utf-8').write(t)
print('done')
