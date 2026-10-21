package com.foco.app.ui

import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class FormattingTest {

    @Test
    fun englishAppLocaleOnSpanishDeviceGivesEnglishLabels() {
        val d = LocalDate.of(2026, 10, 21)
        val today = LocalDate.of(2026, 10, 21)

        assertEquals("Wed", formatWeekday(d, Locale.ENGLISH))
        assertEquals("Today", formatDayLabel(d, today, "Today", "Yesterday", Locale.ENGLISH))
        assertEquals("Mon 19 Oct", formatDayLabel(d.minusDays(2), today, "Today", "Yesterday", Locale.ENGLISH))
    }

    @Test
    fun spanishAppLocaleGivesSpanishLabels() {
        val d = LocalDate.of(2026, 10, 21)
        val today = LocalDate.of(2026, 10, 21)

        assertEquals("mié", formatWeekday(d, Locale.forLanguageTag("es")))
        assertEquals("Hoy", formatDayLabel(d, today, "Hoy", "Ayer", Locale.forLanguageTag("es")))
    }

    @Test
    fun patternsAreByteIdenticalForFixedInputAndLocale() {
        val d = LocalDate.of(2026, 10, 21)
        val today = LocalDate.of(2026, 10, 24)
        // Pattern pins verified against current output: short weekday, then abbreviated day.
        assertEquals("Wed", formatWeekday(d, Locale.ENGLISH))
        assertEquals("Wed 21 Oct", formatDayLabel(d, today, "T", "Y", Locale.ENGLISH))
    }
}
