import io

p = 'app/src/androidTest/java/com/foco/app/ui/AppDetailLimitsTest.kt'
t = io.open(p, encoding='utf-8').read()

t = t.replace(
    'MaterialTheme { AppDetailScreen(packageName = PACKAGE, date = LocalDate.now().minusDays(1)) }',
    'MaterialTheme {\n                AppDetailScreen(\n                    packageName = PACKAGE,\n                    date = LocalDate.now().minusDays(1),\n                    onConfigure = { opened = true },\n                )\n            }',
)
io.open(p, 'w', encoding='utf-8').write(t)
print('onConfigure wired')
