package com.foco.app.ui

import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.foco.app.MainActivity
import com.foco.app.R
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Detail navigation E2E: tapping a row opens it, the system back returns and the visible arrow too.
 * It generates real usage (Settings) before launching; the texts come from resources.
 */
@RunWith(AndroidJUnit4::class)
class AppDetailNavigationTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

    private fun string(resId: Int): String = targetContext.resources.getString(resId)

    private fun shell(command: String) {
        InstrumentationRegistry.getInstrumentation()
            .uiAutomation
            .executeShellCommand(command)
            .close()
    }

    /** Permission + real usage, launches the app, taps the Settings row and waits for the detail. */
    private fun launchAndOpenDetail(): ActivityScenario<MainActivity> {
        shell("appops set com.foco.app android:get_usage_stats allow")
        shell("am start -a android.settings.SETTINGS")
        Thread.sleep(2_000)
        shell("input keyevent KEYCODE_HOME")
        Thread.sleep(500)

        val scenario = ActivityScenario.launch(MainActivity::class.java)
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText("Settings").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Settings").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(string(R.string.detail_summary)).fetchSemanticsNodes().isNotEmpty()
        }
        return scenario
    }

    @Test
    fun tappingARowOpensTheDetailAndBackReturns() {
        launchAndOpenDetail().use { scenario ->
            scenario.onActivity { activity -> activity.onBackPressedDispatcher.onBackPressed() }
            composeRule.waitUntil(timeoutMillis = 10_000) {
                composeRule.onAllNodesWithText(string(R.string.dashboard_title)).fetchSemanticsNodes().isNotEmpty()
            }
        }
    }

    @Test
    fun tappingTheBackArrowReturnsToTheDashboard() {
        launchAndOpenDetail().use {
            composeRule.onNodeWithContentDescription(string(R.string.content_back)).performClick()
            composeRule.waitUntil(timeoutMillis = 10_000) {
                composeRule.onAllNodesWithText(string(R.string.dashboard_title)).fetchSemanticsNodes().isNotEmpty()
            }
        }
    }

    @Test
    fun theBackArrowSitsAtTheLeadingEdgeOfTheHeader() {
        launchAndOpenDetail().use {
            val back = composeRule.onNodeWithContentDescription(string(R.string.content_back))
                .getUnclippedBoundsInRoot()
            val identity = composeRule.onNodeWithText("Settings").getUnclippedBoundsInRoot()

            assertTrue(
                "the arrow must sit at the header's leading edge (the old inset left the node at ~24 dp): $back",
                back.left.value <= 16f
            )
            assertTrue("the arrow must precede the app identity", back.left < identity.left)
        }
    }
}
