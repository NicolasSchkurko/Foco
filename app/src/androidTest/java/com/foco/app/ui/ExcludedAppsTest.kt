package com.foco.app.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.foco.app.MainActivity
import com.foco.app.R
import com.foco.app.data.stores.ExcludedAppsStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Long-press excludes the app from the ranking (the total does not change), Undo brings it back and the
 * exclusion persists across a relaunch. E2E with real usage (Settings) on API 36; texts from resources.
 */
@RunWith(AndroidJUnit4::class)
class ExcludedAppsTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

    private fun string(resId: Int): String = targetContext.resources.getString(resId)

    private val usageLinePrefix = string(R.string.dashboard_usage_line).substringBefore(":")

    private fun shell(command: String) {
        InstrumentationRegistry
            .getInstrumentation()
            .uiAutomation
            .executeShellCommand(command)
            .close()
    }

    private fun headerText(): String =
        composeRule
            .onAllNodesWithText(usageLinePrefix, substring = true)
            .fetchSemanticsNodes()
            .first()
            .config
            .getOrNull(SemanticsProperties.Text)
            ?.joinToString()
            .orEmpty()

    @After
    fun cleanUp() {
        // Leave no exclusions stuck on the device.
        runBlocking { ExcludedAppsStore(targetContext).restore(SETTINGS_PACKAGE) }
    }

    @Test
    fun longPressExcludesUndoRestoresAndTheExclusionPersists() {
        setAccessibilityServiceEnabled(true)
        shell("appops set com.foco.app android:get_usage_stats allow")
        shell("am start -a android.settings.SETTINGS")
        waitForSettingsForeground(composeRule)
        shell("input keyevent KEYCODE_HOME")
        waitForSettingsBackground(composeRule)

        var scenario = ActivityScenario.launch(MainActivity::class.java)

        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText("Settings").fetchSemanticsNodes().isNotEmpty()
        }
        val totalBefore = headerText()

        // Long-press -> excluded from the ranking, total intact.
        composeRule.onNodeWithText("Settings").performTouchInput { longClick() }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Settings").fetchSemanticsNodes().isEmpty()
        }
        assertEquals(totalBefore, headerText())

        // Undo -> back into the ranking.
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(string(R.string.excluded_undo)).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(string(R.string.excluded_undo)).performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Settings").fetchSemanticsNodes().isNotEmpty()
        }

        // Exclude again and relaunch: the exclusion persists.
        composeRule.onNodeWithText("Settings").performTouchInput { longClick() }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Settings").fetchSemanticsNodes().isEmpty()
        }
        scenario.close()

        scenario = ActivityScenario.launch(MainActivity::class.java)
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText(string(R.string.loading)).fetchSemanticsNodes().isEmpty()
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Settings").fetchSemanticsNodes().isEmpty()
        }
        scenario.close()
    }

    private companion object {
        const val SETTINGS_PACKAGE = "com.android.settings"
    }
}
