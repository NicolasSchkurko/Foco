package com.foco.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.test.platform.app.InstrumentationRegistry
import com.foco.app.R
import com.foco.app.domain.AppWindow
import com.foco.app.ui.common.LimitsQuickAdjust
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * The quick adjust of one app: the `-15 min` / `+15 min` buttons and the time-range picker, with
 * the picks the caller persists.
 */
class LimitsQuickAdjustTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val packageName = "com.example.quick"

    /** The strings of the activity: the per-app locale wins over the device one. */
    private fun appString(
        resId: Int,
        vararg args: Any,
    ): String = compose.activity.resources.getString(resId, *args)

    private fun show(
        limit: Int? = null,
        window: AppWindow? = null,
        onLimitChange: (Int?) -> Unit = { },
        onWindowChange: (Int, Int) -> Unit = { _, _ -> },
        onWindowClear: () -> Unit = { },
    ) {
        compose.setContent {
            MaterialTheme {
                LimitsQuickAdjust(
                    limit = limit,
                    window = window,
                    onLimitChange = onLimitChange,
                    onWindowChange = onWindowChange,
                    onWindowClear = onWindowClear,
                )
            }
        }
    }

    @Test
    fun turningTheLimitOnSetsAFifteenMinuteLimit() {
        var limit: Int? = -1
        show(onLimitChange = { minutes -> limit = minutes })

        compose.onNodeWithContentDescription(appString(R.string.detail_limits_limit_label)).performClick()

        assertEquals(15, limit)
    }

    @Test
    fun turningTheLimitOffClearsIt() {
        var limit: Int? = -1
        show(limit = 30, onLimitChange = { minutes -> limit = minutes })

        compose.onNodeWithContentDescription(appString(R.string.detail_limits_limit_label)).performClick()

        assertNull(limit)
    }

    @Test
    fun theStepperIsDisabledWhileTheLimitIsOff() {
        show()

        compose
            .onNodeWithText(appString(R.string.settings_limits_button_plus))
            .assertIsNotEnabled()
    }

    @Test
    fun theMinusButtonIsDisabledAtTheFloor() {
        show(limit = 0)

        compose
            .onNodeWithText(appString(R.string.settings_limits_button_minus))
            .assertIsNotEnabled()
    }

    @Test
    fun thePlusButtonAddsOneStepAndReportsIt() {
        var limit: Int? = null
        show(onLimitChange = { minutes -> limit = minutes })

        compose.onNodeWithText(appString(R.string.settings_limits_button_plus)).performClick()

        assertEquals(15, limit)
    }

    @Test
    fun theMinusButtonOnNoLimitDoesNothing() {
        var called = false
        show(onLimitChange = { minutes -> called = true })

        compose.onNodeWithText(appString(R.string.settings_limits_button_minus)).performClick()

        assertFalse("no limit must not be cleared", called)
    }

    @Test
    fun steppingDownFromOneStepLandsOnBlockNowAndNotOnNoLimit() {
        var limit: Int? = null
        show(limit = 15, onLimitChange = { minutes -> limit = minutes })

        compose.onNodeWithText(appString(R.string.settings_limits_button_minus)).performClick()

        assertEquals(
            "stepping down must land on 0 (block now), never on no limit",
            0,
            limit,
        )
    }

    @Test
    fun theStepperRespondsToTheAccessibilityClickAction() {
        var limit: Int? = null
        show(limit = 15, onLimitChange = { minutes -> limit = minutes })

        compose
            .onNodeWithText(appString(R.string.settings_limits_button_plus))
            .performSemanticsAction(SemanticsActions.OnClick)

        assertEquals(
            "the accessibility action must step the limit, not do nothing",
            30,
            limit,
        )
    }

    @Test
    fun theValueAnnouncesTheLimitAsItsState() {
        show(limit = 15)

        val label = appString(R.string.settings_limits_value_minutes, 15)
        compose
            .onNodeWithText(label)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, label))
    }

    @Test
    fun theLongPressRepeatsTheStep() {
        var limit: Int? = null
        show(onLimitChange = { minutes -> limit = minutes })

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

        compose.onNodeWithText(appString(R.string.settings_limits_picker_open)).performClick()
        compose.onNodeWithText(appString(R.string.settings_limits_picker_title)).assertIsDisplayed()

        compose.onNodeWithText(timeLabel(9)).assertIsDisplayed()
    }

    /** The clock label of the given hour and minute, in the system time format. */
    private fun timeLabel(
        hour: Int,
        minute: Int = 0,
    ): String =
        if (android.text.format.DateFormat
                .is24HourFormat(targetContext)
        ) {
            String.format(java.util.Locale.ROOT, "%02d:%02d", hour, minute)
        } else {
            val cal =
                java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, hour)
                    set(java.util.Calendar.MINUTE, minute)
                }
            android.text.format.DateFormat
                .getTimeFormat(targetContext)
                .format(cal.time)
        }

    @Test
    fun thePickerOffersTheQuarterHourGrid() {
        show()

        compose.onNodeWithText(appString(R.string.settings_limits_picker_open)).performClick()
        compose.onNodeWithText(timeLabel(9)).performClick()

        // The grid is the domain's 15-minute one: a quarter past nine is on it.
        compose.onNodeWithText(timeLabel(9, 15)).assertExists()
    }

    @Test
    fun thePickerReportsTheChosenRange() {
        var range: Pair<Int, Int>? = null
        show(onWindowChange = { start, end -> range = start to end })

        compose.onNodeWithText(appString(R.string.settings_limits_picker_open)).performClick()
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
        var cleared = false
        show(window = AppWindow(9 * 60, 23 * 60), onWindowClear = { cleared = true })

        compose.onNodeWithText(appString(R.string.settings_window_clear)).performClick()

        assertTrue(cleared)
    }
}
