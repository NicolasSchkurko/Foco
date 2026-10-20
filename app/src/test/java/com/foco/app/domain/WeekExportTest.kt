package com.foco.app.domain

import com.foco.app.model.UsageSession
import com.foco.app.model.export.WeekExport
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class WeekExportTest {
    private val zone = ZoneId.of("Europe/Madrid")
    private val end = LocalDate.of(2026, 10, 19)
    private val start = end.minusDays(6)

    private fun at(
        date: LocalDate,
        hour: Int,
        minute: Int,
    ): Long =
        ZonedDateTime
            .of(date.year, date.monthValue, date.dayOfMonth, hour, minute, 0, 0, zone)
            .toInstant()
            .toEpochMilli()

    private fun build(
        totals: Map<LocalDate, Map<String, Long>> = emptyMap(),
        sessions: Map<LocalDate, List<UsageSession>> = emptyMap(),
        today: List<UsageSession> = emptyList(),
        labels: (String) -> String = { it },
    ): WeekExport = buildWeekExport(start, end, totals, sessions, today, labels, zone)

    @Test
    fun `seven days oldest first and an empty day has no apps`() {
        val export = build()

        assertEquals(start.toString(), export.weekStart)
        assertEquals(end.toString(), export.weekEnd)
        assertEquals(7, export.days.size)
        assertEquals(start.toString(), export.days.first().date)
        assertEquals(end.toString(), export.days.last().date)
        assertTrue(
            export.days
                .first()
                .apps
                .isEmpty(),
        )
        assertEquals(0L, export.days.first().totalMillis)
    }

    @Test
    fun `today comes live while the previous days come from the store`() {
        val yesterday = end.minusDays(1)
        val export =
            build(
                totals = mapOf(yesterday to mapOf("app.b" to 1_000L)),
                sessions =
                    mapOf(
                        yesterday to listOf(UsageSession("app.b", at(yesterday, 9, 0), at(yesterday, 9, 10))),
                    ),
                today = listOf(UsageSession("app.a", at(end, 10, 0), at(end, 10, 5))),
            )

        val todayEntry = export.days.last()
        val yesterdayEntry = export.days.single { it.date == yesterday.toString() }

        assertEquals("app.a", todayEntry.apps.single().packageName)
        assertTrue(todayEntry.apps.single().sessionsAvailable)
        assertEquals(5 * 60_000L, todayEntry.totalMillis)

        assertEquals("app.b", yesterdayEntry.apps.single().packageName)
        assertEquals(10 * 60_000L, yesterdayEntry.apps.single().totalMillis)
    }

    @Test
    fun `an app total is the sum of its sessions, oldest first, with offset timestamps`() {
        val day = end.minusDays(2)
        val export =
            build(
                sessions =
                    mapOf(
                        day to
                            listOf(
                                UsageSession("app.a", at(day, 11, 0), at(day, 11, 30)),
                                UsageSession("app.a", at(day, 9, 0), at(day, 9, 20)),
                            ),
                    ),
            )

        val app =
            export.days
                .single { it.date == day.toString() }
                .apps
                .single()

        assertEquals(50 * 60_000L, app.totalMillis)
        assertEquals(2, app.sessions.size)
        assertEquals("2026-10-17T09:00:00+02:00", app.sessions.first().start)
        assertTrue(app.sessions.first().start < app.sessions.last().start)
        assertEquals(20 * 60_000L, app.sessions.first().durationMillis)
    }

    @Test
    fun `a stored same-minute pair is normalized to one session`() {
        val day = end.minusDays(2)
        val export =
            build(
                sessions =
                    mapOf(
                        day to
                            listOf(
                                UsageSession("app.a", at(day, 3, 44) + 10_000, at(day, 3, 44) + 50_000),
                                UsageSession("app.a", at(day, 3, 44) + 55_000, at(day, 3, 46)),
                            ),
                    ),
            )

        val app =
            export.days
                .single { it.date == day.toString() }
                .apps
                .single()

        assertEquals(110_000L, app.totalMillis)
        assertEquals(1, app.sessions.size)
        assertEquals("2026-10-17T03:44:10+02:00", app.sessions.single().start)
        assertEquals("2026-10-17T03:46:00+02:00", app.sessions.single().end)
        assertEquals(110_000L, app.sessions.single().durationMillis)
    }

    @Test
    fun `a stored same-minute pair from different apps stays separate`() {
        val day = end.minusDays(2)
        val export =
            build(
                sessions =
                    mapOf(
                        day to
                            listOf(
                                UsageSession("app.a", at(day, 3, 44) + 10_000, at(day, 3, 44) + 50_000),
                                UsageSession("app.b", at(day, 3, 44) + 55_000, at(day, 3, 46)),
                            ),
                    ),
            )

        val apps = export.days.single { it.date == day.toString() }.apps

        assertEquals(setOf("app.a", "app.b"), apps.map { it.packageName }.toSet())
        assertEquals(2, apps.sumOf { it.sessions.size })
    }

    @Test
    fun `a stored total without raw rows keeps its total and says so`() {
        val day = end.minusDays(3)
        val export = build(totals = mapOf(day to mapOf("app.c" to 4_200L)))

        val app =
            export.days
                .single { it.date == day.toString() }
                .apps
                .single()

        assertEquals(4_200L, app.totalMillis)
        assertFalse(app.sessionsAvailable)
        assertTrue(app.sessions.isEmpty())
        assertEquals(4_200L, export.days.single { it.date == day.toString() }.totalMillis)
    }

    @Test
    fun `raw records are exported even when short or from an excluded app`() {
        val day = end.minusDays(4)
        val export =
            build(
                sessions = mapOf(day to listOf(UsageSession("app.z", at(day, 8, 0), at(day, 8, 0) + 500))),
            )

        val app =
            export.days
                .single { it.date == day.toString() }
                .apps
                .single()

        assertEquals(500L, app.totalMillis)
        assertTrue(app.sessionsAvailable)
        assertEquals(500L, app.sessions.single().durationMillis)
    }

    @Test
    fun `apps with the same total are ordered by package and the labels are escaped`() {
        val day = end.minusDays(5)
        val export =
            build(
                today =
                    listOf(
                        UsageSession("app.b", at(end, 9, 0), at(end, 9, 1)),
                        UsageSession("app.a", at(end, 10, 0), at(end, 10, 1)),
                    ),
                labels = { pkg -> if (pkg == "app.a") "Quote \" and \\ slash" else pkg },
            )

        val apps = export.days.last().apps
        assertEquals(listOf("app.a", "app.b"), apps.map { it.packageName })

        val json = weekExportJson(export)
        val decoded = Json.decodeFromString(WeekExport.serializer(), json)
        assertEquals(export, decoded)
        assertEquals(
            "Quote \" and \\ slash",
            decoded.days
                .last()
                .apps
                .first()
                .appName,
        )
        assertTrue(json.contains("\n  \"weekStart\""))
    }
}
