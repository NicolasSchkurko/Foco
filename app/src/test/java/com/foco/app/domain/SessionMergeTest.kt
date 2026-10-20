package com.foco.app.domain

import com.foco.app.model.UsageSession
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class SessionMergeTest {
    private val zone = ZoneOffset.UTC

    private fun at(
        hour: Int,
        minute: Int,
        second: Int = 0,
    ): Long =
        LocalDate
            .of(2026, 10, 19)
            .atTime(hour, minute, second)
            .toInstant(zone)
            .toEpochMilli()

    private fun session(
        start: Long,
        end: Long,
        packageName: String = "app.a",
    ) = UsageSession(packageName, start, end)

    @Test
    fun `sessions that meet in the same minute are one`() {
        val first = session(at(3, 44, 10), at(3, 44, 50))
        val second = session(at(3, 44, 55), at(3, 46, 0))

        assertEquals(
            listOf(UsageSession("app.a", first.start, second.end)),
            mergeContiguousSessions(listOf(first, second), zone),
        )
    }

    @Test
    fun `the chain folds transitively`() {
        val first = session(at(4, 0), at(4, 30))
        val second = session(at(4, 30), at(4, 45))
        val third = session(at(4, 45), at(5, 0))

        assertEquals(
            listOf(UsageSession("app.a", first.start, third.end)),
            mergeContiguousSessions(listOf(first, second, third), zone),
        )
    }

    @Test
    fun `a gap in the next minute stays separate`() {
        val first = session(at(3, 44), at(3, 44, 30))
        val second = session(at(3, 45), at(3, 46))

        assertEquals(
            listOf(first, second),
            mergeContiguousSessions(listOf(first, second), zone),
        )
    }

    @Test
    fun `different apps are never merged`() {
        val first = session(at(3, 43), at(3, 44), "app.a")
        val second = session(at(3, 44), at(3, 45), "app.b")

        assertEquals(
            listOf(first, second),
            mergeContiguousSessions(listOf(first, second), zone),
        )
    }

    @Test
    fun `the same time on different days is not merged`() {
        val first = session(at(3, 44), at(3, 44, 30))
        val nextDay =
            LocalDate
                .of(2026, 10, 20)
                .atTime(3, 44, 30)
                .toInstant(zone)
                .toEpochMilli()
        val second = session(nextDay, nextDay + 30_000)

        assertEquals(
            listOf(first, second),
            mergeContiguousSessions(listOf(first, second), zone),
        )
    }

    @Test
    fun `the output is ordered by start`() {
        val late = session(at(5, 0), at(5, 30))
        val early = session(at(4, 0), at(4, 30))

        assertEquals(
            listOf(early, late),
            mergeContiguousSessions(listOf(late, early), zone),
        )
    }
}
