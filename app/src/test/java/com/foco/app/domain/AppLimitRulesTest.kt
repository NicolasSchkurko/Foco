package com.foco.app.domain

import com.foco.app.model.UsageSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLimitRulesTest {
    /** 09:00:00 — the moment Foco's countdown ended and HOME was sent. */
    private val blockFinished = 9 * 60 * 60 * 1000L
    private val firstRecheck = blockFinished + 2_000L
    private val secondRecheck = blockFinished + 6_000L

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
        val sessions =
            listOf(
                UsageSession("instagram", 0, 10 * 60_000L),
                UsageSession("youtube", 10 * 60_000L, 40 * 60_000L),
            )

        assertEquals(10 * 60L, AppLimitRules.usedSeconds(sessions, "instagram"))
        assertEquals(30 * 60L, AppLimitRules.usedSeconds(sessions, "youtube"))
        assertEquals(0L, AppLimitRules.usedSeconds(sessions, "other"))
    }

    @Test
    fun `sub-second sessions are not truncated away`() {
        val sessions =
            listOf(
                UsageSession("instagram", 0, 1_500L),
                UsageSession("instagram", 2_000L, 3_500L),
            )

        assertEquals(3L, AppLimitRules.usedSeconds(sessions, "instagram"))
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

    @Test
    fun `a reopen merged into the pre-block session still counts as foreground at the re-check`() {
        // Used before the block (08:59:00), reopened in the same minute → one merged session.
        val mergedReopen = UsageSession("instagram", blockFinished - 60_000, blockFinished + 30_000)

        assertTrue(AppLimitRules.isForegroundAt(listOf(mergedReopen), "instagram", firstRecheck))
        assertTrue(AppLimitRules.isForegroundAt(listOf(mergedReopen), "instagram", secondRecheck))
    }

    @Test
    fun `a session ended at HOME before the re-check is not foreground`() {
        val endedAtHome = UsageSession("instagram", blockFinished - 60_000, blockFinished)

        assertFalse(AppLimitRules.isForegroundAt(listOf(endedAtHome), "instagram", firstRecheck))
        assertFalse(AppLimitRules.isForegroundAt(listOf(endedAtHome), "instagram", secondRecheck))
    }

    @Test
    fun `another package covering the re-check does not make this package foreground`() {
        val sessions =
            listOf(
                UsageSession("youtube", blockFinished - 60_000, blockFinished + 30_000),
                UsageSession("instagram", blockFinished - 60_000, blockFinished),
            )

        assertFalse(AppLimitRules.isForegroundAt(sessions, "instagram", firstRecheck))
        assertTrue(AppLimitRules.isForegroundAt(sessions, "youtube", firstRecheck))
    }

    @Test
    fun `no sessions at all means nothing is foreground`() {
        assertFalse(AppLimitRules.isForegroundAt(emptyList(), "instagram", firstRecheck))
    }

    @Test
    fun `a session ending exactly at the re-check instant still counts as foreground`() {
        val endsAtRecheck = UsageSession("instagram", firstRecheck - 5_000L, firstRecheck)

        assertTrue(AppLimitRules.isForegroundAt(listOf(endsAtRecheck), "instagram", firstRecheck))
    }

    @Test
    fun `the end instant is foreground and the next millisecond is not`() {
        val session = UsageSession("instagram", 0, 1_000L)

        assertTrue(AppLimitRules.isForegroundAt(listOf(session), "instagram", 1_000L))
        assertFalse(AppLimitRules.isForegroundAt(listOf(session), "instagram", 1_001L))
    }
}
