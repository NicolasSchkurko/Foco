package com.foco.app.ui

import androidx.compose.foundation.lazy.LazyColumn
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
import com.foco.app.ui.settings.appLimitsSection
import com.foco.app.ui.theme.FocoTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Category: settings limits (API 36). The section lists apps and persists the picked minutes. */
@RunWith(AndroidJUnit4::class)
class AppLimitsSettingsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val strings = InstrumentationRegistry.getInstrumentation().targetContext.resources

    private fun showSection(
        picked: (Pair<String, Int?>?) -> Unit,
        limits: Map<String, Int> = mapOf("com.example.one" to 30),
        alerts: Map<String, Boolean> = emptyMap(),
        notificationsEnabled: Boolean = true,
        onAlert: (Pair<String, Boolean>) -> Unit = {}
    ) {
        composeRule.setContent {
            FocoTheme {
                LazyColumn {
                    appLimitsSection(
                        apps = listOf("com.example.one" to "One"),
                        limits = limits,
                        alerts = alerts,
                        notificationsEnabled = notificationsEnabled,
                        onLimitChange = { packageName, minutes -> picked(packageName to minutes) },
                        onAlertChange = { packageName, enabled -> onAlert(packageName to enabled) }
                    )
                }
            }
        }
    }

    private fun alertToggle() = composeRule.onNodeWithContentDescription(
        strings.getString(R.string.settings_limit_alerts_toggle, "One")
    )

    @Test
    fun theRowShowsItsCurrentLimitAndPicksAStep() {
        var picked: Pair<String, Int?>? = null
        showSection(picked = { picked = it })

        composeRule.onNodeWithText("One").assertIsDisplayed()
        composeRule.onNodeWithText("30 min").assertIsDisplayed()

        composeRule.onNodeWithText("One").performClick()
        composeRule.onNodeWithText("45 min").performClick()

        assertEquals("com.example.one" to 45, picked)
    }

    @Test
    fun noLimitClearsTheLimit() {
        var picked: Pair<String, Int?>? = null
        showSection(picked = { picked = it })

        composeRule.onNodeWithText("One").performClick()
        composeRule.onNodeWithText(strings.getString(R.string.settings_limit_none)).performClick()

        assertEquals("com.example.one", picked?.first)
        assertNull(picked?.second)
    }

    @Test
    fun blockImmediatelyPicksZero() {
        var picked: Pair<String, Int?>? = null
        showSection(picked = { picked = it })

        composeRule.onNodeWithText("One").performClick()
        composeRule.onNodeWithText(
            strings.getString(R.string.settings_limit_block_now)
        ).performClick()

        assertEquals("com.example.one" to 0, picked)
    }

    @Test
    fun theAlertToggleStartsOffAndIsEnabledWithAPositiveLimit() {
        var changed: Pair<String, Boolean>? = null
        showSection(picked = {}) { changed = it }

        alertToggle().assertIsDisplayed().assertIsOff().assertIsEnabled()
        alertToggle().performClick()

        assertEquals("com.example.one" to true, changed)
    }

    @Test
    fun zeroMinutesLeavesTheAlertToggleDisabled() {
        showSection(picked = {}, limits = mapOf("com.example.one" to 0))

        alertToggle().assertIsOff().assertIsNotEnabled()
    }

    @Test
    fun noLimitLeavesTheAlertToggleDisabled() {
        showSection(picked = {}, limits = emptyMap())

        alertToggle().assertIsOff().assertIsNotEnabled()
    }

    @Test
    fun theStoredAlertChoiceIsShown() {
        showSection(
            picked = {},
            alerts = mapOf("com.example.one" to true)
        )

        alertToggle().assertIsOn()
    }

    @Test
    fun aMissingNotificationPermissionIsExplained() {
        showSection(
            picked = {},
            alerts = mapOf("com.example.one" to true),
            notificationsEnabled = false
        )

        composeRule.onNodeWithText(
            strings.getString(R.string.settings_limit_alerts_no_permission)
        ).assertIsDisplayed()
    }
}
