import io

p = 'app/src/androidTest/java/com/foco/app/ui/AppDetailLimitsTest.kt'
t = io.open(p, encoding='utf-8').read()

old = '''    private val strings = InstrumentationRegistry.getInstrumentation().targetContext.resources

    @Before
    fun forceEnglish() {
        // Another test may leave the app in Spanish; the strings here are resolved in English.
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
    }

    @After
    fun restoreAuto() {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
    }
'''

new = '''    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

    /** The string as the app renders it: the per-app locale wins over the device one. */
    private fun appString(resId: Int): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales.isEmpty) return targetContext.getString(resId)
        val config = Configuration(targetContext.resources.configuration)
        config.setLocales(locales)
        return targetContext.createConfigurationContext(config).getString(resId)
    }
'''

assert old in t, 'old block not found'
t = t.replace(old, new, 1)

# Use the locale-aware lookup in the assertions.
t = t.replace('strings.getString(R.string.detail_limits_section)', 'appString(R.string.detail_limits_section)')
t = t.replace('strings.getString(R.string.detail_limits_configure)', 'appString(R.string.detail_limits_configure)')

# Fix the imports: no more LocaleListCompat / After / Before.
t = t.replace('import androidx.core.os.LocaleListCompat\n', '', 1)
t = t.replace('import org.junit.After\n', '', 1)
t = t.replace('import org.junit.Before\n', '', 1)
t = t.replace('import android.content.Intent\n', 'import android.content.Intent\nimport android.content.res.Configuration\n', 1)

io.open(p, 'w', encoding='utf-8').write(t)
print('locale-aware strings')
