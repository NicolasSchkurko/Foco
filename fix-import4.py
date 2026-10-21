import io

p = 'app/src/androidTest/java/com/foco/app/ui/LimitsQuickAdjustTest.kt'
t = io.open(p, encoding='utf-8').read()
t = t.replace('import androidx.compose.ui.test.onAllNodesWithText\n', 'import androidx.compose.ui.test.filterToOne\nimport androidx.compose.ui.test.onAllNodesWithText\n', 1)
io.open(p, 'w', encoding='utf-8').write(t)
print('import added')
