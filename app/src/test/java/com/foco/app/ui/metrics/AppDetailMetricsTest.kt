package com.foco.app.ui.metrics

import com.foco.app.model.UsageSession
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppDetailMetricsTest {

    private val zone = ZoneOffset.UTC

    private fun at(hour: Int, minute: Int): Long =
        ZonedDateTime.of(2026, 10, 16, hour, minute, 0, 0, zone).toInstant().toEpochMilli()

    @Test
    fun `only the selected app counts`() {
        val sessions = listOf(
            UsageSession("app.a", at(10, 0), at(10, 30)),
            UsageSession("app.b", at(11, 0), at(11, 50))
        )

        val detail = appDayDetail(sessions, "app.a", zone)!!

        assertEquals(30 * 60_000L, detail.totalMillis)
        assertEquals(listOf("app.a"), detail.sessions.map { it.packageName })
        assertEquals(0L, detail.hourly[11].millis)
    }

    @Test
    fun `a session crossing an hour boundary splits for the app`() {
        val sessions = listOf(UsageSession("app.a", at(10, 50), at(11, 10)))

        val hourly = appDayDetail(sessions, "app.a", zone)!!.hourly

        assertEquals(10 * 60_000L, hourly[10].millis)
        assertEquals(10 * 60_000L, hourly[11].millis)
    }

    @Test
    fun `typical day summarizes and lists sessions in order`() {
        val sessions = listOf(
            UsageSession("app.a", at(11, 0), at(11, 50)),
            UsageSession("app.a", at(10, 0), at(10, 30)) // fuera de orden a propósito
        )

        val detail = appDayDetail(sessions, "app.a", zone)!!

        assertEquals(80 * 60_000L, detail.totalMillis)
        assertEquals(2, detail.sessionCount)
        assertEquals(at(10, 0), detail.firstStart)
        assertEquals(at(11, 50), detail.lastEnd)
        assertEquals(40 * 60_000L, detail.averageMillis)
        assertEquals(50 * 60_000L, detail.longestMillis)
        assertEquals(listOf(at(10, 0), at(11, 0)), detail.sessions.map { it.start })
    }

    @Test
    fun `day without data yields null`() {
        assertNull(appDayDetail(listOf(UsageSession("app.b", at(9, 0), at(9, 1))), "app.a", zone))
    }
}
