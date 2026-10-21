package com.foco.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.test.platform.app.InstrumentationRegistry
import com.foco.app.R
import com.foco.app.data.SessionHistoryStore
import com.foco.app.model.UsageSession
import com.foco.app.model.day.DayData
import com.foco.app.ui.detail.AppDetailScreen
import com.foco.app.ui.detail.DETAIL_CONTENT_TAG
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

    private fun appString(resId: Int): String = compose.activity.resources.getString(resId)

    @Before
    fun seedTheDay() {
        // The limits preview only renders with a loaded day; the store is the source of it.
        val day = LocalDate.now().minusDays(1)
        val from = day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() + 9 * HOUR
        val store =
            SessionHistoryStore.forApp(targetContext) { date ->
                if (date == day || date == day.minusDays(1)) {
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

    @Test fun aHorizontalSwipeWalksTheDays() {
        // The window only holds the days the store has, so yesterday must be stored.
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

        // The detail opens on the stored day: the live day has no usage on the device.
        compose.setContent {
            MaterialTheme { AppDetailScreen(packageName = PACKAGE, date = day) }
        }
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText(appString(R.string.day_yesterday)).fetchSemanticsNodes().isNotEmpty()
        }

        // A swipe right walks back to today, the way the home's ring walks its positions.
        // The touch injection is not always delivered on the first try: swipe until the day moves.
        var moved = false
        repeat(3) {
            if (moved) return@repeat
            compose.onNodeWithTag(DETAIL_CONTENT_TAG).performTouchInput { swipeRight() }
            compose.waitUntil(timeoutMillis = 3_000) {
                compose.onAllNodesWithText(appString(R.string.day_today)).fetchSemanticsNodes().isNotEmpty()
            }
            moved = compose.onAllNodesWithText(appString(R.string.day_today)).fetchSemanticsNodes().isNotEmpty()
        }

        assertTrue("the swipe must walk to the previous day", moved)
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
