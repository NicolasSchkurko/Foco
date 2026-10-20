package com.foco.app.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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
 * The dashboard subtitle counts today's unlocks (`KEYGUARD_HIDDEN` marks from the same daily query).
 * E2E: it generates 2 real unlocks through the shell (SLEEP -> WAKEUP -> MENU); texts from resources.
 */
@RunWith(AndroidJUnit4::class)
class UnlockCountTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

    private val usageLinePrefix =
        targetContext.resources.getString(R.string.dashboard_usage_line).substringBefore(":")

    /** "desbloqueo"/"unlock": the root that appears in the subtitle in any language. */
    private val unlockWord = targetContext.resources
        .getQuantityString(R.plurals.dashboard_unlocks, 2, 2)
        .replace(Regex("\\d+"), "")
        .trim()
        .removeSuffix("s")

    private fun shell(command: String) {
        InstrumentationRegistry.getInstrumentation()
            .uiAutomation
            .executeShellCommand(command)
            .close()
    }

    private fun generateUnlocks(times: Int) {
        repeat(times) {
            shell("input keyevent KEYCODE_SLEEP")
            waitForInteractive(composeRule, interactive = false)
            shell("input keyevent KEYCODE_WAKEUP")
            waitForInteractive(composeRule, interactive = true)
            shell("input keyevent 82") // MENU descarta el keyguard
            waitForKeyguardUnlocked(composeRule)
        }
        shell("input keyevent KEYCODE_WAKEUP")
        shell("wm dismiss-keyguard")
        waitForKeyguardUnlocked(composeRule)
    }

    @Test
    fun subtitleShowsTheDaysUnlockCount() {
        // Known state: on a keyguard that is already up, SLEEP/WAKEUP does not always land on it again.
        shell("input keyevent KEYCODE_WAKEUP")
        shell("wm dismiss-keyguard")
        waitForDeviceAwakeAndUnlocked(composeRule)

        setAccessibilityServiceEnabled(true)
        shell("appops set com.foco.app android:get_usage_stats allow")
        generateUnlocks(times = 2)

        ActivityScenario.launch(MainActivity::class.java).use {
            composeRule.waitUntil(timeoutMillis = 15_000) {
                composeRule.onAllNodesWithText(unlockWord, substring = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }

            val subtitle = composeRule.onAllNodesWithText(usageLinePrefix, substring = true)
                .fetchSemanticsNodes()
                .firstOrNull()
                ?.config?.getOrNull(SemanticsProperties.Text)
                ?.joinToString(" ") { it.text }
                .orEmpty()

            val unlocks = Regex("(\\d+)\\s+" + Regex.escape(unlockWord))
                .find(subtitle)?.groupValues?.get(1)?.toIntOrNull()
            assertTrue(
                "the subtitle must show the unlock count with N >= 2 (saw: '$subtitle')",
                unlocks != null && unlocks >= 2
            )
        }
    }
}
