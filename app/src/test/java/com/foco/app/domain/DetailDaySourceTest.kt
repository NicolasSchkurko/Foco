package com.foco.app.domain

import com.foco.app.domain.history.DaySource
import com.foco.app.domain.history.daySource
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** The read rule that decides, per day, whether the detail goes live, to the store, or nowhere. */
class DetailDaySourceTest {
    private val today = LocalDate.of(2026, 10, 19)
    private val storedDays = setOf(today.minusDays(1), today.minusDays(3))

    @Test
    fun `today is live`() {
        assertEquals(DaySource.Live, daySource(today, today, emptySet()))
    }

    @Test
    fun `today stays live even when the store has a row for it`() {
        assertEquals(DaySource.Live, daySource(today, today, setOf(today)))
    }

    @Test
    fun `a day the store has reads from storage`() {
        assertEquals(DaySource.Stored, daySource(today.minusDays(1), today, storedDays))
    }

    @Test
    fun `a stored day the platform no longer serves still reads from storage`() {
        assertEquals(DaySource.Stored, daySource(today.minusDays(3), today, storedDays))
    }

    @Test
    fun `an unknown day is absent`() {
        assertEquals(DaySource.Absent, daySource(today.minusDays(2), today, storedDays))
        assertEquals(DaySource.Absent, daySource(today.plusDays(1), today, storedDays))
    }
}
