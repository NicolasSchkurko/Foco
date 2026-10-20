package com.foco.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.foco.app.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The entry gate with an injected predicate (deterministic): dashboard or onboarding per the permission and
 * the re-check on returning to the foreground, both ways; the texts come from resources.
 */
@RunWith(AndroidJUnit4::class)
class AppEntrySeamTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

    private fun string(resId: Int): String = targetContext.resources.getString(resId)

    private fun hasText(text: String): Boolean =
        try {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        } catch (e: IllegalStateException) {
            false
        }

    private fun awaitText(text: String) {
        composeRule.waitUntil(timeoutMillis = 10_000) { hasText(text) }
    }

    /** The simulated "real" permission: the test flips it back and forth. */
    private var granted = false

    /** The simulated accessibility binding: the test flips it back and forth. */
    private var accessibilityGranted = true

    private fun showGate() {
        composeRule.setContent {
            App(
                isUsageAccessGranted = { granted },
                isAccessibilityGranted = { accessibilityGranted },
                startupLoad = {},
            )
        }
    }

    private fun leaveAndReturn() {
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        composeRule.waitForIdle()
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.waitForIdle()
    }

    @Test
    fun grantedAtStartShowsTheDashboard() {
        granted = true
        showGate()
        awaitText(string(R.string.dashboard_title))
    }

    @Test
    fun missingAtStartShowsTheOnboardingWithTheSettingsShortcut() {
        granted = false
        showGate()
        awaitText(string(R.string.onboarding_open_settings))
        awaitText(string(R.string.onboarding_explanation))
    }

    @Test
    fun grantedWhileAwayShowsTheDashboard() {
        granted = false
        showGate()
        awaitText(string(R.string.onboarding_open_settings))

        granted = true
        leaveAndReturn()
        awaitText(string(R.string.dashboard_title))
    }

    @Test
    fun revokedWhileAwayShowsTheOnboarding() {
        granted = true
        showGate()
        awaitText(string(R.string.dashboard_title))

        granted = false
        leaveAndReturn()
        awaitText(string(R.string.onboarding_open_settings))
    }

    @Test
    fun usageGrantedWithoutAccessibilityShowsTheAccessibilityOnboarding() {
        granted = true
        accessibilityGranted = false
        showGate()

        awaitText(string(R.string.onboarding_accessibility_open_settings))
        awaitText(string(R.string.onboarding_accessibility_explanation))
    }

    @Test
    fun accessibilityGrantedWhileAwayShowsTheDashboard() {
        granted = true
        accessibilityGranted = false
        showGate()
        awaitText(string(R.string.onboarding_accessibility_open_settings))

        accessibilityGranted = true
        leaveAndReturn()
        awaitText(string(R.string.dashboard_title))
    }

    @Test
    fun revokingAccessibilityWhileAwayShowsItsOnboarding() {
        granted = true
        accessibilityGranted = true
        showGate()
        awaitText(string(R.string.dashboard_title))

        accessibilityGranted = false
        leaveAndReturn()
        awaitText(string(R.string.onboarding_accessibility_open_settings))
    }
}
