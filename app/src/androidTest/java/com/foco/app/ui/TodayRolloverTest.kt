package com.foco.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.foco.app.ui.common.rememberCurrentDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * The current date is a screen input: it must roll over on resume and at midnight while the app is
 * open. The clock is injected so the test never waits for the real midnight.
 */
@RunWith(AndroidJUnit4::class)
class TodayRolloverTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val clock = MutableClock(Instant.parse("2026-10-20T23:59:00Z"), ZoneOffset.UTC)
    private val owner = TestLifecycleOwner()

    private fun show() {
        composeRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                val today = rememberCurrentDate(clock)
                MaterialTheme { Text(today.toString()) }
            }
        }
        composeRule.runOnUiThread { owner.registry.currentState = Lifecycle.State.RESUMED }
    }

    private fun showsText(text: String): Boolean =
        try {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        } catch (_: IllegalStateException) {
            false
        }

    private fun awaitText(text: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) { showsText(text) }
    }

    @Test
    fun resumeAfterMidnightUsesTheNewDay() {
        show()
        awaitText("2026-10-20")

        composeRule.runOnUiThread { owner.registry.currentState = Lifecycle.State.CREATED }
        clock.current = Instant.parse("2026-10-21T00:01:00Z")
        composeRule.runOnUiThread { owner.registry.currentState = Lifecycle.State.RESUMED }

        awaitText("2026-10-21")
    }

    @Test
    fun midnightWhileOpenUsesTheNewDay() {
        clock.current = Instant.parse("2026-10-20T23:59:59.500Z")
        show()
        awaitText("2026-10-20")

        clock.current = Instant.parse("2026-10-21T00:00:00.500Z")

        awaitText("2026-10-21")
    }

    private class TestLifecycleOwner : LifecycleOwner {
        val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle get() = registry
    }

    private class MutableClock(
        initial: Instant,
        private val zone: ZoneId,
    ) : Clock() {
        @Volatile
        var current: Instant = initial

        override fun getZone(): ZoneId = zone

        override fun withZone(zone: ZoneId): Clock = MutableClock(current, zone)

        override fun instant(): Instant = current
    }
}
