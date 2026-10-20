package com.foco.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.foco.app.R
import com.foco.app.model.day.DayData
import kotlin.math.abs
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The home ring: arrows, both swipes, the calendar control, back retracing and Settings. */
@RunWith(AndroidJUnit4::class)
class DashboardRingTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val liveDay = DayData(sessions = emptyList(), unlockTimes = emptyList())

    private fun string(resId: Int): String = targetContext.resources.getString(resId)

    private fun string(resId: Int, vararg args: Any): String =
        targetContext.resources.getString(resId, *args)

    private fun quantity(resId: Int, count: Int): String =
        targetContext.resources.getQuantityString(resId, count, count)

    private fun showApp() {
        composeRule.setContent { FocoNavHost(loadDay = { liveDay }) }
    }

    private fun awaitText(text: String) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun pressBack() {
        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
    }

    @Test
    fun arrowsStepAndBackRetracesTheVisitedPosition() {
        showApp()
        awaitText(string(R.string.dashboard_title))

        composeRule.onNodeWithContentDescription(string(R.string.day_next)).performClick()
        awaitText(string(R.string.week_title))

        pressBack()
        awaitText(string(R.string.dashboard_title))

        composeRule.onNodeWithContentDescription(string(R.string.day_previous)).performClick()
        awaitText(string(R.string.week_title))
    }

    @Test
    fun horizontalSwipesMoveBothWays() {
        showApp()
        awaitText(string(R.string.dashboard_title))

        composeRule.onNodeWithTag("dashboard_content").performTouchInput { swipeLeft() }
        awaitText(string(R.string.week_title))

        composeRule.onNodeWithTag("dashboard_content").performTouchInput { swipeRight() }
        awaitText(string(R.string.dashboard_title))
    }

    @Test
    fun theCalendarControlOpensTheWeekWithoutABackControl() {
        showApp()
        awaitText(string(R.string.dashboard_title))

        composeRule.onNodeWithContentDescription(string(R.string.week_title)).performClick()
        awaitText(string(R.string.week_title))
        composeRule.onAllNodesWithContentDescription(string(R.string.content_back)).assertCountEquals(0)

        pressBack()
        awaitText(string(R.string.dashboard_title))
    }

    @Test
    fun theUsageLegendSitsBetweenTheArrowsAndTheTitleAlignsWithTheActions() {
        showApp()
        val usage = string(
            R.string.dashboard_usage_line,
            formatDuration(0),
            quantity(R.plurals.dashboard_unlocks, 0)
        )
        awaitText(usage)

        val previous = composeRule.onNodeWithContentDescription(string(R.string.day_previous))
            .getUnclippedBoundsInRoot()
        val next = composeRule.onNodeWithContentDescription(string(R.string.day_next))
            .getUnclippedBoundsInRoot()
        val legend = composeRule.onNodeWithText(usage).getUnclippedBoundsInRoot()
        val title = composeRule.onNodeWithText(string(R.string.dashboard_title))
            .getUnclippedBoundsInRoot()
        val settings = composeRule.onNodeWithContentDescription(string(R.string.settings_title))
            .getUnclippedBoundsInRoot()

        assertTrue(
            "the usage legend must sit between the arrows: $legend",
            legend.left.value >= previous.right.value && legend.right.value <= next.left.value
        )
        val titleCenter = (title.top.value + title.bottom.value) / 2f
        val actionCenter = (settings.top.value + settings.bottom.value) / 2f
        assertTrue(
            "the title must be vertically centered with the actions (off by ${abs(titleCenter - actionCenter)} dp)",
            abs(titleCenter - actionCenter) <= 16f
        )
    }

    @Test
    fun settingsReturnsToThePositionItWasOpenedFrom() {
        showApp()
        awaitText(string(R.string.dashboard_title))

        composeRule.onNodeWithContentDescription(string(R.string.day_next)).performClick()
        awaitText(string(R.string.week_title))

        composeRule.onNodeWithContentDescription(string(R.string.settings_title)).performClick()
        awaitText(string(R.string.settings_title))
        composeRule.onNodeWithContentDescription(string(R.string.content_back)).performClick()

        awaitText(string(R.string.week_title))
    }
}
