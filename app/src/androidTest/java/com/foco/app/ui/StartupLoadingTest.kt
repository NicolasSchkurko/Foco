package com.foco.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.foco.app.R
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The startup gate: the home is not mounted until the history load finishes. */
@RunWith(AndroidJUnit4::class)
class StartupLoadingTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val load = CompletableDeferred<Unit>()

    private fun string(resId: Int): String = targetContext.resources.getString(resId)

    private fun hasText(text: String): Boolean =
        try {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        } catch (failure: IllegalStateException) {
            false
        }

    @Test
    fun theHomeWaitsForTheStartupLoad() {
        composeRule.setContent {
            App(
                isUsageAccessGranted = { true },
                startupLoad = { load.await() },
            )
        }

        composeRule.waitUntil(timeoutMillis = 10_000) { hasText(string(R.string.loading)) }
        assertTrue(
            "the home must not be mounted while the startup load runs",
            !hasText(string(R.string.dashboard_title)),
        )

        load.complete(Unit)

        composeRule.waitUntil(timeoutMillis = 10_000) {
            hasText(string(R.string.dashboard_title))
        }
    }
}
