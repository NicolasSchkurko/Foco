package com.foco.app.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Ajustes: entrada/volver; el idioma se aplica al instante, persiste y Automático
 * sigue al sistema; la apariencia se elige, persiste y llega al modo nocturno de
 * appcompat (el fondo de la ventana es pasada visual, no aserción); la lista de
 * excluidas se muestra y restaura. E2E en la API 36.
 *
 * Notas de estabilidad:
 * - Aplicar el idioma recrea la activity por cambio de configuración (appcompat) y
 *   la pantalla puede volver al inicio: [ensureSettings] reabre Ajustes si hace falta.
 * - Aplicar la apariencia también puede recrear la activity (cambio de uiMode), así
 *   que después de elegirla se vuelve a llamar a [ensureSettings].
 * - Durante una recreación hay instantes sin jerarquía Compose: los chequeos usan
 *   [showsText]/[showsDescription], que tratan ese hueco como "todavía no".
 * - Los "reinicios" se hacen con close() + un launch nuevo (no `recreate()`: encadenar
 *   un recreate sobre otro deja colgado el tracking de ActivityScenario).
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
        // El AVD headless puede estar bloqueado: despertar y descartar el keyguard.
        shell("input keyevent KEYCODE_WAKEUP")
        shell("wm dismiss-keyguard")
        Thread.sleep(500)

        // Estado conocido para aserciones deterministas: inglés elegido (no Automático).
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

    /** true si hay algún nodo con ese texto; tolera el hueco sin jerarquía al recrear. */
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
     * true si el único nodo con [label] está (o no) seleccionado. Tolera el hueco sin
     * jerarquía y el empalme de las dos jerarquías durante una recreación (ahí
     * `onNodeWithText` no encuentra un nodo único), así que sirve para esperar.
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

    /** Deja la pantalla de Ajustes abierta con la UI en [language] ("es" | "en"). */
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

    /** El idioma del sistema, sin el override de idioma por app. */
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

        // Elegir Español: cambia al instante (la recreación puede volver al inicio).
        composeRule.onNodeWithText("Spanish").performClick()
        ensureSettings(language = "es")

        // Reinicio en frío: sigue en Español.
        scenario.close()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        ensureSettings(language = "es")

        // Automático: sigue al idioma del sistema.
        composeRule.onAllNodesWithText("Automático")[0].performClick()
        ensureSettings(language = if (systemIsSpanish) "es" else "en")

        // Reinicio en frío: sigue al sistema.
        scenario.close()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        ensureSettings(language = if (systemIsSpanish) "es" else "en")
        scenario.close()
    }

    @Test
    fun appearanceSelectionPersistsAndBackToAutomatic() {
        var scenario = ActivityScenario.launch(MainActivity::class.java)
        ensureSettings(language = "en")

        // Oscuro: la elección llega al modo nocturno de appcompat (puede recrear la activity).
        composeRule.onNodeWithText("Dark").performClick()
        ensureSettings(language = "en")
        composeRule.waitUntil(timeoutMillis = 10_000) {
            AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        }
        composeRule.waitUntil(timeoutMillis = 10_000) { selectedRow("Dark", selected = true) }
        composeRule.onNodeWithText("Dark").assertIsSelected()

        // Reinicio en frío: sigue en Oscuro, con el modo ya aplicado en el arranque.
        scenario.close()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        ensureSettings(language = "en")
        composeRule.waitUntil(timeoutMillis = 10_000) { selectedRow("Dark", selected = true) }
        composeRule.onNodeWithText("Dark").assertIsSelected()
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.getDefaultNightMode())

        // Volver a Automático (segunda opción "Automatic" = sección Apariencia).
        composeRule.onAllNodesWithText("Automatic")[1].performClick()
        ensureSettings(language = "en")
        composeRule.waitUntil(timeoutMillis = 10_000) {
            AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        // "Dark" solo existe en la sección Apariencia: si ya no está seleccionada, la
        // jerarquía quedó asentada y la opción elegida es Automático.
        composeRule.waitUntil(timeoutMillis = 10_000) { selectedRow("Dark", selected = false) }
        composeRule.onAllNodesWithText("Automatic")[1].assertIsSelected()
        scenario.close()
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
