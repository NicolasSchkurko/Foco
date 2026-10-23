package com.foco.app.data.monitor

import com.foco.app.domain.history.TodayUsageReader
import com.foco.app.domain.limits.AppWindow
import com.foco.app.domain.limits.BlockReason
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

    @Test
    fun `no limit and no window returns null and does not read usage`() {
        val usage = FakeUsage()
        val evaluator = UsageMonitorEvaluator(usage, { _, _, _ -> 0L })

        val result = evaluator.evaluate("app.a", limitMinutes = null, window = null)

        assertNull(result)
        assertEquals(0, usage.reads)
    }

    @Test
    fun `a zero minute limit blocks immediately without reading usage`() {
        val usage = FakeUsage()
        val evaluator = UsageMonitorEvaluator(usage, { _, _, _ -> 0L })

        val result = evaluator.evaluate("app.a", limitMinutes = 0, window = null)

        assertEquals(BlockReason.LIMIT, result)
        assertEquals(0, usage.reads)
    }

    @Test
    fun `outside window blocks before reading limit or usage`() {
        val usage = FakeUsage()
        val evaluator = UsageMonitorEvaluator(usage, { _, _, _ -> 0L })
        val window = AppWindow.of(9 * 60, 17 * 60)!!
        val zone = ZoneId.systemDefault()
        val outside =
            LocalTime
                .of(8, 0)
                .atDate(LocalDate.now())
                .atZone(zone)
                .toInstant()
                .toEpochMilli()

        val result = evaluator.evaluate("app.a", limitMinutes = 60, window = window, now = outside)

        assertEquals(BlockReason.OUTSIDE_WINDOW, result)
        assertEquals(0, usage.reads)
    }

    @Test
    fun `usage at or over limit blocks`() {
        val usage = FakeUsage().apply { millis = 60 * 60_000L }
        val evaluator = UsageMonitorEvaluator(usage, { _, _, _ -> 0L })

        val result = evaluator.evaluate("app.a", limitMinutes = 60, window = null)

        assertEquals(BlockReason.LIMIT, result)
        assertEquals(1, usage.reads)
    }

    @Test
    fun `usage under limit returns null`() {
        val usage = FakeUsage().apply { millis = 59 * 60_000L }
        val evaluator = UsageMonitorEvaluator(usage, { _, _, _ -> 0L })

        val result = evaluator.evaluate("app.a", limitMinutes = 60, window = null)

        assertNull(result)
        assertEquals(1, usage.reads)
    }
}
