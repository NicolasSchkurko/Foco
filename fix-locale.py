import io

p = 'app/src/androidTest/java/com/foco/app/ui/LimitsQuickAdjustTest.kt'
t = io.open(p, encoding='utf-8').read()
t = t.replace('import androidx.compose.material3.MaterialTheme\n', 'import androidx.appcompat.app.AppCompatDelegate\nimport androidx.compose.material3.MaterialTheme\n', 1)
t = t.replace('import androidx.test.platform.app.InstrumentationRegistry\n', 'import androidx.core.os.LocaleListCompat\nimport androidx.test.platform.app.InstrumentationRegistry\n', 1)
t = t.replace('import org.junit.Before\n', 'import org.junit.After\nimport org.junit.Before\n', 1)
old = '''    @Before
    fun clean() {
        runBlocking { store.clearLimit(packageName) }
    }'''
new = '''    @Before
    fun clean() {
        // Another test may leave the app in Spanish; the strings here are resolved in English.
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
        runBlocking { store.clearLimit(packageName) }
    }

    @After
    fun restoreAuto() {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
    }'''
assert old in t, 'before not found'
t = t.replace(old, new, 1)
io.open(p, 'w', encoding='utf-8').write(t)
print('limits test: locale forced')
