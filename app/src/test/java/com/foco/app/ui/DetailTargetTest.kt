package com.foco.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DetailTargetTest {
    @Test
    fun theDetailRouteCarriesTheDateOnlyWhenItIsThere() {
        val day = LocalDate.of(2026, 10, 21)

        assertEquals("detail/com.example.app", detailRoute(DetailTarget("com.example.app")))
        assertEquals("detail/com.example.app?date=2026-10-21", detailRoute(DetailTarget("com.example.app", day)))
    }

    @Test
    fun aMalformedDateArgumentIsIgnored() {
        assertNull(parseRouteDate("not-a-day"))
        assertNull(parseRouteDate(""))
        assertNull(parseRouteDate(null))
        assertEquals(LocalDate.of(2026, 10, 21), parseRouteDate("2026-10-21"))
    }
}
