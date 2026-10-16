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
 * Tocar una fila del ranking abre el detalle de esa app; el back del sistema
 * vuelve al dashboard. E2E: genera uso real (Settings) antes de lanzar la app.
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

    @Test
    fun tappingARowOpensTheDetailAndBackReturns() {
        // Permiso + un poco de uso real para que exista al menos una fila.
        shell("appops set com.foco.app android:get_usage_stats allow")
        shell("am start -a android.settings.SETTINGS")
        Thread.sleep(2_000)
        shell("input keyevent KEYCODE_HOME")
        Thread.sleep(500)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            // Espera la fila de Settings y la toca.
            composeRule.waitUntil(timeoutMillis = 15_000) {
                composeRule.onAllNodesWithText("Settings").fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNodeWithText("Settings").performClick()

            // El detalle abrió: está el encabezado del resumen.
            composeRule.waitUntil(timeoutMillis = 10_000) {
                composeRule.onAllNodesWithText("Resumen").fetchSemanticsNodes().isNotEmpty()
            }

            // Back del sistema → dashboard otra vez.
            scenario.onActivity { activity -> activity.onBackPressedDispatcher.onBackPressed() }
            composeRule.waitUntil(timeoutMillis = 10_000) {
                composeRule.onAllNodesWithText("Hoy").fetchSemanticsNodes().isNotEmpty()
            }
        }
    }
}
