package com.foco.app.ui.dashboard

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.foco.app.model.day.DayData
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger

/**
 * The dashboard refreshes on its own: when shown, on every return to the foreground and every
 * [DEFAULT_REFRESH_INTERVAL_MILLIS] while visible; never in the background and with no manual button.
 */
@RunWith(AndroidJUnit4::class)
class DashboardRefreshTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val loads = AtomicInteger(0)

    private fun showDashboard(refreshIntervalMillis: Long) {
        composeRule.setContent {
            DashboardScreen(
                refreshIntervalMillis = refreshIntervalMillis,
                loadDay = {
                    loads.incrementAndGet()
                    DayData(sessions = emptyList(), unlockTimes = emptyList())
                },
            )
        }
    }

    private fun waitForLoadsAtLeast(count: Int) {
        composeRule.waitUntil(timeoutMillis = 10_000) { loads.get() >= count }
    }

    @Test
    fun loadsWhenShownAndAgainOnEachReturnToForeground() {
        showDashboard(refreshIntervalMillis = 60_000)
        waitForLoadsAtLeast(1)
        val afterFirstShow = loads.get()

        // How to return from another app: it goes to the background and comes back.
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        composeRule.waitForIdle()
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)

        composeRule.waitUntil(timeoutMillis = 10_000) { loads.get() > afterFirstShow }
    }

    @Test
    fun refreshesPeriodicallyWhileVisible() {
        showDashboard(refreshIntervalMillis = 150)
        waitForLoadsAtLeast(3)
        assertTrue(loads.get() >= 3)
    }

    @Test
    fun doesNotRefreshInTheBackground() {
        showDashboard(refreshIntervalMillis = 100)
        waitForLoadsAtLeast(2)

        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        val frozen = loads.get()
        composeRule.waitForIdle()

        // Six intervals at 100 ms: wait for a refresh event instead of sleeping through them.
        val refreshed =
            try {
                composeRule.waitUntil(timeoutMillis = 600) { loads.get() > frozen }
                true
            } catch (_: ComposeTimeoutException) {
                false
            }
        assertFalse("the dashboard must not refresh in the background", refreshed)
    }

    @Test
    fun hasNoManualRefreshButton() {
        showDashboard(refreshIntervalMillis = 60_000)
        waitForLoadsAtLeast(1)

        composeRule.onNodeWithText("Actualizar").assertDoesNotExist()
    }
}
