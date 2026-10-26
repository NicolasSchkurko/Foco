package com.foco.app.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.foco.app.data.db.limits.AppLimitDatabase
import com.foco.app.data.stores.AppLimitsStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** Category: local store (API 36). The alert flag, the fired tiers and their reset rules. */
@RunWith(AndroidJUnit4::class)
class AppLimitAlertStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val databaseName = "limit-alerts-store-test.db"
    private val day = LocalDate.of(2026, 10, 20)
    private var database: AppLimitDatabase? = null

    @Before
    fun setUp() {
        context.deleteDatabase(databaseName)
        database = open()
    }

    @After
    fun cleanUp() {
        database?.close()
        context.deleteDatabase(databaseName)
    }

    private fun open(): AppLimitDatabase = Room.databaseBuilder(context, AppLimitDatabase::class.java, databaseName).build()

    private fun store(): AppLimitsStore = AppLimitsStore(database!!.appLimits(), database!!.appLimitAlerts(), database!!.appLimitWindows())

    @Test
    fun theAlertFlagStartsOff() =
        runBlocking {
            val store = store()
            store.setLimit("com.instagram.android", 30, day)

            assertFalse(store.alertEnabledFor("com.instagram.android"))
            assertEquals(mapOf("com.instagram.android" to false), store.alertsFlow.first())
        }

    @Test
    fun enablingOnlyWorksWithAPositiveLimit() =
        runBlocking {
            val store = store()
            store.setLimit("com.a", 0, day)
            store.setAlertEnabled("com.a", true, day)
            assertFalse(store.alertEnabledFor("com.a"))

            store.setAlertEnabled("com.unknown", true, day)
            assertEquals(mapOf("com.a" to false), store.alertsFlow.first())

            store.setLimit("com.a", 30, day)
            store.setAlertEnabled("com.a", true, day)
            assertTrue(store.alertEnabledFor("com.a"))
        }

    @Test
    fun firedTiersAreScopedByPackageAndDay() =
        runBlocking {
            val store = store()
            store.setLimit("com.a", 30, day)
            store.markFired("com.a", day, listOf(50, 75))

            assertEquals(setOf(50, 75), store.firedTiers("com.a", day))
            assertEquals(emptySet<Int>(), store.firedTiers("com.a", day.plusDays(1)))
            assertEquals(emptySet<Int>(), store.firedTiers("com.b", day))
        }

    @Test
    fun changingTheLimitClearsTheDaysFiredTiers() =
        runBlocking {
            val store = store()
            store.setLimit("com.a", 30, day)
            store.markFired("com.a", day, listOf(50))

            store.setLimit("com.a", 60, day)

            assertEquals(emptySet<Int>(), store.firedTiers("com.a", day))
        }

    @Test
    fun changingTheToggleClearsTheDaysFiredTiers() =
        runBlocking {
            val store = store()
            store.setLimit("com.a", 30, day)
            store.markFired("com.a", day, listOf(50, 75))

            store.setAlertEnabled("com.a", true, day)

            assertEquals(emptySet<Int>(), store.firedTiers("com.a", day))
        }

    @Test
    fun zeroTurnsAlertsOffWhenTheLimitChanges() =
        runBlocking {
            val store = store()
            store.setLimit("com.a", 30, day)
            store.setAlertEnabled("com.a", true, day)

            store.setLimit("com.a", 0, day)

            assertFalse(store.alertEnabledFor("com.a"))
        }

    @Test
    fun clearingTheLimitRemovesAlertsAndFiredTiers() =
        runBlocking {
            val store = store()
            store.setLimit("com.a", 30, day)
            store.setAlertEnabled("com.a", true, day)
            store.markFired("com.a", day, listOf(50))

            store.clearLimit("com.a")

            assertNull(store.limitFor("com.a"))
            assertFalse(store.alertEnabledFor("com.a"))
            assertEquals(emptySet<Int>(), store.firedTiers("com.a", day))
        }

    @Test
    fun firedTiersSurviveClosingAndReopeningTheDatabase() =
        runBlocking {
            store().markFired("com.a", day, listOf(90))
            database!!.close()

            database = open()

            assertEquals(setOf(90), store().firedTiers("com.a", day))
        }
}
