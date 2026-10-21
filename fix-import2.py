p = 'app/src/main/java/com/foco/app/ui/common/LimitsQuickAdjust.kt'
t = open(p, encoding='utf-8').read()
t = t.replace('import androidx.compose.foundation.layout.height\n', 'import androidx.compose.foundation.layout.fillMaxWidth\nimport androidx.compose.foundation.layout.height\n', 1)
open(p, 'w', encoding='utf-8').write(t)
print('done')
