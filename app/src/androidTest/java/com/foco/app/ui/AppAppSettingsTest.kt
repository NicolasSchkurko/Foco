package com.foco.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.foco.app.R
import com.foco.app.data.stores.AppLimitsStore
import com.foco.app.ui.detail.AppAppSettingsScreen
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AppAppSettingsTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val store = AppLimitsStore.forApp(targetContext)
    private val packageName = "com.foco.app"

    /** The strings of the activity: the per-app locale wins over the device one. */
    private val strings get() = compose.activity.resources

    @Test fun screenShowsTheQuickAdjustAndTheAlerts() {
        compose.setContent {
            MaterialTheme { AppAppSettingsScreen(packageName = packageName) }
        }

        compose.onNodeWithText(strings.getString(R.string.detail_limits_limit_label)).assertIsDisplayed()
        compose.onNodeWithText(strings.getString(R.string.settings_limits_button_minus)).assertIsDisplayed()
        compose.onNodeWithText(strings.getString(R.string.settings_limits_button_plus)).assertIsDisplayed()
        compose.onAllNodesWithText(strings.getString(R.string.settings_limits_picker_label))[0].assertIsDisplayed()
        compose.onNodeWithText(strings.getString(R.string.detail_limits_alerts_label)).assertIsDisplayed()
    }

    @Test fun theBackArrowReturns() {
        var back = false
        compose.setContent {
            MaterialTheme { AppAppSettingsScreen(packageName = packageName, onBack = { back = true }) }
        }

        compose.onNodeWithContentDescription(strings.getString(R.string.content_back)).performClick()

        assertTrue(back)
    }

    @Test fun turningTheLimitOnStoresIt() {
        runBlocking { store.clearLimit(packageName) }
        compose.setContent {
            MaterialTheme { AppAppSettingsScreen(packageName = packageName) }
        }
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText(strings.getString(R.string.settings_limits_value_none)).fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNodeWithContentDescription(strings.getString(R.string.detail_limits_limit_label)).performClick()

        compose.waitUntil(timeoutMillis = 10_000) {
            runBlocking { store.limitFor(packageName) } == 15
        }
        assertEquals(15, runBlocking { store.limitFor(packageName) })
    }

    @Test fun turningTheLimitOffClearsIt() {
        runBlocking { store.setLimit(packageName, 30) }
        compose.setContent {
            MaterialTheme { AppAppSettingsScreen(packageName = packageName) }
        }
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText(strings.getString(R.string.settings_limits_value_minutes, 30)).fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNodeWithContentDescription(strings.getString(R.string.detail_limits_limit_label)).performClick()

        compose.waitUntil(timeoutMillis = 10_000) { runBlocking { store.limitFor(packageName) } == null }
        assertNull(runBlocking { store.limitFor(packageName) })
    }
}
