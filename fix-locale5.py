import io

# The quick-adjust test resolves strings the same way: per-app locale first.
p = 'app/src/androidTest/java/com/foco/app/ui/LimitsQuickAdjustTest.kt'
t = io.open(p, encoding='utf-8').read()

old = '''    private val strings = InstrumentationRegistry.getInstrumentation().targetContext.resources'''
new = '''    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

    /** The string as the app renders it: the per-app locale wins over the device one. */
    private fun appString(resId: Int): String {
        val tags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        if (tags.isEmpty()) return targetContext.getString(resId)
        val config = Configuration(targetContext.resources.configuration)
        config.setLocales(LocaleList.forLanguageTags(tags))
        return targetContext.createConfigurationContext(config).getString(resId)
    }'''
assert old in t, 'strings not found'
t = t.replace(old, new, 1)
t = t.replace('strings.getString(', 'appString(')
t = t.replace('import android.content.res.Configuration\n', '', 1)
t = t.replace('import androidx.appcompat.app.AppCompatDelegate\n', 'import android.content.res.Configuration\nimport android.os.LocaleList\nimport androidx.appcompat.app.AppCompatDelegate\n', 1)
# The locale forcing is no longer needed: the lookup follows the app.
old_before = '''    @Before
    fun clean() {
        // Another test may leave the app in Spanish; the strings here are resolved in English.
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
        runBlocking { store.clearLimit(packageName) }
    }

    @After
    fun restoreAuto() {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
    }'''
new_before = '''    @Before
    fun clean() {
        runBlocking { store.clearLimit(packageName) }
    }'''
assert old_before in t, 'before not found'
t = t.replace(old_before, new_before, 1)
t = t.replace('import androidx.core.os.LocaleListCompat\n', '', 1)
t = t.replace('import org.junit.After\n', '', 1)
io.open(p, 'w', encoding='utf-8').write(t)
print('limits test locale-aware')
