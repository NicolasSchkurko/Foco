package com.foco.app.ui.metrics

import com.foco.app.model.UsageSession
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class UsageMetricsTest {

    private val zone = ZoneOffset.UTC

    private fun at(hour: Int, minute: Int): Long =
        ZonedDateTime.of(2026, 10, 16, hour, minute, 0, 0, zone).toInstant().toEpochMilli()

    @Test
    fun `day total sums session durations`() {
        val sessions = listOf(
            UsageSession("app.a", at(9, 0), at(10, 0)),    // 1 h
            UsageSession("app.a", at(11, 0), at(12, 0)),   // 1 h
            UsageSession("app.b", at(13, 0), at(14, 24))   // 1 h 24 min
        )

        assertEquals(3 * 3_600_000L + 24 * 60_000L, deriveDayMetrics(sessions, zone).totalMillis)
    }

    @Test
    fun `a session crossing an hour boundary splits between the two hours`() {
        val sessions = listOf(UsageSession("app.a", at(10, 50), at(11, 10)))

        val hourly = hourlyDistribution(sessions, zone)

        assertEquals(10 * 60_000L, hourly[10].millis)
        assertEquals(10 * 60_000L, hourly[11].millis)
    }

    @Test
    fun `ranking sorts apps by total duration descending`() {
        val sessions = listOf(
            UsageSession("app.b", at(9, 0), at(9, 10)),    // 10 min
            UsageSession("app.a", at(10, 0), at(10, 40))   // 40 min
        )

        val ranking = appRanking(sessions)

        assertEquals(listOf("app.a", "app.b"), ranking.map { it.packageName })
        assertEquals(40 * 60_000L, ranking[0].totalMillis)
        assertEquals(10 * 60_000L, ranking[1].totalMillis)
    }

    @Test
    fun `excluded apps leave the ranking but keep their time in totals`() {
        val sessions = listOf(
            UsageSession("app.a", at(9, 0), at(9, 40)),   // 40 min
            UsageSession("app.b", at(10, 0), at(10, 10))  // 10 min
        )

        val metrics = deriveDayMetrics(sessions, zone, excludedPackages = setOf("app.a"))

        assertEquals(listOf("app.b"), metrics.entries.map { it.packageName })
        assertEquals(50 * 60_000L, metrics.totalMillis)
        assertEquals(40 * 60_000L, metrics.hourly[9].millis)
    }
}
