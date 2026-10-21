package com.foco.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.foco.app.R
import com.foco.app.ui.detail.AppAppSettingsScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AppAppSettingsTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    /** The strings of the activity: the per-app locale wins over the device one. */
    private val strings get() = compose.activity.resources

    @Test fun screenShowsTheQuickAdjustAndTheAlerts() {
        compose.setContent {
            MaterialTheme { AppAppSettingsScreen(packageName = "com.foco.app") }
        }

        compose.onNodeWithText(strings.getString(R.string.settings_limits_quick_adjust)).assertIsDisplayed()
        compose.onNodeWithText(strings.getString(R.string.settings_limits_button_minus)).assertIsDisplayed()
        compose.onNodeWithText(strings.getString(R.string.settings_limits_button_plus)).assertIsDisplayed()
        compose.onAllNodesWithText(strings.getString(R.string.settings_limits_picker_label))[0].assertIsDisplayed()
        compose.onNodeWithText(strings.getString(R.string.detail_limits_alerts_label)).assertIsDisplayed()
    }

    @Test fun theBackArrowReturns() {
        var back = false
        compose.setContent {
            MaterialTheme { AppAppSettingsScreen(packageName = "com.foco.app", onBack = { back = true }) }
        }

        compose.onNodeWithContentDescription(strings.getString(R.string.content_back)).performClick()

        assertTrue(back)
    }
}
