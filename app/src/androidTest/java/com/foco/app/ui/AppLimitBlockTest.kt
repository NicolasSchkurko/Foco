package com.foco.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.foco.app.R
import com.foco.app.domain.limits.BlockReason
import com.foco.app.ui.limits.AppLimitBlockScreen
import com.foco.app.ui.limits.OverlayLifecycleOwner
import com.foco.app.ui.theme.FocoTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Category: limit screen (API 36). The countdown is driven by the frame clock. */
@RunWith(AndroidJUnit4::class)
class AppLimitBlockTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /** The strings of the activity: the per-app locale wins over the device one. */
    private val strings get() = composeRule.activity.resources

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

    @Test
    fun theOverlayHostIsRestoredAndCreatedBeforeCompose() {
        val owner = OverlayLifecycleOwner()

        // The lifecycle and the saved-state registry move on the main thread, like production.
        val main = InstrumentationRegistry.getInstrumentation()
        main.runOnMainSync { owner.prepare() }

        assertEquals(Lifecycle.State.CREATED, owner.lifecycle.currentState)
        // Compose makes this call as soon as it composes: it must not throw.
        owner.savedStateRegistry.consumeRestoredStateForKey("block")

        main.runOnMainSync { owner.resume() }
        assertEquals(Lifecycle.State.RESUMED, owner.lifecycle.currentState)

        main.runOnMainSync { owner.destroy() }
        assertEquals(Lifecycle.State.DESTROYED, owner.lifecycle.currentState)
    }

    @Test
    fun aDestroyedOwnerCannotBeResumed() {
        val owner = OverlayLifecycleOwner()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            owner.prepare()
            owner.resume()
            owner.destroy()
        }

        // The host must build a fresh owner per block: reuse would kill the service.
        assertThrows(IllegalStateException::class.java) { owner.resume() }
    }
}
