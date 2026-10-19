package com.foco.app.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.foco.app.MainActivity
import com.foco.app.R
import com.foco.app.data.SessionHistoryStore
import com.foco.app.model.UsageSession
import com.foco.app.model.day.DayData
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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

    /**
     * A day only the store has still renders its total (this test seeds it past the platform reader),
     * and a day stored while the screen is open becomes reachable when the screen is resumed.
     */
    @Test
    fun aDayOnlyTheStoreHasStillShowsItsTotal() {
        val hourMillis = 3_600_000L
        val seededDay = LocalDate.now().minusDays(3)
        val start = seededDay.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() + 9 * hourMillis
        val end = start + hourMillis
        seedInStore(seededDay, start, end)

        launchAndOpenDetail().use { scenario ->
            // One step back is yesterday, a day the store may have nothing for: the window is walked
            // day by day. A selector that offered only the stored days would land on the seeded day.
            tapBackArrow()
            val yesterday = targetContext.getString(R.string.day_yesterday)
            composeRule.waitUntil(timeoutMillis = 10_000) {
                composeRule.onAllNodesWithText(yesterday).fetchSemanticsNodes().isNotEmpty()
            }

            // Two more steps reach the seeded day.
            repeat(2) { tapBackArrow() }

            val storedTotal = targetContext.getString(R.string.detail_day_usage, formatDuration(end - start))
            composeRule.waitUntil(timeoutMillis = 10_000) {
                composeRule.onAllNodesWithText(storedTotal).fetchSemanticsNodes().isNotEmpty()
            }

            val sessionRow = "${formatTimeOfDay(start)} – ${formatTimeOfDay(end)}"
            assertTrue(
                "the stored session of the day must be listed: $sessionRow",
                composeRule.onAllNodesWithText(sessionRow).fetchSemanticsNodes().isNotEmpty()
            )

            // A day stored while the detail is already open (exactly what the startup backfill does)
            // must be picked up when the screen comes back, without restarting the app.
            val floor = LocalDate.now().minusDays(6)
            val floorStart = floor.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            seedInStore(floor, floorStart, floorStart + hourMillis)
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.moveToState(Lifecycle.State.RESUMED)

            var steps = 0
            while (steps < 4) {
                tapBackArrow() // the first call waits for the re-read: the arrow is disabled until then
                steps++
                if (!backArrowEnabled()) break
            }

            assertEquals("the stored window must grow on resume", 3, steps)
            composeRule.onNodeWithText("‹").assertIsNotEnabled()
        }
    }

    /** Stores one day's session in the app's own store, behind the platform reader. */
    private fun seedInStore(day: LocalDate, from: Long, to: Long) {
        val store = SessionHistoryStore.forApp(targetContext) { date ->
            if (date == day) {
                DayData(listOf(UsageSession("com.android.settings", from, to)), unlockTimes = emptyList())
            } else {
                DayData(sessions = emptyList(), unlockTimes = emptyList())
            }
        }
        runBlocking { store.consolidate(LocalDate.now()) }
    }

    /** Whether the back arrow can still walk one day back. */
    private fun backArrowEnabled(): Boolean =
        !composeRule.onNodeWithText("‹").fetchSemanticsNode()
            .config.contains(SemanticsProperties.Disabled)

    /** Waits for the back arrow to be enabled, taps it once and lets the screen settle. */
    private fun tapBackArrow() {
        composeRule.waitUntil(timeoutMillis = 10_000) { backArrowEnabled() }
        composeRule.onNodeWithText("‹").performClick()
        composeRule.waitForIdle()
    }
}
