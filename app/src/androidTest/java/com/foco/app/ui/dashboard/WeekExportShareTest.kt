package com.foco.app.ui.dashboard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.foco.app.R
import com.foco.app.ui.theme.FocoTheme
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

/**
 * Category: export UI (API 36). A failed export shows the error on the week position and shares no
 * partial file.
 */
@RunWith(AndroidJUnit4::class)
class WeekExportShareTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun aFailingExportShowsTheErrorAndSharesNoFile() {
        val file = File(context.cacheDir, "week-exports/foco-week-${LocalDate.now()}.json")
        file.delete()

        composeRule.setContent {
            FocoTheme {
                DashboardScreen(loadExport = { throw IllegalStateException("history busy") })
            }
        }

        composeRule
            .onNodeWithContentDescription(context.getString(R.string.week_title))
            .performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule
                .onAllNodesWithContentDescription(context.getString(R.string.week_export))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule
            .onNodeWithContentDescription(context.getString(R.string.week_export))
            .performClick()

        composeRule
            .onNodeWithText(context.getString(R.string.week_export_error))
            .assertIsDisplayed()
        assertFalse(file.exists())
    }
}
