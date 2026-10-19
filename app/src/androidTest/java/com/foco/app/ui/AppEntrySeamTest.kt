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
 * El gate de entrada con predicado inyectado (determinista): dashboard u onboarding según el permiso y
 * el re-chequeo al volver a primer plano, en ambos sentidos; los textos se resuelven de recursos.
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

    /** El permiso "real" simulado: el test lo cambia entre idas y vueltas. */
    private var granted = false

    private fun showGate() {
        composeRule.setContent {
            App(isUsageAccessGranted = { granted })
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
}
