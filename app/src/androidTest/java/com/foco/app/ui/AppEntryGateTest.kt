package com.foco.app.ui

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.foco.app.MainActivity
import com.foco.app.R
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * El gate de entrada E2E, contra el AppOp real (`appops`): frío con permiso →
 * dashboard; frío sin permiso → onboarding; cambios en vivo al volver.
 * El `appops` se setea ANTES del lanzamiento y el cleanup deja el emulador en
 * `allow` (perderlo era justo el artefacto que originó este change).
 */
@RunWith(AndroidJUnit4::class)
class AppEntryGateTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

    private fun string(resId: Int): String = targetContext.resources.getString(resId)

    private fun hasText(text: String): Boolean =
        try {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        } catch (e: IllegalStateException) {
            false
        }

    private fun awaitText(text: String) {
        composeRule.waitUntil(timeoutMillis = 15_000) { hasText(text) }
    }

    private fun shell(command: String) {
        InstrumentationRegistry.getInstrumentation()
            .uiAutomation
            .executeShellCommand(command)
            .close()
    }

    private fun setUsageAccessAllowed(allowed: Boolean) {
        shell("appops set com.foco.app android:get_usage_stats ${if (allowed) "allow" else "ignore"}")
    }

    @After
    fun restorePermission() {
        setUsageAccessAllowed(true)
    }

    private fun leaveAndReturn(scenario: ActivityScenario<MainActivity>) {
        scenario.moveToState(Lifecycle.State.CREATED)
        composeRule.waitForIdle()
        scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.waitForIdle()
    }

    @Test
    fun grantedAtLaunchShowsTheDashboard() {
        setUsageAccessAllowed(true)
        ActivityScenario.launch(MainActivity::class.java).use {
            awaitText(string(R.string.dashboard_title))
        }
    }

    @Test
    fun missingAtLaunchShowsTheOnboarding() {
        setUsageAccessAllowed(false)
        ActivityScenario.launch(MainActivity::class.java).use {
            awaitText(string(R.string.onboarding_open_settings))
        }
    }

    @Test
    fun revokedWhileAwayShowsTheOnboardingOnReturn() {
        setUsageAccessAllowed(true)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            awaitText(string(R.string.dashboard_title))

            setUsageAccessAllowed(false)
            leaveAndReturn(scenario)
            awaitText(string(R.string.onboarding_open_settings))
        }
    }

    @Test
    fun grantedWhileAwayShowsTheDashboardOnReturn() {
        setUsageAccessAllowed(false)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            awaitText(string(R.string.onboarding_open_settings))

            setUsageAccessAllowed(true)
            leaveAndReturn(scenario)
            awaitText(string(R.string.dashboard_title))
        }
    }
}
