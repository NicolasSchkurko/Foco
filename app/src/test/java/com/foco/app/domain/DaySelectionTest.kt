package com.foco.app.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The selector's window comes from the stored history: today and every day back to the oldest
 * stored one inside the raw retention.
 */
class DaySelectionTest {

    private val today = LocalDate.of(2026, 10, 16)

    /** The days [daysBack] behind today, as `SessionHistoryStore.storedDays()` returns them. */
    private fun storedDays(vararg daysBack: Long): Set<LocalDate> = daysBack.map { today.minusDays(it) }.toSet()

    @Test
    fun `offers today first and every day back to the oldest stored one`() {
        val days = availableDays(today, storedDays(1, 2, 3, 4, 5, 6))

        assertEquals(7, days.size)
        assertEquals(today, days.first())
        assertEquals(today.minusDays(6), days.last())
        assertEquals(emptyList<LocalDate>(), days.filter { it.isAfter(today) })
    }

    @Test
    fun `a day with no totals inside the window is still offered`() {
        // A day the store never got (the app was not opened) is walked over, not skipped.
        val days = availableDays(today, storedDays(3))

        assertEquals(
            listOf(today, today.minusDays(1), today.minusDays(2), today.minusDays(3)),
            days
        )
    }

    @Test
    fun `a store missing the sixth previous day gives a shorter window`() {
        val days = availableDays(today, storedDays(1, 2, 3, 4))

        assertEquals(
            listOf(today, today.minusDays(1), today.minusDays(2), today.minusDays(3), today.minusDays(4)),
            days
        )
        assertEquals(today.minusDays(4), days.last())
    }

    @Test
    fun `a stored day older than the raw retention never extends the window`() {
        val days = availableDays(today, storedDays(1, 2, 3, 4, 5, 6, 10))

        assertEquals(7, days.size)
        assertEquals(today.minusDays(6), days.last())
        assertEquals(emptyList<LocalDate>(), days.filter { it.isBefore(today.minusDays(6)) })
    }

    @Test
    fun `nothing stored offers today alone`() {
        assertEquals(listOf(today), availableDays(today, emptySet()))
    }

    @Test
    fun `respects a custom window`() {
        assertEquals(
            listOf(today, today.minusDays(1)),
            availableDays(today, storedDays(1, 2), windowDays = 2)
        )
        assertEquals(listOf(today), availableDays(today, storedDays(1), windowDays = 1))
    }

    @Test
    fun `finished days stay the assumption window without today`() {
        assertEquals(
            listOf(
                today.minusDays(1),
                today.minusDays(2),
                today.minusDays(3),
                today.minusDays(4),
                today.minusDays(5),
                today.minusDays(6)
            ),
            finishedDays(today)
        )
        assertEquals(emptyList<LocalDate>(), finishedDays(today, windowDays = 1))
    }
}
