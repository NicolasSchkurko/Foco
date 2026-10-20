package com.foco.app.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.foco.app.MainActivity
import com.foco.app.data.AppIdentity
import com.foco.app.data.AppAppearance
import com.foco.app.data.AppLanguage
import com.foco.app.data.ExcludedAppsStore
import com.foco.app.data.SettingsStore
import com.foco.app.model.AppearanceOption
import com.foco.app.model.LanguageOption
import kotlinx.coroutines.flow.first
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

    /** Opens the dropdown of the row titled [title] and picks [option]. */
    private fun pickOption(title: String, option: String) {
        composeRule.onNodeWithText(title).performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(option).fetchSemanticsNodes().isNotEmpty()
        }
        // Picking a language re-creates the activity mid-gesture: the injection may fail after the
        // effect is delivered, so the caller asserts the result (the row or the locale) instead.
        composeRule.onAllNodesWithText(option).onFirst().performClick()
    }

    /** Leaves the Settings screen open with the UI in [language] ("es" | "en"). */
    private fun ensureSettings(language: String) {
        val gear = if (language == "es") "Ajustes" else "Settings"
        val section = if (language == "es") "Idioma" else "Language"

        composeRule.waitUntil(timeoutMillis = 20_000) {
            showsText(section) || showsDescription(gear)
        }
        if (!showsText(section)) {
            runCatching { composeRule.onNodeWithContentDescription(gear).performClick() }
            composeRule.waitUntil(timeoutMillis = 15_000) { showsText(section) }
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
    fun languagePicksThroughTheDropdownPersist() {
        var scenario = ActivityScenario.launch(MainActivity::class.java)
        ensureSettings(language = "en")

        // Spanish: the pick reaches the store and the screen settles in it.
        pickOption("Language", "Spanish")
        waitForLanguage(LanguageOption.ES)
        ensureSettings(language = "es")

        // Cold restart: still Spanish.
        scenario.close()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        ensureSettings(language = "es")

        // Automatic: the pick reaches the store again.
        pickOption("Idioma", "Automático")
        waitForLanguage(LanguageOption.AUTO)
        scenario.close()
    }

    /** Waits until the persisted language is [option]; the UI may be re-creating meanwhile. */
    private fun waitForLanguage(option: LanguageOption) {
        composeRule.waitUntil(timeoutMillis = 15_000) {
            runBlocking { SettingsStore(targetContext).language.first() } == option
        }
    }

@Test
    fun appearanceSelectionPersistsAndBackToAutomatic() {
        var scenario = ActivityScenario.launch(MainActivity::class.java)
        ensureSettings(language = "en")

        // Dark: the choice reaches appcompat's night mode (it may re-create the activity).
        pickOption("Appearance", "Dark")
        ensureSettings(language = "en")
        composeRule.waitUntil(timeoutMillis = 10_000) {
            AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        }
        composeRule.waitUntil(timeoutMillis = 10_000) { showsText("Dark") }
        

        // Cold restart: still Dark, with the mode already applied at startup.
        scenario.close()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        ensureSettings(language = "en")
        composeRule.waitUntil(timeoutMillis = 10_000) { showsText("Dark") }
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.getDefaultNightMode())

        // Back to Automatic through the Appearance dropdown.
        pickOption("Appearance", "Automatic")
        ensureSettings(language = "en")
        composeRule.waitUntil(timeoutMillis = 10_000) {
            AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
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

    @Test
    fun theAboutBlockShowsTheInstalledVersionAboveLanguage() {
        ActivityScenario.launch(MainActivity::class.java).use {
            ensureSettings(language = "en")

            val identity = AppIdentity(targetContext)
            val version = identity.versionName()!!

            composeRule.onNodeWithText("Made by Nicolas Schkurko").assertIsDisplayed()
            composeRule.onNodeWithText("Version $version").assertIsDisplayed()

            val about = composeRule.onNodeWithText("Made by Nicolas Schkurko").getUnclippedBoundsInRoot()
            val language = composeRule.onNodeWithText("Language").getUnclippedBoundsInRoot()
            assertTrue("the About block must sit above Language", about.top < language.top)

            val intent = identity.repositoryIntent()
            assertEquals(Intent.ACTION_VIEW, intent.action)
            assertEquals(AppIdentity.REPOSITORY_URL, intent.data.toString())
        }
    }

    private companion object {
        const val SETTINGS_PACKAGE = "com.android.settings"
    }
}
