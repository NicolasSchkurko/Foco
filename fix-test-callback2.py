import io

p = 'app/src/androidTest/java/com/foco/app/ui/AppDetailLimitsTest.kt'
t = io.open(p, encoding='utf-8').read()

# The first test has no `opened` variable: give it its own body.
old = '''    @Test fun previewShowsValues() {
        compose.setContent {
            MaterialTheme {
                AppDetailScreen(
                    packageName = PACKAGE,
                    date = LocalDate.now().minusDays(1),
                    onConfigure = { opened = true },
                )
            }
        }
        compose.onNodeWithText(appString(R.string.detail_limits_section)).assertIsDisplayed()
    }'''
new = '''    @Test fun previewShowsValues() {
        compose.setContent {
            MaterialTheme {
                AppDetailScreen(
                    packageName = PACKAGE,
                    date = LocalDate.now().minusDays(1),
                )
            }
        }
        compose.onNodeWithText(appString(R.string.detail_limits_section)).assertIsDisplayed()
    }'''
assert old in t, 'preview test not found'
t = t.replace(old, new, 1)
io.open(p, 'w', encoding='utf-8').write(t)
print('preview test fixed')
