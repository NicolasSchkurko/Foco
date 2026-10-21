package com.foco.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.foco.app.R
import com.foco.app.ui.detail.AppDetailScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AppDetailLimitsTest {
    @get:Rule val compose = createComposeRule()

    private val strings = InstrumentationRegistry.getInstrumentation().targetContext.resources

    @Test fun previewShowsValues() {
        compose.setContent {
            MaterialTheme { AppDetailScreen(packageName = "com.foco.app", onConfigure = {}) }
        }
        compose.onNodeWithText(strings.getString(R.string.detail_limits_section)).assertIsDisplayed()
    }

    @Test fun theGearOpensTheDedicatedSettings() {
        var opened = false
        compose.setContent {
            MaterialTheme { AppDetailScreen(packageName = "com.foco.app", onConfigure = { opened = true }) }
        }

        compose
            .onNodeWithContentDescription(strings.getString(R.string.detail_limits_configure))
            .performClick()

        assertTrue(opened)
    }
}
