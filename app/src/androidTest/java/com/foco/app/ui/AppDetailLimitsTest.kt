package com.foco.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.foco.app.R
import com.foco.app.data.SessionHistoryStore
import com.foco.app.model.UsageSession
import com.foco.app.model.day.DayData
import com.foco.app.ui.detail.AppDetailScreen
import com.foco.app.ui.detail.LIMITS_PREVIEW_TAG
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class AppDetailLimitsTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

    /** The strings of the activity: the per-app locale wins over the device one. */
    private val strings get() = compose.activity.resources

    @Before
    fun seedTheDay() {
        // The limits preview only renders with a loaded day; the store is the source of it.
        val day = LocalDate.now().minusDays(1)
        val from = day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() + 9 * HOUR
        val store =
            SessionHistoryStore.forApp(targetContext) { date ->
                if (date == day) {
                    DayData(listOf(UsageSession(PACKAGE, from, from + HOUR)), unlockTimes = emptyList())
                } else {
                    emptyDay
                }
            }
        runBlocking { store.consolidate(LocalDate.now()) }
    }

    @Test fun previewShowsValues() {
        compose.setContent {
            MaterialTheme {
                AppDetailScreen(
                    packageName = PACKAGE,
                    date = LocalDate.now().minusDays(1),
                )
            }
        }
        compose.onNodeWithTag(LIMITS_PREVIEW_TAG).assertIsDisplayed()
    }

    @Test fun theLimitsIconOpensTheDedicatedSettings() {
        var opened = false
        compose.setContent {
            MaterialTheme {
                AppDetailScreen(
                    packageName = PACKAGE,
                    date = LocalDate.now().minusDays(1),
                    onConfigure = { opened = true },
                )
            }
        }

        compose
            .onNodeWithContentDescription(strings.getString(R.string.detail_limits_configure))
            .performClick()

        assertTrue(opened)
    }

    @Test fun tappingTheLimitsBlockOpensTheDedicatedSettings() {
        var opened = false
        compose.setContent {
            MaterialTheme {
                AppDetailScreen(
                    packageName = PACKAGE,
                    date = LocalDate.now().minusDays(1),
                    onConfigure = { opened = true },
                )
            }
        }

        // The whole preview block is a shortcut to the app's limit settings.
        compose.onNodeWithTag(LIMITS_PREVIEW_TAG).performClick()

        assertTrue(opened)
    }

    private companion object {
        const val PACKAGE = "com.foco.app"
        const val HOUR = 3_600_000L
        val emptyDay = DayData(sessions = emptyList(), unlockTimes = emptyList())
    }
}
