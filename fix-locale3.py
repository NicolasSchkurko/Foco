import io

p = 'app/src/androidTest/java/com/foco/app/ui/AppDetailLimitsTest.kt'
t = io.open(p, encoding='utf-8').read()
t = t.replace('import androidx.appcompat.app.AppCompatDelegate\n', 'import android.content.res.Configuration\nimport androidx.appcompat.app.AppCompatDelegate\n', 1)
io.open(p, 'w', encoding='utf-8').write(t)
print('import fixed')
