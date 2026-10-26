package com.foco.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.test.platform.app.InstrumentationRegistry
import com.foco.app.R
import com.foco.app.data.stores.SessionHistoryStore
import com.foco.app.model.UsageSession
import com.foco.app.model.day.DayData
import com.foco.app.ui.detail.AppDetailScreen
import com.foco.app.ui.detail.DAY_LABEL_TAG
import com.foco.app.ui.detail.DETAIL_CONTENT_TAG
import com.foco.app.ui.detail.LIMITS_PREVIEW_TAG
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertTrue
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

    /**
     * A day and a package the store really holds, inside the detail's seven-day window. The seed
     * runs first; a store that holds nothing is a failure, never a skipped test.
     */
    private fun storedDayAndPackage(): Pair<LocalDate, String> {
        val today = LocalDate.now()
        val days = runBlocking { storedDays() }
        for (day in days.filter { it <= today && it >= today.minusDays(6) }.sortedDescending()) {
            val pkg = runBlocking { store.sessionsFor(day) }.firstOrNull()?.packageName
            if (pkg != null) return day to pkg
        }
        throw AssertionError("the store holds no finished day with a session: the seed did not land")
    }

    /** The stored days; the flow re-emits when the seed lands, so the wait is not a fixed sleep. */
    private suspend fun storedDays(): Set<LocalDate> =
        withTimeoutOrNull(SEED_TIMEOUT_MILLIS) {
            store.storedDaysFlow().first { it.isNotEmpty() }
        }.orEmpty()

    @Before
    fun seedTheDay() {
        // The window only holds the days the store has, so a finished day must be stored. The seed
        // takes a day the store has not consolidated yet, so the rows really land.
        val today = LocalDate.now()
        val marked = runBlocking { store.storedDaysFlow().firstOrNull() }.orEmpty()
        val day = (1..6).map { today.minusDays(it.toLong()) }.firstOrNull { it !in marked } ?: today.minusDays(1)
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
        val day = stored.first
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

    @Test fun theDayArrowsCarryADescription() {
        val stored = storedDayAndPackage()
        val day = stored.first
        val pkg = stored.second
        compose.setContent {
            MaterialTheme { AppDetailScreen(packageName = pkg, date = day) }
        }

        // A screen reader announces the arrows, not the "<" and ">" glyphs.
        compose.onNodeWithContentDescription(appString(R.string.day_previous)).assertIsDisplayed()
        compose.onNodeWithContentDescription(appString(R.string.day_next)).assertIsDisplayed()
    }

    @Test fun theLimitsIconOpensTheDedicatedSettings() {
        val stored = storedDayAndPackage()
        val day = stored.first
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
        val day = stored.first
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
        val day = stored.first
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
            compose
                .onNodeWithTag(DAY_LABEL_TAG)
                .fetchSemanticsNode()
                .config[SemanticsProperties.Text]
                .joinToString(" ")
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

    @Test fun aSwipeWalksTheDaysThatArriveAfterTheFirstFrame() {
        val stored = storedDayAndPackage()
        val pkg = stored.second
        // Opened without a day, the way the home opens it: the window only holds the days the
        // store announces, and those arrive after the first frame.
        compose.setContent { MaterialTheme { AppDetailScreen(packageName = pkg) } }
        // The window grew when the arrow to an older day turned on.
        compose.waitUntil(timeoutMillis = 10_000) {
            compose
                .onAllNodesWithText(ARROW_TO_OLDER)
                .fetchSemanticsNodes()
                .any { SemanticsProperties.Disabled !in it.config }
        }

        val shown = {
            compose
                .onNodeWithTag(DAY_LABEL_TAG)
                .fetchSemanticsNode()
                .config[SemanticsProperties.Text]
                .joinToString(" ")
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
        const val ARROW_TO_OLDER = "‹"
        const val SEED_TIMEOUT_MILLIS = 5_000L
        val emptyDay = DayData(sessions = emptyList(), unlockTimes = emptyList())
    }
}
