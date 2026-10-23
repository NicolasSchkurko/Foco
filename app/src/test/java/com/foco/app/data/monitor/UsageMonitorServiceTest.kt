package com.foco.app.data.monitor

import com.foco.app.domain.limits.AppLimitRules
import com.foco.app.domain.limits.AppLimitWindowRules
import com.foco.app.domain.limits.AppWindow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * The limit decision, on the JVM: the tier arithmetic, the alert bookkeeping and the window rule
 * are pure, so they run here and the device suite keeps only what the device decides.
 */
class UsageMonitorServiceTest {
    @Test
    fun theCheckAlertsOnlyTheHighestTierAndReturnsEveryCrossedOne() {
        val notified = mutableListOf<Int>()
        val blocked = mutableListOf<Boolean>()

        val crossed =
            evaluateLimitCheck(
                foregroundPackage = "com.example.app",
                limitMinutes = 60,
                usedSeconds = 48 * 60, // 80 %
                firedTiers = setOf(50),
                onNotify = { notified += it },
                onBlock = { blocked += true },
            )

        assertEquals(listOf(50, 75), crossed)
        assertEquals(listOf(75), notified)
        assertTrue(blocked.isEmpty())
    }

    @Test
    fun reachingOneHundredPercentBlocksAndDoesNotAlert() {
        val notified = mutableListOf<Int>()
        val blocked = mutableListOf<Boolean>()

        val crossed =
            evaluateLimitCheck(
                foregroundPackage = "com.example.app",
                limitMinutes = 60,
                usedSeconds = 60 * 60,
                firedTiers = setOf(50, 75, 90),
                onNotify = { notified += it },
                onBlock = { blocked += true },
            )

        assertTrue(crossed.isEmpty())
        assertTrue(notified.isEmpty())
        assertEquals(listOf(true), blocked)
    }

    @Test
    fun zeroMinuteLimitBlocksAtOnce() {
        val blocked = mutableListOf<Boolean>()

        evaluateLimitCheck(
            foregroundPackage = "com.example.app",
            limitMinutes = 0,
            usedSeconds = 0,
            firedTiers = emptySet(),
            onNotify = { },
            onBlock = { blocked += true },
        )

        assertEquals(listOf(true), blocked)
    }

    @Test
    fun anOvernightWindowBlocksDuringTheDayOutsideIt() {
        val window = AppWindow.of(22 * 60, 6 * 60)!! // 22:00 to 06:00
        val zone = ZoneId.systemDefault()
        val afternoon =
            LocalTime
                .of(14, 0)
                .atDate(LocalDate.now())
                .atZone(zone)
                .toInstant()
                .toEpochMilli()
        val night =
            LocalTime
                .of(23, 0)
                .atDate(LocalDate.now())
                .atZone(zone)
                .toInstant()
                .toEpochMilli()

        assertTrue(AppLimitWindowRules.blocksAt(afternoon, window, zone))
        assertFalse(AppLimitWindowRules.blocksAt(night, window, zone))
    }

    private fun evaluateLimitCheck(
        foregroundPackage: String,
        limitMinutes: Int,
        usedSeconds: Long,
        firedTiers: Set<Int>,
        onNotify: (Int) -> Unit,
        onBlock: () -> Unit,
    ): List<Int> {
        if (AppLimitRules.isAtOrOverLimit(limitMinutes, usedSeconds)) {
            onBlock()
            return emptyList()
        }
        val crossed = AppLimitRules.crossedAlertTiers(limitMinutes, usedSeconds)
        val highest = crossed.lastOrNull()
        if (highest != null && highest !in firedTiers) {
            onNotify(highest)
        }
        return crossed
    }
}
