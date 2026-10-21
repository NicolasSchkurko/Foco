import io

p = 'app/src/androidTest/java/com/foco/app/ui/LimitsQuickAdjustTest.kt'
t = io.open(p, encoding='utf-8').read()

t = t.replace('import androidx.compose.ui.test.junit4.createComposeRule\n', 'import androidx.activity.ComponentActivity\nimport androidx.compose.ui.test.junit4.createAndroidComposeRule\n', 1)
t = t.replace('    val compose = createComposeRule()', '    val compose = createAndroidComposeRule<ComponentActivity>()', 1)
old = '''    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val store = AppLimitsStore.forApp(targetContext)
    private val packageName = "com.example.quick"

    /** The string as the app renders it: the per-app locale wins over the device one. */
    private fun appString(resId: Int): String {
        val tags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        if (tags.isEmpty()) return targetContext.getString(resId)
        val config = Configuration(targetContext.resources.configuration)
        config.setLocales(LocaleList.forLanguageTags(tags))
        return targetContext.createConfigurationContext(config).getString(resId)
    }'''
new = '''    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val store = AppLimitsStore.forApp(targetContext)
    private val packageName = "com.example.quick"

    /** The strings of the activity: the per-app locale wins over the device one. */
    private fun appString(resId: Int): String = compose.activity.resources.getString(resId)'''
assert old in t, 'strings block not found'
t = t.replace(old, new, 1)
for imp in ['import android.content.res.Configuration\n', 'import android.os.LocaleList\n', 'import androidx.appcompat.app.AppCompatDelegate\n']:
    t = t.replace(imp, '', 1)
io.open(p, 'w', encoding='utf-8').write(t)
print('limits test uses activity resources')
