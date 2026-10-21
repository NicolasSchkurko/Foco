package com.foco.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.foco.app.R
import com.foco.app.ui.detail.AppAppSettingsScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AppAppSettingsTest {
    @get:Rule val compose = createComposeRule()

    private val strings = InstrumentationRegistry.getInstrumentation().targetContext.resources

    @Test fun screenShowsTheControlsForOneApp() {
        compose.setContent {
            MaterialTheme { AppAppSettingsScreen(packageName = "com.foco.app") }
        }

        compose.onNodeWithText(strings.getString(R.string.detail_limits_limit_label)).assertIsDisplayed()
        compose.onNodeWithText(strings.getString(R.string.settings_window_label)).assertIsDisplayed()
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
