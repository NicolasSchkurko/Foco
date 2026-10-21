package com.foco.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.test.platform.app.InstrumentationRegistry
import com.foco.app.R
import com.foco.app.data.AppLimitsStore
import com.foco.app.ui.common.LimitsQuickAdjust
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * The quick adjust of one app: the `-15 min` / `+15 min` buttons and the time-range picker, with
 * the picks the caller persists through `AppLimitsStore`.
 */
class LimitsQuickAdjustTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val store = AppLimitsStore.forApp(targetContext)
    private val packageName = "com.example.quick"

    /** The strings of the activity: the per-app locale wins over the device one. */
    private fun appString(resId: Int, vararg args: Any): String =
        compose.activity.resources.getString(resId, *args)

    @Before
    fun clean() {
        runBlocking {
            store.clearLimit(packageName)
            store.clearWindow(packageName)
        }
    }

    private fun show(
        onLimitChange: (String, Int?) -> Unit = { _, _ -> },
        onWindowChange: (String, Int, Int) -> Unit = { _, _, _ -> },
        onWindowClear: (String) -> Unit = { },
    ) {
        compose.setContent {
            MaterialTheme {
                LimitsQuickAdjust(
                    store = store,
                    packageName = packageName,
                    onLimitChange = onLimitChange,
                    onWindowChange = onWindowChange,
                    onWindowClear = onWindowClear,
                )
            }
        }
    }

    @Test
    fun thePlusButtonAddsOneStepAndReportsIt() {
        var limit: Int? = null
        show(onLimitChange = { _, minutes -> limit = minutes })

        compose.onNodeWithText(appString(R.string.settings_limits_button_plus)).performClick()

        assertEquals(15, limit)
    }

    @Test
    fun theMinusButtonOnNoLimitDoesNothing() {
        var called = false
        show(onLimitChange = { _, _ -> called = true })

        compose.onNodeWithText(appString(R.string.settings_limits_button_minus)).performClick()

        assertFalse("no limit must not be cleared", called)
    }

    @Test
    fun steppingDownFromOneStepLandsOnBlockNowAndNotOnNoLimit() {
        runBlocking { store.setLimit(packageName, 15) }
        var limit: Int? = null
        show(onLimitChange = { _, minutes -> limit = minutes })
        // The stored limit is read on first composition; wait for it.
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText(appString(R.string.settings_limits_value_minutes, 15)).fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNodeWithText(appString(R.string.settings_limits_button_minus)).performClick()

        assertEquals(
            "stepping down must land on 0 (block now), never on no limit",
            0,
            limit,
        )
    }

    @Test
    fun theLongPressRepeatsTheStep() {
        var limit: Int? = null
        show(onLimitChange = { _, minutes -> limit = minutes })
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText(appString(R.string.settings_limits_value_none)).fetchSemanticsNodes().isNotEmpty()
        }

        // The finger stays down: the repeat runs while it is held.
        compose.onNodeWithText(appString(R.string.settings_limits_button_plus)).performTouchInput {
            down(center)
            advanceEventTime(1_500)
        }

        compose.waitUntil(timeoutMillis = 10_000) {
            (limit ?: 0) > 15
        }
    }

    @Test
    fun thePickerOpensWithTheSystemTimeFormat() {
        show()

        val pickerLabel = appString(R.string.settings_limits_picker_label)
        compose.onAllNodesWithText(pickerLabel).filterToOne(hasClickAction()).performClick()
        compose.onNodeWithText(appString(R.string.settings_limits_picker_title)).assertIsDisplayed()

        compose.onNodeWithText(timeLabel(9)).assertIsDisplayed()
    }

    /** The clock label of the given hour, in the system time format. */
    private fun timeLabel(hour: Int): String =
        if (android.text.format.DateFormat.is24HourFormat(targetContext)) {
            String.format(java.util.Locale.ROOT, "%02d:00", hour)
        } else {
            val cal = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, hour)
                set(java.util.Calendar.MINUTE, 0)
            }
            android.text.format.DateFormat.getTimeFormat(targetContext).format(cal.time)
        }

    @Test
    fun thePickerReportsTheChosenRange() {
        var range: Pair<Int, Int>? = null
        show(onWindowChange = { _, start, end -> range = start to end })

        val pickerLabel = appString(R.string.settings_limits_picker_label)
        compose.onAllNodesWithText(pickerLabel).filterToOne(hasClickAction()).performClick()
        // The start dropdown shows the current start, in the system time format.
        compose.onNodeWithText(timeLabel(9)).performClick()
        // The dropdown lives in a popup: the semantic action avoids a coordinate click.
        compose.onNodeWithText(timeLabel(10)).performSemanticsAction(SemanticsActions.OnClick)
        // The start button (the one with the click action) shows the new value before confirming.
        compose.waitUntil(timeoutMillis = 10_000) {
            runCatching {
                compose.onAllNodesWithText(timeLabel(10)).filterToOne(hasClickAction())
            }.isSuccess
        }
        compose.onNodeWithText(appString(R.string.settings_limits_picker_ok)).performClick()

        // The visible outcome: the range under the picker shows the chosen start.
        val rangeText =
            appString(
                R.string.settings_window_range,
                timeLabel(10),
                timeLabel(23),
            )
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText(rangeText).fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitUntil(timeoutMillis = 10_000) { range != null }
        assertEquals(10 * 60 to 23 * 60, range)
    }

    @Test
    fun clearingTheWindowIsReported() {
        runBlocking { store.setWindow(packageName, 9 * 60, 23 * 60) }
        var cleared = false
        show(onWindowClear = { cleared = true })
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText(appString(R.string.settings_window_clear)).fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNodeWithText(appString(R.string.settings_window_clear)).performClick()

        assertTrue(cleared)
    }
}
