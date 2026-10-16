package com.foco.app.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class DaySelectionTest {

    private val today = LocalDate.of(2026, 10, 16)

    @Test
    fun `offers today first plus the previous window days`() {
        val days = availableDays(today)

        assertEquals(7, days.size)
        assertEquals(today, days.first())
        assertEquals(today.minusDays(6), days.last())
    }

    @Test
    fun `respects a custom window`() {
        assertEquals(listOf(today, today.minusDays(1)), availableDays(today, windowDays = 2))
    }

    @Test
    fun `never offers future days nor days older than the window`() {
        val days = availableDays(today)

        assertEquals(emptyList<LocalDate>(), days.filter { it.isAfter(today) })
        assertEquals(emptyList<LocalDate>(), days.filter { it.isBefore(today.minusDays(6)) })
    }
}
