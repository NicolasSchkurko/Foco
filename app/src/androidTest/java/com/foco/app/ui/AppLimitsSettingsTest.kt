package com.foco.app.ui

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.foco.app.R
import com.foco.app.ui.settings.appLimitsSection
import com.foco.app.ui.theme.FocoTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Category: settings limits (API 36). The section lists apps and persists the picked minutes. */
@RunWith(AndroidJUnit4::class)
class AppLimitsSettingsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val strings = InstrumentationRegistry.getInstrumentation().targetContext.resources

    private fun showSection(picked: (Pair<String, Int?>?) -> Unit) {
        composeRule.setContent {
            FocoTheme {
                LazyColumn {
                    appLimitsSection(
                        apps = listOf("com.example.one" to "One"),
                        limits = mapOf("com.example.one" to 30),
                        onLimitChange = { packageName, minutes -> picked(packageName to minutes) }
                    )
                }
            }
        }
    }

    @Test
    fun theRowShowsItsCurrentLimitAndPicksAStep() {
        var picked: Pair<String, Int?>? = null
        showSection { picked = it }

        composeRule.onNodeWithText("One").assertIsDisplayed()
        composeRule.onNodeWithText("30 min").assertIsDisplayed()

        composeRule.onNodeWithText("One").performClick()
        composeRule.onNodeWithText("45 min").performClick()

        assertEquals("com.example.one" to 45, picked)
    }

    @Test
    fun noLimitClearsTheLimit() {
        var picked: Pair<String, Int?>? = null
        showSection { picked = it }

        composeRule.onNodeWithText("One").performClick()
        composeRule.onNodeWithText(strings.getString(R.string.settings_limit_none)).performClick()

        assertEquals("com.example.one", picked?.first)
        assertNull(picked?.second)
    }

    @Test
    fun blockImmediatelyPicksZero() {
        var picked: Pair<String, Int?>? = null
        showSection { picked = it }

        composeRule.onNodeWithText("One").performClick()
        composeRule.onNodeWithText(
            strings.getString(R.string.settings_limit_block_now)
        ).performClick()

        assertEquals("com.example.one" to 0, picked)
    }
}
