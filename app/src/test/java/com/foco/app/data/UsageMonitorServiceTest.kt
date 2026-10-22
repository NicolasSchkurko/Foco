package com.foco.app.data

import com.foco.app.domain.AppLimitWindowRules
import com.foco.app.domain.AppWindow
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
                limitMinutes = 30,
                alertEnabled = true,
                usedSeconds = 1_700,
                firedTiers = emptySet(),
                onBlock = { blocked += true },
                onNotify = { tier, _ ->
                    notified += tier
                    true
                },
            )

        assertEquals(listOf(90), notified)
        assertEquals(listOf(50, 75, 90), crossed)
        assertTrue(blocked.isEmpty())
    }

    @Test
    fun aFiredTierDoesNotAlertTwice() {
        val notified = mutableListOf<Int>()

        val first =
            evaluateLimitCheck(
                foregroundPackage = "com.example.app",
                limitMinutes = 30,
                alertEnabled = true,
                usedSeconds = 1_350,
                firedTiers = setOf(50),
                onBlock = {},
                onNotify = { tier, _ ->
                    notified += tier
                    true
                },
            )
        assertEquals(listOf(75), notified)
        assertEquals(listOf(50, 75), first)

        notified.clear()
        val second =
            evaluateLimitCheck(
                foregroundPackage = "com.example.app",
                limitMinutes = 30,
                alertEnabled = true,
                usedSeconds = 1_350,
                firedTiers = setOf(50, 75),
                onBlock = {},
                onNotify = { tier, _ ->
                    notified += tier
                    true
                },
            )
        assertTrue(notified.isEmpty())
        assertTrue(second.isEmpty())
    }

    @Test
    fun noForegroundOrNoAlertFlagNeverNotifies() {
        val notified = mutableListOf<Int>()

        val withoutForeground =
            evaluateLimitCheck(
                foregroundPackage = null,
                limitMinutes = 30,
                alertEnabled = true,
                usedSeconds = 1_500,
                firedTiers = emptySet(),
                onBlock = {},
                onNotify = { tier, _ ->
                    notified += tier
                    true
                },
            )
        val alertsOff =
            evaluateLimitCheck(
                foregroundPackage = "com.example.app",
                limitMinutes = 30,
                alertEnabled = false,
                usedSeconds = 1_500,
                firedTiers = emptySet(),
                onBlock = {},
                onNotify = { tier, _ ->
                    notified += tier
                    true
                },
            )

        assertTrue(notified.isEmpty())
        assertTrue(withoutForeground.isEmpty())
        assertTrue(alertsOff.isEmpty())
    }

    @Test
    fun atOrOverTheLimitBlocksAndNeverNotifies() {
        val notified = mutableListOf<Int>()
        val blocked = mutableListOf<Boolean>()

        val crossed =
            evaluateLimitCheck(
                foregroundPackage = "com.example.app",
                limitMinutes = 30,
                alertEnabled = true,
                usedSeconds = 1_800,
                firedTiers = emptySet(),
                onBlock = { blocked += true },
                onNotify = { tier, _ ->
                    notified += tier
                    true
                },
            )

        assertEquals(listOf(true), blocked)
        assertTrue(notified.isEmpty())
        assertTrue(crossed.isEmpty())
    }

    @Test
    fun aDeniedNotificationLeavesTheTierAvailable() {
        var attempts = 0

        val crossed =
            evaluateLimitCheck(
                foregroundPackage = "com.example.app",
                limitMinutes = 30,
                alertEnabled = true,
                usedSeconds = 1_620,
                firedTiers = emptySet(),
                onBlock = {},
                onNotify = { _, _ ->
                    attempts += 1
                    false
                },
            )

        assertEquals(1, attempts)
        assertTrue(crossed.isEmpty())
    }

    @Test
    fun aWindowBlocksOutsideAndLetsTheAppThroughInside() {
        val zone = ZoneId.of("UTC")
        val day = LocalDate.of(2026, 10, 20)

        fun at(time: String): Long =
            day
                .atTime(LocalTime.parse(time))
                .atZone(zone)
                .toInstant()
                .toEpochMilli()
        val window = AppWindow(9 * 60, 23 * 60)

        assertFalse(AppLimitWindowRules.blocksAt(at("10:00"), window, zone))
        assertTrue(AppLimitWindowRules.blocksAt(at("23:00"), window, zone))
        assertFalse(AppLimitWindowRules.blocksAt(at("23:00"), null, zone))
    }
}
