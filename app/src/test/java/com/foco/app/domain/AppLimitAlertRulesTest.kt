package com.foco.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The pure alert thresholds: 50/75/90, highest crossed, none without a limit or at 100 %. */
class AppLimitAlertRulesTest {

    @Test
    fun noLimitOrZeroNeverAlerts() {
        assertEquals(emptyList<Int>(), AppLimitRules.crossedAlertTiers(null, 10_000))
        assertEquals(emptyList<Int>(), AppLimitRules.crossedAlertTiers(0, 10_000))
    }

    @Test
    fun theThreeTiersAppearInOrderAtTheirPercentages() {
        val limit = 30
        assertEquals(emptyList<Int>(), AppLimitRules.crossedAlertTiers(limit, 899))
        assertEquals(listOf(50), AppLimitRules.crossedAlertTiers(limit, 900))
        assertEquals(listOf(50), AppLimitRules.crossedAlertTiers(limit, 1_349))
        assertEquals(listOf(50, 75), AppLimitRules.crossedAlertTiers(limit, 1_350))
        assertEquals(listOf(50, 75), AppLimitRules.crossedAlertTiers(limit, 1_619))
        assertEquals(listOf(50, 75, 90), AppLimitRules.crossedAlertTiers(limit, 1_620))
    }

    @Test
    fun aJumpStillReportsEveryCrossedTier() {
        assertEquals(listOf(50, 75, 90), AppLimitRules.crossedAlertTiers(30, 1_700))
    }

    @Test
    fun atOrOverTheLimitAlwaysYieldsNoTier() {
        assertEquals(emptyList<Int>(), AppLimitRules.crossedAlertTiers(30, 1_800))
        assertEquals(emptyList<Int>(), AppLimitRules.crossedAlertTiers(30, 1_801))
    }

    @Test
    fun noUsageYieldsNoTier() {
        assertEquals(emptyList<Int>(), AppLimitRules.crossedAlertTiers(30, 0))
        assertEquals(emptyList<Int>(), AppLimitRules.crossedAlertTiers(30, -10))
    }

    @Test
    fun aOneMinuteLimitUsesTheSamePercentages() {
        assertEquals(listOf(50), AppLimitRules.crossedAlertTiers(1, 30))
        assertEquals(listOf(50, 75), AppLimitRules.crossedAlertTiers(1, 45))
        assertEquals(listOf(50, 75, 90), AppLimitRules.crossedAlertTiers(1, 54))
        assertTrue(AppLimitRules.crossedAlertTiers(1, 60).isEmpty())
    }
}
