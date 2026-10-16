package com.foco.app.ui

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.foco.app.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Navegación al detalle: tocar una fila lo abre; el back del sistema vuelve;
 * y la flechita visible del detalle también vuelve. E2E: genera uso real
 * (Settings) antes de lanzar la app.
 */
@RunWith(AndroidJUnit4::class)
class AppDetailNavigationTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private fun shell(command: String) {
        InstrumentationRegistry.getInstrumentation()
            .uiAutomation
            .executeShellCommand(command)
            .close()
    }

    /** Permiso + uso real, lanza la app, toca la fila de Settings y espera el detalle. */
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
            composeRule.onAllNodesWithText("Resumen").fetchSemanticsNodes().isNotEmpty()
        }
        return scenario
    }

    @Test
    fun tappingARowOpensTheDetailAndBackReturns() {
        launchAndOpenDetail().use { scenario ->
            scenario.onActivity { activity -> activity.onBackPressedDispatcher.onBackPressed() }
            composeRule.waitUntil(timeoutMillis = 10_000) {
                composeRule.onAllNodesWithText("Hoy").fetchSemanticsNodes().isNotEmpty()
            }
        }
    }

    @Test
    fun tappingTheBackArrowReturnsToTheDashboard() {
        launchAndOpenDetail().use {
            composeRule.onNodeWithText("←").performClick()
            composeRule.waitUntil(timeoutMillis = 10_000) {
                composeRule.onAllNodesWithText("Hoy").fetchSemanticsNodes().isNotEmpty()
            }
        }
    }
}
