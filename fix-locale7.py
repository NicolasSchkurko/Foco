import io

p = 'app/src/androidTest/java/com/foco/app/ui/LimitsQuickAdjustTest.kt'
t = io.open(p, encoding='utf-8').read()
t = t.replace('    @get:Rule val compose = createComposeRule()', '    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()', 1)
t = t.replace('import androidx.compose.ui.test.junit4.createComposeRule\n', '', 1)
io.open(p, 'w', encoding='utf-8').write(t)
print([l for l in t.splitlines() if 'createComposeRule' in l or 'createAndroidComposeRule' in l][:3])
