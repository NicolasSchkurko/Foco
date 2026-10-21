package com.foco.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.foco.app.R
import com.foco.app.data.ExcludedAppsStore
import com.foco.app.data.SessionHistoryStore
import com.foco.app.data.db.DailyAggregate
import com.foco.app.model.UsageSession
import com.foco.app.model.day.DayData
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

/**
 * The week view on device: entry from the dashboard and back, the seven bars (today live, the
 * stored days from the store, the rest absent) and a bar opening that day's view with its numbers.
 */
@RunWith(AndroidJUnit4::class)
class WeekScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

    private val today = LocalDate.now()

    private val settings = "com.android.settings"

    private val liveFrom =
        today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() + 9 * HOUR_MILLIS
    private val liveTo = liveFrom + 30 * 60_000L
    private val liveDay = DayData(listOf(UsageSession(settings, liveFrom, liveTo)), unlockTimes = emptyList())

    /** A leftover exclusion from another test would hide the row the week shows. */
    @Before
    fun clearExclusion() {
        runBlocking { ExcludedAppsStore(targetContext).restore(settings) }
    }

    private fun string(resId: Int): String = targetContext.resources.getString(resId)

    private fun string(
        resId: Int,
        vararg args: Any,
    ): String = targetContext.resources.getString(resId, *args)

    private fun quantity(
        resId: Int,
        count: Int,
    ): String = targetContext.resources.getQuantityString(resId, count, count)

    private fun dayLabel(date: LocalDate): String =
        formatDayLabel(
            date,
            today,
            string(R.string.day_today),
            string(R.string.day_yesterday),
        )

    /** The live day comes through the seam: the platform is never read and no activity is launched. */
    private fun showApp() {
        composeRule.setContent { FocoNavHost(loadDay = { liveDay }) }
    }

    private fun awaitText(text: String) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun awaitDescription(description: String) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithContentDescription(description).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun openWeek() {
        awaitDescription(string(R.string.week_title))
        composeRule.onNodeWithContentDescription(string(R.string.week_title)).performClick()
        awaitText(string(R.string.week_title))
    }

    /** Stores one day's session in the app's own store, behind the platform reader. */
    private fun seedStoredDay(
        day: LocalDate,
        from: Long,
        to: Long,
    ) {
        val store =
            SessionHistoryStore.forApp(targetContext) { date ->
                if (date == day) {
                    DayData(listOf(UsageSession(settings, from, to)), emptyList())
                } else {
                    DayData(emptyList(), emptyList())
                }
            }
        runBlocking { store.consolidate(today) }
    }

    /** The read-only store over the app's database. */
    private fun store(): SessionHistoryStore = SessionHistoryStore.forApp(targetContext) { DayData(emptyList(), emptyList()) }

    private fun storedWeek(): List<DailyAggregate> = runBlocking { store().week(today) }

    @Test
    fun theWeekControlOpensTheWeekAndBackReturnsToTheDashboard() {
        showApp()
        awaitText(string(R.string.dashboard_title))

        openWeek()

        composeRule.onAllNodesWithContentDescription(string(R.string.content_back)).assertCountEquals(0)
        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        awaitText(string(R.string.dashboard_title))
    }

    @Test
    fun theSevenBarsCoverTheWindowAndTheRankingListsTheApp() {
        seedStoredDay(today.minusDays(1), at(today.minusDays(1), 20), at(today.minusDays(1), 21))
        seedStoredDay(today.minusDays(2), at(today.minusDays(2), 18), at(today.minusDays(2), 18, 45))

        showApp()
        openWeek()

        // The expected description of every bar, day by day: today live, a stored day summed from
        // its rows and a day the store has nothing for absent (never a duration of zero).
        val storedTotals = storedWeek()
        val unseeded =
            (1..6)
                .map { today.minusDays(it.toLong()) }
                .filter { date -> storedTotals.none { it.day == date.toString() } }
        val expected =
            (6 downTo 0).map { back ->
                val date = today.minusDays(back.toLong())
                val rows = storedTotals.filter { it.day == date.toString() }
                when {
                    date == today ->
                        string(R.string.week_bar, dayLabel(date), formatDuration(liveTo - liveFrom))

                    rows.isNotEmpty() ->
                        string(R.string.week_bar, dayLabel(date), formatDuration(rows.sumOf { it.totalMillis }))

                    else -> string(R.string.week_bar_absent, dayLabel(date))
                }
            }
        expected.forEach { awaitDescription(it) }

        // Every day the store has nothing for reads "no data", never a zero duration. A device whose
        // store already holds all six finished days has no absent bar left to assert here; the rule
        // itself is pinned by `WeekMetricsTest`. Unseeded today: $unseeded
        unseeded.forEach {
            awaitDescription(string(R.string.week_bar_absent, dayLabel(it)))
        }

        val daysUsed =
            storedTotals
                .filter { it.packageName == settings }
                .map { it.day }
                .distinct()
                .size + 1
        awaitText(string(R.string.dashboard_apps))
        awaitText(quantity(R.plurals.week_days_used, daysUsed))
    }

    @Test
    fun tappingAnAbsentBarOpensTheEmptyDayAndBackReturnsToTheWeek() {
        showApp()
        openWeek()

        val stored = storedWeek().map { it.day }.toSet()
        val absent =
            (1..6)
                .map { today.minusDays(it.toLong()) }
                .firstOrNull { it.toString() !in stored }
        assumeTrue("the device already stores every day in the window", absent != null)

        val day = absent!!
        val description = string(R.string.week_bar_absent, dayLabel(day))
        awaitDescription(description)
        composeRule.onNodeWithContentDescription(description).performClick()

        awaitText(dayLabel(day))
        awaitText(string(R.string.day_empty))

        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        awaitText(string(R.string.week_title))
    }

    @Test
    fun tappingAStoredDaysBarOpensThatDayAndItsAppDetail() {
        val day = today.minusDays(1)
        seedStoredDay(day, at(day, 20), at(day, 21))

        val dayTotal = storedWeek().filter { it.day == day.toString() }.sumOf { it.totalMillis }
        val storedUnlocks = runBlocking { store().unlocksFor(day) } ?: 0
        val settingsTotal =
            runBlocking { store().sessionsFor(day) }
                .filter { it.packageName == settings }
                .sumOf { it.end - it.start }
        assertTrue("the seeded day must keep its app's sessions: $settingsTotal", settingsTotal > 0)

        showApp()
        openWeek()

        val bar = string(R.string.week_bar, dayLabel(day), formatDuration(dayTotal))
        awaitDescription(bar)
        composeRule.onNodeWithContentDescription(bar).performClick()

        // The day view: that date's title, its stored total with its stored unlocks, chart, ranking.
        awaitText(dayLabel(day))
        awaitText(
            string(
                R.string.dashboard_usage_line,
                formatDuration(dayTotal),
                quantity(R.plurals.dashboard_unlocks, storedUnlocks),
            ),
        )
        awaitText(string(R.string.chart_hourly))
        awaitText(string(R.string.dashboard_apps))

        // The week is gone; its row for the same app would be a second match for the label.
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(string(R.string.week_title)).fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithText("Settings").performClick()

        awaitText(string(R.string.detail_day_usage, formatDuration(settingsTotal)))
    }

    @Test
    fun tappingAWeekRankingRowOpensTodaysDetail() {
        val day = today.minusDays(1)
        seedStoredDay(day, at(day, 20), at(day, 21))

        showApp()
        openWeek()

        awaitText(string(R.string.dashboard_apps))
        composeRule.onAllNodesWithText("Settings")[0].performClick()

        // The week position's rows open today's detail — the row summarises seven days, the screen
        // shows one: today.
        awaitText(string(R.string.detail_day_usage, formatDuration(liveTo - liveFrom)))
    }

    /** One day's instant at the given hour and minute, on the device's own calendar. */
    private fun at(
        day: LocalDate,
        hour: Int,
        minute: Int = 0,
    ): Long =
        day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() +
            hour * HOUR_MILLIS + minute * 60_000L

    private companion object {
        const val HOUR_MILLIS = 3_600_000L
    }
}
