package com.foco.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.foco.app.R
import com.foco.app.domain.AppWindow
import com.foco.app.ui.detail.AppLimitEditor
import com.foco.app.ui.theme.FocoTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Category: app limits (API 36). The per-app editor shows the stored values and reports picks. */
@RunWith(AndroidJUnit4::class)
class AppLimitsSettingsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val strings = InstrumentationRegistry.getInstrumentation().targetContext.resources

    private val packageName = "com.example.one"

    private fun showEditor(
        picked: (Int?) -> Unit,
        minutes: Int? = 30,
        window: AppWindow? = null,
        alertEnabled: Boolean = false,
        notificationsEnabled: Boolean = true,
        onAlert: (Boolean) -> Unit = {},
        onWindow: (Int, Int) -> Unit = { _, _ -> },
        onWindowClear: () -> Unit = {},
    ) {
        composeRule.setContent {
            FocoTheme {
                AppLimitEditor(
                    label = "One",
                    minutes = minutes,
                    window = window,
                    alertEnabled = alertEnabled,
                    notificationsEnabled = notificationsEnabled,
                    onPick = picked,
                    onWindowChange = onWindow,
                    onWindowClear = onWindowClear,
                    onAlertChange = onAlert,
                )
            }
        }
    }

    private fun limitRow() = composeRule.onNodeWithText(strings.getString(R.string.detail_limits_limit_label))

    private fun alertToggle() =
        composeRule.onNodeWithContentDescription(
            strings.getString(R.string.settings_limit_alerts_toggle, "One"),
        )

    @Test
    fun theEditorShowsItsCurrentLimitAndPicksAStep() {
        var picked: Int? = null
        showEditor(picked = { picked = it })

        composeRule.onNodeWithText("30 min").assertIsDisplayed()

        limitRow().performClick()
        composeRule.onNodeWithText("45 min").performClick()

        assertEquals(45, picked)
    }

    @Test
    fun noLimitClearsTheLimit() {
        var picked: Int? = -1
        showEditor(picked = { picked = it })

        limitRow().performClick()
        composeRule.onNodeWithText(strings.getString(R.string.settings_limit_none)).performClick()

        assertNull(picked)
    }

    @Test
    fun blockImmediatelyPicksZero() {
        var picked: Int? = null
        showEditor(picked = { picked = it })

        limitRow().performClick()
        composeRule
            .onNodeWithText(
                strings.getString(R.string.settings_limit_block_now),
            ).performClick()

        assertEquals(0, picked)
    }

    @Test
    fun theAlertToggleStartsOffAndIsEnabledWithAPositiveLimit() {
        var changed: Boolean? = null
        showEditor(picked = {}, onAlert = { changed = it })

        alertToggle().assertIsDisplayed().assertIsOff().assertIsEnabled()
        alertToggle().performClick()

        assertEquals(true, changed)
    }

    @Test
    fun zeroMinutesLeavesTheAlertToggleDisabled() {
        showEditor(picked = {}, minutes = 0)

        alertToggle().assertIsOff().assertIsNotEnabled()
    }

    @Test
    fun noLimitLeavesTheAlertToggleDisabled() {
        showEditor(picked = {}, minutes = null)

        alertToggle().assertIsOff().assertIsNotEnabled()
    }

    @Test
    fun theStoredAlertChoiceIsShown() {
        showEditor(picked = {}, alertEnabled = true)

        alertToggle().assertIsOn()
    }

    @Test
    fun aMissingNotificationPermissionIsExplained() {
        showEditor(picked = {}, alertEnabled = true, notificationsEnabled = false)

        composeRule
            .onNodeWithText(
                strings.getString(R.string.settings_limit_alerts_no_permission),
            ).assertIsDisplayed()
    }

    @Test
    fun noWindowIsShownByDefault() {
        showEditor(picked = {})

        composeRule
            .onNodeWithText(strings.getString(R.string.settings_window_none))
            .assertIsDisplayed()
    }

    @Test
    fun aConfiguredWindowIsShown() {
        showEditor(picked = {}, window = AppWindow(9 * 60, 23 * 60))

        composeRule
            .onNodeWithText(strings.getString(R.string.settings_window_range, "09:00", "23:00"))
            .assertIsDisplayed()
    }

    @Test
    fun pickingANewStartReportsThePair() {
        var picked: Pair<Int, Int>? = null
        showEditor(
            picked = {},
            window = AppWindow(9 * 60, 23 * 60),
            onWindow = { start, end -> picked = start to end },
        )

        composeRule
            .onNodeWithContentDescription(strings.getString(R.string.settings_window_from))
            .performClick()
        composeRule.onNodeWithText("10:00").performClick()

        assertEquals(10 * 60 to 23 * 60, picked)
    }
}
