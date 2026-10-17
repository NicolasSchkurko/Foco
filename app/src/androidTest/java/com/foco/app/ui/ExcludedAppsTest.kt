package com.foco.app.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.foco.app.MainActivity
import com.foco.app.R
import com.foco.app.data.ExcludedAppsStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Mantener presionado excluye la app del ranking (el total no cambia); Deshacer
 * la trae de vuelta; y la exclusión persiste al relanzar. E2E con uso real
 * (Settings) en la API 36. Los textos de la app se resuelven de recursos:
 * el test no depende del idioma del device.
 */
@RunWith(AndroidJUnit4::class)
class ExcludedAppsTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

    private fun string(resId: Int): String = targetContext.resources.getString(resId)

    private val usageLinePrefix = string(R.string.dashboard_usage_line).substringBefore(":")

    private fun shell(command: String) {
        InstrumentationRegistry.getInstrumentation()
            .uiAutomation
            .executeShellCommand(command)
            .close()
    }

    private fun headerText(): String =
        composeRule.onAllNodesWithText(usageLinePrefix, substring = true)
            .fetchSemanticsNodes()
            .first()
            .config
            .getOrNull(SemanticsProperties.Text)
            ?.joinToString()
            .orEmpty()

    @After
    fun cleanUp() {
        // No dejar exclusiones pegadas en el device.
        runBlocking { ExcludedAppsStore(targetContext).remove(SETTINGS_PACKAGE) }
    }

    @Test
    fun longPressExcludesUndoRestoresAndTheExclusionPersists() {
        shell("appops set com.foco.app android:get_usage_stats allow")
        shell("am start -a android.settings.SETTINGS")
        Thread.sleep(2_000)
        shell("input keyevent KEYCODE_HOME")
        Thread.sleep(500)

        var scenario = ActivityScenario.launch(MainActivity::class.java)

        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText("Settings").fetchSemanticsNodes().isNotEmpty()
        }
        val totalBefore = headerText()

        // Mantener presionado -> excluida del ranking, total intacto.
        composeRule.onNodeWithText("Settings").performTouchInput { longClick() }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Settings").fetchSemanticsNodes().isEmpty()
        }
        assertEquals(totalBefore, headerText())

        // Deshacer -> vuelve al ranking.
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(string(R.string.excluded_undo)).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(string(R.string.excluded_undo)).performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Settings").fetchSemanticsNodes().isNotEmpty()
        }

        // Excluir de nuevo y relanzar: la exclusión persiste.
        composeRule.onNodeWithText("Settings").performTouchInput { longClick() }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Settings").fetchSemanticsNodes().isEmpty()
        }
        scenario.close()

        scenario = ActivityScenario.launch(MainActivity::class.java)
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText(string(R.string.loading)).fetchSemanticsNodes().isEmpty()
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Settings").fetchSemanticsNodes().isEmpty()
        }
        scenario.close()
    }

    private companion object {
        const val SETTINGS_PACKAGE = "com.android.settings"
    }
}
