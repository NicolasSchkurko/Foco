package com.foco.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.test.platform.app.InstrumentationRegistry
import com.foco.app.R
import com.foco.app.data.SessionHistoryStore
import com.foco.app.model.UsageSession
import com.foco.app.model.day.DayData
import com.foco.app.ui.detail.AppDetailScreen
import com.foco.app.ui.detail.DAY_LABEL_TAG
import com.foco.app.ui.detail.DETAIL_CONTENT_TAG
import com.foco.app.ui.detail.LIMITS_PREVIEW_TAG
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class AppDetailLimitsTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val store: SessionHistoryStore by lazy { SessionHistoryStore.forApp(targetContext) { emptyDay } }

    /** The strings of the activity: the per-app locale wins over the device one. */
    private val strings get() = compose.activity.resources

    private fun appString(resId: Int): String = compose.activity.resources.getString(resId)

    /** A day and a package the store really holds, inside the detail's seven-day window. */
    private fun storedDayAndPackage(): Pair<LocalDate, String>? {
        val today = LocalDate.now()
        val days = runBlocking { store.storedDaysFlow().firstOrNull() }.orEmpty()
        for (day in days.filter { it <= today && it >= today.minusDays(6) }.sortedDescending()) {
            val pkg = runBlocking { store.sessionsFor(day) }.firstOrNull()?.packageName
            if (pkg != null) return day to pkg
        }
        return null
    }

    @Before
    fun seedTheDay() {
        // The window only holds the days the store has, so a finished day must be stored.
        val day = LocalDate.now().minusDays(1)
        val from = day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() + 9 * HOUR
        val seeded =
            SessionHistoryStore.forApp(targetContext) { date ->
                if (date == day) {
                    DayData(listOf(UsageSession(PACKAGE, from, from + HOUR)), unlockTimes = emptyList())
                } else {
                    emptyDay
                }
            }
        runBlocking { seeded.consolidate(LocalDate.now()) }
    }

    @Test fun previewShowsValues() {
        val stored = storedDayAndPackage()
        assumeTrue("the device stores no finished day", stored != null)
        val day = stored!!.first
        val pkg = stored.second
        compose.setContent {
            MaterialTheme {
                AppDetailScreen(
                    packageName = pkg,
                    date = day,
                )
            }
        }
        compose.onNodeWithTag(LIMITS_PREVIEW_TAG, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test fun theLimitsIconOpensTheDedicatedSettings() {
        val stored = storedDayAndPackage()
        assumeTrue("the device stores no finished day", stored != null)
        val day = stored!!.first
        val pkg = stored.second
        var opened = false
        compose.setContent {
            MaterialTheme {
                AppDetailScreen(
                    packageName = pkg,
                    date = day,
                    onConfigure = { opened = true },
                )
            }
        }

        compose
            .onNodeWithContentDescription(appString(R.string.detail_limits_configure))
            .performClick()

        assertTrue(opened)
    }

    @Test fun tappingTheLimitsBlockOpensTheDedicatedSettings() {
        val stored = storedDayAndPackage()
        assumeTrue("the device stores no finished day", stored != null)
        val day = stored!!.first
        val pkg = stored.second
        var opened = false
        compose.setContent {
            MaterialTheme {
                AppDetailScreen(
                    packageName = pkg,
                    date = day,
                    onConfigure = { opened = true },
                )
            }
        }

        // The whole preview block is a shortcut to the app's limit settings.
        compose.onNodeWithTag(LIMITS_PREVIEW_TAG, useUnmergedTree = true).performClick()

        assertTrue(opened)
    }

    @Test fun aHorizontalSwipeWalksTheDays() {
        val stored = storedDayAndPackage()
        assumeTrue("the device stores no finished day", stored != null)
        val day = stored!!.first
        val pkg = stored.second
        compose.setContent {
            MaterialTheme { AppDetailScreen(packageName = pkg, date = day) }
        }
        // The detail is loaded and shows the requested (stored) day, not today.
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText(appString(R.string.detail_limits_section)).fetchSemanticsNodes().isNotEmpty() &&
                compose.onAllNodesWithText(appString(R.string.day_today)).fetchSemanticsNodes().isEmpty()
        }

        // A swipe right walks to the previous day, the way the home's ring walks its positions.
        // The touch injection is not always delivered on the first try: swipe until the day moves.
        val shown = {
            compose.onNodeWithTag(DAY_LABEL_TAG).fetchSemanticsNode().config[SemanticsProperties.Text].joinToString(" ")
        }
        val before = shown()
        var moved = false
        repeat(3) {
            if (moved) return@repeat
            compose.onNodeWithTag(DETAIL_CONTENT_TAG).performTouchInput { swipeRight() }
            compose.waitUntil(timeoutMillis = 3_000) { shown() != before }
            moved = shown() != before
        }

        assertTrue("the swipe must walk to another day (was $before)", moved)
    }

    private companion object {
        const val PACKAGE = "com.foco.app"
        const val HOUR = 3_600_000L
        val emptyDay = DayData(sessions = emptyList(), unlockTimes = emptyList())
    }
}
