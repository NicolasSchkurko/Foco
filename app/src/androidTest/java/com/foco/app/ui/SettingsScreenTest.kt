package com.foco.app.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.foco.app.MainActivity
import com.foco.app.data.AppAppearance
import com.foco.app.data.AppLanguage
import com.foco.app.data.ExcludedAppsStore
import com.foco.app.data.SettingsStore
import com.foco.app.model.AppearanceOption
import com.foco.app.model.LanguageOption
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Settings E2E (API 36): language and appearance apply and persist, exclusions are listed and restored. The stability notes live in `docs/mechanisms/instrumented-tests.md`.
 */
@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

    private fun shell(command: String) {
        InstrumentationRegistry.getInstrumentation()
            .uiAutomation
            .executeShellCommand(command)
            .close()
    }

    @Before
    fun setUp() {
        // The headless AVD may be locked: wake it and dismiss the keyguard.
        shell("input keyevent KEYCODE_WAKEUP")
        shell("wm dismiss-keyguard")
        Thread.sleep(500)

        // Known state for deterministic assertions: English picked (not Automatic).
        runBlocking {
            SettingsStore(targetContext).setLanguage(LanguageOption.EN)
            SettingsStore(targetContext).setAppearance(AppearanceOption.AUTO)
        }
        AppLanguage.applyToSystem(LanguageOption.EN)
        AppAppearance.applyToSystem(AppearanceOption.AUTO)
        shell("appops set com.foco.app android:get_usage_stats allow")
    }

    @After
    fun cleanUp() {
        runBlocking {
            SettingsStore(targetContext).setLanguage(LanguageOption.AUTO)
            SettingsStore(targetContext).setAppearance(AppearanceOption.AUTO)
            ExcludedAppsStore(targetContext).restore(SETTINGS_PACKAGE)
        }
        AppLanguage.applyToSystem(LanguageOption.AUTO)
        AppAppearance.applyToSystem(AppearanceOption.AUTO)
    }

    /** true if any node carries that text; tolerates the hierarchy gap during a re-creation. */
    private fun showsText(text: String): Boolean =
        try {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        } catch (e: IllegalStateException) {
            false
        }

    private fun showsDescription(description: String): Boolean =
        try {
            composeRule.onAllNodesWithContentDescription(description).fetchSemanticsNodes().isNotEmpty()
        } catch (e: IllegalStateException) {
            false
        }

    /**
     * true if the only node with [label] is (or is not) selected; tolerates the hierarchy gap and the
     * overlap of the two hierarchies during a re-creation, so it serves as a wait.
     */
    private fun selectedRow(label: String, selected: Boolean): Boolean =
        try {
            if (selected) composeRule.onNodeWithText(label).assertIsSelected()
            else composeRule.onNodeWithText(label).assertIsNotSelected()
            true
        } catch (e: IllegalStateException) {
            false
        } catch (e: AssertionError) {
            false
        }

    /** Leaves the Settings screen open with the UI in [language] ("es" | "en"). */
    private fun ensureSettings(language: String) {
        val gear = if (language == "es") "Ajustes" else "Settings"
        val section = if (language == "es") "Idioma" else "Language"

        composeRule.waitUntil(timeoutMillis = 15_000) {
            showsText(section) || showsDescription(gear)
        }
        if (!showsText(section)) {
            composeRule.onNodeWithContentDescription(gear).performClick()
            composeRule.waitUntil(timeoutMillis = 10_000) { showsText(section) }
        }
    }

    /** The system language, without the per-app language override. */
    private fun systemLanguage(): String =
        android.content.res.Resources.getSystem().configuration.locales[0].language

    @Test
    fun entryAndBackReturnsToTheDashboard() {
        ActivityScenario.launch(MainActivity::class.java).use {
            ensureSettings(language = "en")

            composeRule.onNodeWithContentDescription("Back").performClick()
            composeRule.waitUntil(timeoutMillis = 10_000) { showsText("Today") }
        }
    }

    @Test
    fun languageSwitchesImmediatelyFollowsTheSystemAndPersists() {
        val systemIsSpanish = systemLanguage() == "es"

        var scenario = ActivityScenario.launch(MainActivity::class.java)
        ensureSettings(language = "en")

        // Picking Spanish: it switches instantly (the re-creation may go back to the start).
        composeRule.onNodeWithText("Spanish").performClick()
        ensureSettings(language = "es")

        // Cold restart: still in Spanish.
        scenario.close()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        ensureSettings(language = "es")

        // Automatic: it follows the system language.
        composeRule.onAllNodesWithText("Automático")[0].performClick()
        ensureSettings(language = if (systemIsSpanish) "es" else "en")

        // Cold restart: still follows the system.
        scenario.close()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        ensureSettings(language = if (systemIsSpanish) "es" else "en")
        scenario.close()
    }

    @Test
    fun appearanceSelectionPersistsAndBackToAutomatic() {
        var scenario = ActivityScenario.launch(MainActivity::class.java)
        ensureSettings(language = "en")

        // Dark: the choice reaches appcompat's night mode (it may re-create the activity).
        composeRule.onNodeWithText("Dark").performClick()
        ensureSettings(language = "en")
        composeRule.waitUntil(timeoutMillis = 10_000) {
            AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        }
        composeRule.waitUntil(timeoutMillis = 10_000) { selectedRow("Dark", selected = true) }
        composeRule.onNodeWithText("Dark").assertIsSelected()

        // Cold restart: still Dark, with the mode already applied at startup.
        scenario.close()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        ensureSettings(language = "en")
        composeRule.waitUntil(timeoutMillis = 10_000) { selectedRow("Dark", selected = true) }
        composeRule.onNodeWithText("Dark").assertIsSelected()
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.getDefaultNightMode())

        // Back to Automatic (the second "Automatic" option = the Appearance section).
        composeRule.onAllNodesWithText("Automatic")[1].performClick()
        ensureSettings(language = "en")
        composeRule.waitUntil(timeoutMillis = 10_000) {
            AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        // "Dark" only exists in the Appearance section: if it is no longer selected, the
        // hierarchy has settled and the chosen option is Automatic.
        composeRule.waitUntil(timeoutMillis = 10_000) { selectedRow("Dark", selected = false) }
        composeRule.onAllNodesWithText("Automatic")[1].assertIsSelected()
        scenario.close()
    }

    @Test
    fun theBackControlSitsAtTheLeadingEdgeAndTheTitleIsCentered() {
        ActivityScenario.launch(MainActivity::class.java).use {
            ensureSettings(language = "en")

            val back = composeRule.onNodeWithContentDescription("Back").getUnclippedBoundsInRoot()
            val title = composeRule.onNodeWithText("Settings").getUnclippedBoundsInRoot()
            val root = composeRule.onRoot().getUnclippedBoundsInRoot()

            // Measured: the control's node sits at ~8 dp with the new inset and ~24 dp with the old one
            // (the glyph, ~8 dp further in: ~16 dp against ~32 dp). The threshold goes in between.
            assertTrue(
                "the arrow must sit at the header's leading edge (the old inset left the node at ~24 dp): $back",
                back.left.value <= 16f
            )
            assertTrue("the arrow must precede the title", back.left < title.left)
            val offCenter = kotlin.math.abs(
                (title.left.value + title.right.value) / 2f - (root.left.value + root.right.value) / 2f
            )
            assertTrue("the title must read centered (off by $offCenter dp)", offCenter <= 16f)
        }
    }

    @Test
    fun excludedAppsAreListedAndRestored() {
        runBlocking { ExcludedAppsStore(targetContext).exclude(SETTINGS_PACKAGE) }

        ActivityScenario.launch(MainActivity::class.java).use {
            ensureSettings(language = "en")

            composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Restore"))
            composeRule.waitUntil(timeoutMillis = 10_000) { showsText("Restore") }
            composeRule.onNodeWithText("Restore").performClick()

            composeRule.waitUntil(timeoutMillis = 10_000) { showsText("No excluded apps.") }
        }
    }

    private companion object {
        const val SETTINGS_PACKAGE = "com.android.settings"
    }
}
