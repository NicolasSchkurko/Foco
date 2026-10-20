package com.foco.app.domain

import com.foco.app.model.UsageSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLimitRulesTest {

    @Test
    fun `a package without a limit is never blocked`() {
        assertFalse(AppLimitRules.isAtOrOverLimit(null, 0))
        assertFalse(AppLimitRules.isAtOrOverLimit(null, Long.MAX_VALUE))
    }

    @Test
    fun `thirty minutes block at the boundary, not one second before`() {
        assertFalse(AppLimitRules.isAtOrOverLimit(30, 29 * 60L + 59))
        assertTrue(AppLimitRules.isAtOrOverLimit(30, 30 * 60L))
        assertTrue(AppLimitRules.isAtOrOverLimit(30, 30 * 60L + 1))
    }

    @Test
    fun `zero blocks as soon as the app comes to the foreground`() {
        assertTrue(AppLimitRules.isAtOrOverLimit(0, 0))
    }

    @Test
    fun `the same limit is available again with the new day's usage`() {
        val limit = 30
        val yesterday = listOf(UsageSession("instagram", 0, 45 * 60_000L))
        val today = emptyList<UsageSession>()

        assertTrue(AppLimitRules.isAtOrOverLimit(limit, AppLimitRules.usedSeconds(yesterday, "instagram")))
        assertFalse(AppLimitRules.isAtOrOverLimit(limit, AppLimitRules.usedSeconds(today, "instagram")))
    }

    @Test
    fun `only the package's own sessions count`() {
        val sessions = listOf(
            UsageSession("instagram", 0, 10 * 60_000L),
            UsageSession("youtube", 10 * 60_000L, 40 * 60_000L)
        )

        assertEquals(10 * 60L, AppLimitRules.usedSeconds(sessions, "instagram"))
        assertEquals(30 * 60L, AppLimitRules.usedSeconds(sessions, "youtube"))
        assertEquals(0L, AppLimitRules.usedSeconds(sessions, "other"))
    }

    @Test
    fun `the picker offers zero and 15-minute steps up to 24 hours`() {
        val options = AppLimitRules.options()

        assertEquals(0, options.first())
        assertEquals(15, options[1])
        assertEquals(24 * 60, options.last())
        assertEquals(1 + 24 * 4, options.size)
        assertTrue(options.all(AppLimitRules::isSelectable))
    }

    @Test
    fun `only zero and the 15-minute grid are selectable`() {
        assertTrue(AppLimitRules.isSelectable(0))
        assertTrue(AppLimitRules.isSelectable(15))
        assertTrue(AppLimitRules.isSelectable(24 * 60))

        assertFalse(AppLimitRules.isSelectable(7))
        assertFalse(AppLimitRules.isSelectable(20))
        assertFalse(AppLimitRules.isSelectable(-15))
        assertFalse(AppLimitRules.isSelectable(24 * 60 + 15))
    }
}
