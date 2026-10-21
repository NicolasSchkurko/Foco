import io

p = 'app/src/androidTest/java/com/foco/app/ui/AppDetailLimitsTest.kt'
t = io.open(p, encoding='utf-8').read()
old = '''        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales.isEmpty) return targetContext.getString(resId)
        val config = Configuration(targetContext.resources.configuration)
        config.setLocales(locales)
        return targetContext.createConfigurationContext(config).getString(resId)'''
new = '''        val tags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        if (tags.isEmpty()) return targetContext.getString(resId)
        val config = Configuration(targetContext.resources.configuration)
        config.setLocales(LocaleList.forLanguageTags(tags))
        return targetContext.createConfigurationContext(config).getString(resId)'''
assert old in t, 'old not found'
t = t.replace(old, new, 1)
t = t.replace('import android.content.res.Configuration\n', 'import android.content.res.Configuration\nimport android.os.LocaleList\n', 1)
io.open(p, 'w', encoding='utf-8').write(t)
print('fixed')
