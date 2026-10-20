package com.foco.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.foco.app.R
import com.foco.app.domain.BlockReason
import com.foco.app.ui.limits.AppLimitBlockScreen
import com.foco.app.ui.theme.FocoTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Category: limit screen (API 36). The countdown is driven by the frame clock. */
@RunWith(AndroidJUnit4::class)
class AppLimitBlockTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val strings = InstrumentationRegistry.getInstrumentation().targetContext.resources

    @Test
    fun namesTheAppStatesTheLimitCountsDownAndFinishes() {
        var finished = false
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            FocoTheme {
                AppLimitBlockScreen(appName = "Instagram", onFinished = { finished = true })
            }
        }

        composeRule.onNodeWithText(strings.getString(R.string.limit_block_title)).assertIsDisplayed()
        composeRule
            .onNodeWithText(
                strings.getString(R.string.limit_block_message, "Instagram"),
            ).assertIsDisplayed()
        composeRule
            .onNodeWithText(
                strings.getString(R.string.limit_block_countdown, 3),
            ).assertIsDisplayed()

        composeRule.mainClock.advanceTimeBy(1_100)
        composeRule
            .onNodeWithText(
                strings.getString(R.string.limit_block_countdown, 2),
            ).assertIsDisplayed()

        composeRule.mainClock.advanceTimeBy(1_100)
        composeRule
            .onNodeWithText(
                strings.getString(R.string.limit_block_countdown, 1),
            ).assertIsDisplayed()

        composeRule.mainClock.advanceTimeBy(1_100)
        composeRule.waitForIdle()
        assertTrue("the block must finish after the countdown", finished)
    }

    @Test
    fun outsideHoursShowsItsOwnMessage() {
        composeRule.setContent {
            FocoTheme {
                AppLimitBlockScreen(
                    appName = "Instagram",
                    reason = BlockReason.OUTSIDE_WINDOW,
                    onFinished = {},
                )
            }
        }

        composeRule.onNodeWithText(strings.getString(R.string.window_block_title)).assertIsDisplayed()
        composeRule
            .onNodeWithText(
                strings.getString(R.string.window_block_message, "Instagram"),
            ).assertIsDisplayed()
    }
}
