package com.foco.app.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.foco.app.MainActivity
import com.foco.app.R
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * El subtítulo del dashboard cuenta los desbloqueos de hoy (marcas `KEYGUARD_HIDDEN`
 * de la misma consulta diaria). E2E: genera 2 desbloqueos reales por shell
 * (SLEEP → WAKEUP → MENU descarta el keyguard) y lee el número del subtítulo.
 * Los textos se resuelven de recursos: el test no depende del idioma del device.
 */
@RunWith(AndroidJUnit4::class)
class UnlockCountTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

    private val usageLinePrefix =
        targetContext.resources.getString(R.string.dashboard_usage_line).substringBefore(":")

    /** "desbloqueo"/"unlock": la raíz que aparece en el subtítulo en cualquier idioma. */
    private val unlockWord = targetContext.resources
        .getQuantityString(R.plurals.dashboard_unlocks, 2, 2)
        .replace(Regex("\\d+"), "")
        .trim()
        .removeSuffix("s")

    private fun shell(command: String) {
        InstrumentationRegistry.getInstrumentation()
            .uiAutomation
            .executeShellCommand(command)
            .close()
    }

    private fun generateUnlocks(times: Int) {
        repeat(times) {
            shell("input keyevent KEYCODE_SLEEP")
            Thread.sleep(800)
            shell("input keyevent KEYCODE_WAKEUP")
            Thread.sleep(800)
            shell("input keyevent 82") // MENU descarta el keyguard
            Thread.sleep(800)
        }
        shell("input keyevent KEYCODE_WAKEUP")
        shell("wm dismiss-keyguard")
        Thread.sleep(500)
    }

    @Test
    fun subtitleShowsTheDaysUnlockCount() {
        shell("appops set com.foco.app android:get_usage_stats allow")
        generateUnlocks(times = 2)

        ActivityScenario.launch(MainActivity::class.java).use {
            composeRule.waitUntil(timeoutMillis = 15_000) {
                composeRule.onAllNodesWithText(unlockWord, substring = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }

            val subtitle = composeRule.onAllNodesWithText(usageLinePrefix, substring = true)
                .fetchSemanticsNodes()
                .firstOrNull()
                ?.config?.getOrNull(SemanticsProperties.Text)
                ?.joinToString(" ") { it.text }
                .orEmpty()

            val unlocks = Regex("(\\d+)\\s+" + Regex.escape(unlockWord))
                .find(subtitle)?.groupValues?.get(1)?.toIntOrNull()
            assertTrue(
                "el subtítulo debe mostrar el conteo de desbloqueos con N >= 2 (vi: '$subtitle')",
                unlocks != null && unlocks >= 2
            )
        }
    }
}
