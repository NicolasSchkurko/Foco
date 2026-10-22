package com.foco.app.domain

import com.foco.app.model.UsageSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class AppLimitWindowRulesTest {
    private val zone = ZoneId.of("UTC")
    private val day = LocalDate.of(2026, 10, 20)

    private fun at(time: String): Long =
        day
            .atTime(LocalTime.parse(time))
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

    private fun at(
        zone: ZoneId,
        date: LocalDate,
        time: String,
    ): Long =
        date
            .atTime(LocalTime.parse(time))
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

    private fun session(
        from: String,
        to: String,
    ) = UsageSession("instagram", at(from), at(to))

    @Test
    fun `only the 15-minute grid inside one day is selectable`() {
        assertTrue(AppLimitWindowRules.isSelectable(0))
        assertTrue(AppLimitWindowRules.isSelectable(9 * 60))
        assertTrue(AppLimitWindowRules.isSelectable(23 * 60 + 45))

        assertFalse(AppLimitWindowRules.isSelectable(7))
        assertFalse(AppLimitWindowRules.isSelectable(20))
        assertFalse(AppLimitWindowRules.isSelectable(-15))
        assertFalse(AppLimitWindowRules.isSelectable(24 * 60))
    }

    @Test
    fun `equal start and end is rejected`() {
        assertFalse(AppLimitWindowRules.isValid(9 * 60, 9 * 60))
        assertTrue(AppLimitWindowRules.isValid(9 * 60, 23 * 60))
        assertTrue(AppLimitWindowRules.isValid(22 * 60, 6 * 60))
    }

    @Test
    fun `a daytime window includes its start and excludes its end`() {
        val window = AppWindow(9 * 60, 23 * 60)

        assertFalse(AppLimitWindowRules.isInside(at("08:59"), window, zone))
        assertTrue(AppLimitWindowRules.isInside(at("09:00"), window, zone))
        assertTrue(AppLimitWindowRules.isInside(at("22:59"), window, zone))
        assertFalse(AppLimitWindowRules.isInside(at("23:00"), window, zone))
    }

    @Test
    fun `an overnight window wraps past midnight`() {
        val window = AppWindow(22 * 60, 6 * 60)

        assertFalse(AppLimitWindowRules.isInside(at("21:59"), window, zone))
        assertTrue(AppLimitWindowRules.isInside(at("22:00"), window, zone))
        assertTrue(AppLimitWindowRules.isInside(at("23:00"), window, zone))
        assertTrue(AppLimitWindowRules.isInside(at("05:00"), window, zone))
        assertFalse(AppLimitWindowRules.isInside(at("06:00"), window, zone))
        assertFalse(AppLimitWindowRules.isInside(at("12:00"), window, zone))
    }

    @Test
    fun `the next boundary is the end while inside and the start while outside`() {
        val window = AppWindow(9 * 60, 23 * 60)

        assertEquals(at("23:00"), AppLimitWindowRules.nextBoundaryMillis(at("10:00"), window, zone))
        assertEquals(at("09:00"), AppLimitWindowRules.nextBoundaryMillis(at("08:00"), window, zone))
        assertEquals(
            day
                .plusDays(1)
                .atTime(LocalTime.parse("09:00"))
                .atZone(zone)
                .toInstant()
                .toEpochMilli(),
            AppLimitWindowRules.nextBoundaryMillis(at("23:30"), window, zone),
        )
    }

    @Test
    fun `an overnight boundary stays on the right day`() {
        val window = AppWindow(22 * 60, 6 * 60)

        assertEquals(
            day
                .plusDays(1)
                .atTime(LocalTime.parse("06:00"))
                .atZone(zone)
                .toInstant()
                .toEpochMilli(),
            AppLimitWindowRules.nextBoundaryMillis(at("23:00"), window, zone),
        )
        assertEquals(at("22:00"), AppLimitWindowRules.nextBoundaryMillis(at("12:00"), window, zone))
    }

    @Test
    fun `only the in-window portion of a session counts`() {
        val window = AppWindow(9 * 60, 23 * 60)

        assertEquals(60 * 60_000L, AppLimitWindowRules.inWindowMillis(session("08:00", "10:00"), window, zone))
        assertEquals(0L, AppLimitWindowRules.inWindowMillis(session("23:00", "23:30"), window, zone))
    }

    @Test
    fun `an overnight session counts only its in-window part`() {
        val window = AppWindow(22 * 60, 6 * 60)

        assertEquals(30 * 60_000L, AppLimitWindowRules.inWindowMillis(session("23:00", "23:30"), window, zone))
        assertEquals(60 * 60_000L, AppLimitWindowRules.inWindowMillis(session("05:00", "07:00"), window, zone))
        assertEquals(0L, AppLimitWindowRules.inWindowMillis(session("12:00", "13:00"), window, zone))
    }

    @Test
    fun `a session crossing local midnight counts its in-window minutes on the day they belong to`() {
        val window = AppWindow(22 * 60, 6 * 60)
        val crossing =
            UsageSession(
                "instagram",
                at(zone, day, "23:00"),
                at(zone, day.plusDays(1), "03:00"),
            )

        // 23:00-00:00 belongs to the 20th, 00:00-03:00 to the 21st: 4 h inside the window.
        assertEquals(
            mapOf(day to 60 * 60_000L, day.plusDays(1) to 3 * 60 * 60_000L),
            AppLimitWindowRules.inWindowMillisByDay(crossing, window, zone),
        )
        assertEquals(4 * 60 * 60_000L, AppLimitWindowRules.inWindowMillis(crossing, window, zone))
    }

    @Test
    fun `sub-second sessions are not truncated away`() {
        val window = AppWindow(9 * 60, 23 * 60)
        val sessions =
            listOf(
                UsageSession("instagram", at("09:00"), at("09:00") + 1_500L),
                UsageSession("instagram", at("10:00"), at("10:00") + 1_500L),
            )

        assertEquals(3L, AppLimitWindowRules.usedSeconds(sessions, "instagram", window, zone))
    }

    @Test
    fun `the used seconds clip to the window and the package`() {
        val sessions =
            listOf(
                session("08:00", "10:00"),
                UsageSession("youtube", at("09:00"), at("10:00")),
            )

        assertEquals(
            3600L,
            AppLimitWindowRules.usedSeconds(sessions, "instagram", AppWindow(9 * 60, 23 * 60), zone),
        )
        assertEquals(
            0L,
            AppLimitWindowRules.usedSeconds(sessions, "instagram", AppWindow(23 * 60, 6 * 60), zone),
        )
        assertEquals(
            3600L,
            AppLimitWindowRules.usedSeconds(sessions, "youtube", AppWindow(9 * 60, 23 * 60), zone),
        )
    }

    // @test — REQ: Window boundaries follow the local wall clock (delta spec)
    @Test
    fun `a spring-forward day ends the window at the wall time it names`() {
        val ny = ZoneId.of("America/New_York")
        val d = LocalDate.of(2026, 3, 8) // clocks jump 02:00→03:00
        val w = AppWindow(0, 360) // 00:00–06:00 local
        assertEquals(
            60 * 60_000L,
            AppLimitWindowRules.inWindowMillis(
                UsageSession("x", at(ny, d, "05:00"), at(ny, d, "08:00")),
                w,
                ny,
            ),
        )
        assertEquals(
            at(ny, d, "06:00"),
            AppLimitWindowRules.nextBoundaryMillis(at(ny, d, "05:00"), w, ny),
        )
        assertNotEquals(
            at(ny, d, "07:00"),
            AppLimitWindowRules.nextBoundaryMillis(at(ny, d, "05:00"), w, ny),
        )
    }

    @Test
    fun `a boundary inside the skipped range resolves to the first instant after the gap`() {
        val ny = ZoneId.of("America/New_York")
        val d = LocalDate.of(2026, 3, 8)
        val w = AppWindow(120, 240) // 02:00–04:00
        assertEquals(
            at(ny, d, "03:00"),
            AppLimitWindowRules.nextBoundaryMillis(at(ny, d, "00:30"), w, ny),
        )
        assertEquals(
            60 * 60_000L,
            AppLimitWindowRules.inWindowMillis(
                UsageSession("x", at(ny, d, "03:00"), at(ny, d, "06:00")),
                w,
                ny,
            ),
        )
    }

    @Test
    fun `a fall-back day keeps the window open for the wall range it names`() {
        val ny = ZoneId.of("America/New_York")
        val d = LocalDate.of(2026, 11, 1)
        val w = AppWindow(60, 120)
        val first0130 = at(ny, d, "01:30")
        val second0130 = first0130 + 60 * 60_000L
        assertEquals(
            60 * 60_000L,
            AppLimitWindowRules.inWindowMillis(
                UsageSession("x", first0130, second0130),
                w,
                ny,
            ),
        )
        assertEquals(
            at(ny, d, "02:00"),
            AppLimitWindowRules.nextBoundaryMillis(at(ny, d, "01:30"), w, ny),
        )
    }

    @Test
    fun `an ordinary day in a DST zone is unchanged`() {
        val ny = ZoneId.of("America/New_York")
        val d = LocalDate.of(2026, 10, 20)
        val w = AppWindow(9 * 60, 23 * 60)
        assertEquals(
            60 * 60_000L,
            AppLimitWindowRules.inWindowMillis(
                UsageSession("x", at(ny, d, "08:00"), at(ny, d, "10:00")),
                w,
                ny,
            ),
        )
        assertEquals(
            at(ny, d, "23:00"),
            AppLimitWindowRules.nextBoundaryMillis(at(ny, d, "10:00"), w, ny),
        )
        assertEquals(
            at(ny, d, "09:00"),
            AppLimitWindowRules.nextBoundaryMillis(at(ny, d, "08:00"), w, ny),
        )
    }
}
