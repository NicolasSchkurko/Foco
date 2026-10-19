package com.foco.app.ui.metrics

import com.foco.app.data.db.DailyAggregate
import com.foco.app.model.UsageSession
import com.foco.app.model.day.DayData
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeekMetricsTest {

    private val zone = ZoneOffset.UTC

    private val today = LocalDate.of(2026, 10, 16)

    private fun at(date: LocalDate, hour: Int, minute: Int): Long =
        ZonedDateTime.of(date.year, date.monthValue, date.dayOfMonth, hour, minute, 0, 0, zone)
            .toInstant()
            .toEpochMilli()

    private fun live(sessions: List<UsageSession>) = DayData(sessions, emptyList())

    private fun minutes(count: Int): Long = count * 60_000L

    @Test
    fun `the window runs seven days oldest first, today live and the six previous stored`() {
        val stored = (1..6).map { back ->
            DailyAggregate("app.a", today.minusDays(back.toLong()).toString(), minutes(back))
        }

        val metrics = deriveWeekMetrics(stored, today, live(emptyList()))

        assertEquals(7, metrics.bars.size)
        assertEquals(today.minusDays(6), metrics.bars.first().date)
        assertEquals(today, metrics.bars.last().date)
        assertEquals(WeekDayBar.State.Live, metrics.bars.last().state)
        assertTrue(
            "the six previous days must be stored: ${metrics.bars.map { it.state }}",
            metrics.bars.dropLast(1).all { it.state == WeekDayBar.State.Stored }
        )
    }

    @Test
    fun `today takes its total from the live sessions`() {
        val sessions = listOf(UsageSession("app.a", at(today, 9, 0), at(today, 10, 0)))

        val bar = deriveWeekMetrics(emptyList(), today, live(sessions)).bars.last()

        assertEquals(WeekDayBar.State.Live, bar.state)
        assertEquals(60 * 60_000L, bar.totalMillis)
    }

    @Test
    fun `a day with no stored total reads absent, not as a stored zero`() {
        val stored = listOf(DailyAggregate("app.a", today.minusDays(2).toString(), minutes(10)))

        val bar = deriveWeekMetrics(stored, today, live(emptyList()))
            .bars.single { it.date == today.minusDays(1) }

        assertEquals(WeekDayBar.State.Absent, bar.state)
        assertEquals(0L, bar.totalMillis)
    }

    @Test
    fun `the ranking merges a package's stored days and its live sessions into one entry`() {
        val stored = listOf(
            DailyAggregate("app.a", today.minusDays(1).toString(), minutes(10)),
            DailyAggregate("app.a", today.minusDays(2).toString(), minutes(20)),
            DailyAggregate("app.b", today.minusDays(1).toString(), minutes(5))
        )
        val sessions = listOf(UsageSession("app.a", at(today, 9, 0), at(today, 9, 30)))

        val ranking = deriveWeekMetrics(stored, today, live(sessions)).ranking

        assertEquals(listOf("app.a", "app.b"), ranking.map { it.packageName })
        assertEquals(minutes(60), ranking[0].totalMillis)
        assertEquals(3, ranking[0].daysUsed)
        assertEquals(minutes(5), ranking[1].totalMillis)
        assertEquals(1, ranking[1].daysUsed)
    }

    @Test
    fun `a week with only today still yields seven bars and the live ranking`() {
        val sessions = listOf(UsageSession("app.a", at(today, 8, 0), at(today, 8, 10)))

        val metrics = deriveWeekMetrics(emptyList(), today, live(sessions))

        assertEquals(7, metrics.bars.size)
        assertEquals(6, metrics.bars.count { it.state == WeekDayBar.State.Absent })
        assertEquals(listOf("app.a"), metrics.ranking.map { it.packageName })
    }

    @Test
    fun `an excluded package leaves the ranking but keeps its time in the bars`() {
        val stored = listOf(
            DailyAggregate("app.a", today.minusDays(1).toString(), minutes(40)),
            DailyAggregate("app.b", today.minusDays(1).toString(), minutes(10))
        )

        val metrics = deriveWeekMetrics(stored, today, live(emptyList()), excludedPackages = setOf("app.a"))

        assertEquals(listOf("app.b"), metrics.ranking.map { it.packageName })
        assertEquals(minutes(50), metrics.bars.single { it.date == today.minusDays(1) }.totalMillis)
    }
}
