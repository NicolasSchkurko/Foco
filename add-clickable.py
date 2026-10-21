p = 'app/src/main/java/com/foco/app/ui/detail/AppDetailScreen.kt'
t = open(p, encoding='utf-8').read()
t = t.replace('import androidx.compose.foundation.layout.Arrangement\n', 'import androidx.compose.foundation.clickable\nimport androidx.compose.foundation.layout.Arrangement\n', 1)
open(p, 'w', encoding='utf-8').write(t)
print('done')
