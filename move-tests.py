import io

# 1) The quick-adjust tests move out of the global settings (the UI lives in the app's tab now).
p = 'app/src/androidTest/java/com/foco/app/ui/SettingsScreenTest.kt'
t = io.open(p, encoding='utf-8').read()
start = t.find('    @Test\n    fun limitsQuickAdjustIncrementsAndDecrementsByStep() {')
assert start >= 0, 'tests not found'
end = t.find('    private companion object {', start)
assert end > start
t = t[:start] + t[end:]
io.open(p, 'w', encoding='utf-8').write(t)
print('SettingsScreenTest: quick adjust tests removed')

# 2) The format test joins the new component test.
p = 'app/src/androidTest/java/com/foco/app/ui/LimitsQuickAdjustTest.kt'
t = io.open(p, encoding='utf-8').read()
t = t.replace('import androidx.compose.ui.test.onNodeWithText\n', 'import androidx.compose.ui.test.onAllNodesWithText\nimport androidx.compose.ui.test.onNodeWithText\n', 1)
old = '''    @Test
    fun thePickerIsReachable() {'''
new = '''    @Test
    fun thePickerFormatMatchesTheSystemPreference() {
        compose.setContent {
            MaterialTheme {
                LimitsQuickAdjust(
                    store = store,
                    packageName = packageName,
                    onLimitChange = { _, _ -> },
                    onWindowChange = { _, _, _ -> },
                    onWindowClear = { },
                )
            }
        }

        compose.onAllNodesWithText(strings.getString(R.string.settings_limits_picker_label))[0].performClick()
        compose.onNodeWithText(strings.getString(R.string.settings_limits_picker_title)).assertIsDisplayed()

        val expected =
            if (android.text.format.DateFormat.is24HourFormat(
                    InstrumentationRegistry.getInstrumentation().targetContext,
                )
            ) {
                "09:00"
            } else {
                val cal = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 9)
                    set(java.util.Calendar.MINUTE, 0)
                }
                android.text.format.DateFormat.getTimeFormat(
                    InstrumentationRegistry.getInstrumentation().targetContext,
                ).format(cal.time)
            }
        compose.onNodeWithText(expected).assertIsDisplayed()
    }

    @Test
    fun thePickerIsReachable() {'''
assert old in t, 'picker test not found'
t = t.replace(old, new, 1)
t = t.replace('import org.junit.Assert.assertEquals\n', 'import org.junit.Assert.assertEquals\nimport org.junit.Assert.assertTrue\n', 1)
io.open(p, 'w', encoding='utf-8').write(t)
print('LimitsQuickAdjustTest: format test added')
