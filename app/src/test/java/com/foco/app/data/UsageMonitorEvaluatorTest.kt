package com.foco.app.data

import com.foco.app.domain.AppWindow
import com.foco.app.domain.BlockReason
import com.foco.app.domain.TodayUsageReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * The monitor's decision, exercised without the platform: the fake reader counts the reads, so
 * "the usage is read only when a limit can fire" is pinned.
 */
class UsageMonitorEvaluatorTest {
    private class FakeUsage : TodayUsageReader {
        var millis = 0L
        var reads = 0

        override fun todayUsageMillis(
            packageName: String,
            now: Long,
        ): Long {
            reads++
            return millis
        }
    }

    private val usage = FakeUsage()
    private var windowedMillis = 0L
    private var windowedReads = 0

    private fun usageInsideWindow(
        packageName: String,
        now: Long,
        window: AppWindow,
    ): Long {
        windowedReads++
        return windowedMillis
    }

    private val evaluator = UsageMonitorEvaluator(usage, ::usageInsideWindow)

    private val zone = ZoneId.systemDefault()
    private val day = LocalDate.of(2026, 10, 20)

    private fun at(
        hour: Int,
        minute: Int,
    ): Long {
        val local = day.atTime(LocalTime.of(hour, minute)).atZone(zone)
        return local.toInstant().toEpochMilli()
    }

    @Test
    fun `a package without a limit or a window is never read`() {
        assertNull(evaluator.evaluate("com.example.app", null, null, at(12, 0)))
        assertEquals(0, usage.reads)
    }

    @Test
    fun `a package over its limit is blocked`() {
        usage.millis = 30 * 60 * 1000L

        assertEquals(BlockReason.LIMIT, evaluator.evaluate("com.example.app", 30, null, at(12, 0)))
        assertEquals(1, usage.reads)
    }

    @Test
    fun `a package under its limit is not blocked`() {
        usage.millis = 29 * 60 * 1000L

        assertNull(evaluator.evaluate("com.example.app", 30, null, at(12, 0)))
    }

    @Test
    fun `a zero-minute limit blocks without reading`() {
        assertEquals(BlockReason.LIMIT, evaluator.evaluate("com.example.app", 0, null, at(12, 0)))
        assertEquals(0, usage.reads)
    }

    @Test
    fun `a package outside its window is blocked without reading`() {
        val window = AppWindow.of(9 * 60, 23 * 60)

        val reason = evaluator.evaluate("com.example.app", 30, window, at(23, 30))

        assertEquals(BlockReason.OUTSIDE_WINDOW, reason)
        assertEquals(0, usage.reads)
        assertEquals(0, windowedReads)
    }

    @Test
    fun `inside the window the in-window usage is the one that counts`() {
        val window = AppWindow.of(9 * 60, 23 * 60)
        usage.millis = 60 * 60 * 1000L
        windowedMillis = 10 * 60 * 1000L

        assertNull(evaluator.evaluate("com.example.app", 30, window, at(12, 0)))
        assertEquals("the plain usage is not read for a windowed package", 0, usage.reads)
        assertEquals(1, windowedReads)

        windowedMillis = 30 * 60 * 1000L
        assertEquals(BlockReason.LIMIT, evaluator.evaluate("com.example.app", 30, window, at(12, 0)))
    }

    @Test
    fun `a window without a limit blocks outside and nothing inside`() {
        val window = AppWindow.of(9 * 60, 23 * 60)

        assertEquals(BlockReason.OUTSIDE_WINDOW, evaluator.evaluate("com.example.app", null, window, at(23, 30)))
        assertNull(evaluator.evaluate("com.example.app", null, window, at(12, 0)))
        assertEquals(0, usage.reads)
    }

    @Test
    fun `the seconds used today come from the reader`() {
        usage.millis = 90 * 1000L

        assertEquals(90L, evaluator.usedSecondsToday("com.example.app", at(12, 0)))
    }
}
