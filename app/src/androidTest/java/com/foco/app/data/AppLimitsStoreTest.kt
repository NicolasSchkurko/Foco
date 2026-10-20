package com.foco.app.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.foco.app.data.db.AppLimitDatabase
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

/** Category: local store (API 36). The real `limits.db` file survives closing and reopening. */
@RunWith(AndroidJUnit4::class)
class AppLimitsStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val databaseName = "limits-store-test.db"
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

    @Test
    fun setReadAndClearOneLimit() =
        runBlocking {
            val store = AppLimitsStore(database!!.appLimits(), database!!.appLimitAlerts())

            store.setLimit("com.instagram.android", 30)
            assertEquals(30, store.limitFor("com.instagram.android"))

            store.clearLimit("com.instagram.android")
            assertNull(store.limitFor("com.instagram.android"))
            assertFalse(store.hasAnyLimit())
        }

    @Test
    fun theFlowCarriesEveryLimitByPackage() =
        runBlocking {
            val store = AppLimitsStore(database!!.appLimits(), database!!.appLimitAlerts())

            store.setLimit("com.instagram.android", 30)
            store.setLimit("com.youtube", 0)

            assertEquals(
                mapOf("com.instagram.android" to 30, "com.youtube" to 0),
                store.limitsFlow.first(),
            )
            assertTrue(store.hasAnyLimit())
        }

    @Test
    fun replacingOneLimitKeepsTheOthers() =
        runBlocking {
            val store = AppLimitsStore(database!!.appLimits(), database!!.appLimitAlerts())
            store.setLimit("com.instagram.android", 30)
            store.setLimit("com.youtube", 15)

            store.setLimit("com.instagram.android", 60)

            assertEquals(60, store.limitFor("com.instagram.android"))
            assertEquals(15, store.limitFor("com.youtube"))
        }

    @Test
    fun aLimitSurvivesClosingAndReopeningTheDatabase() =
        runBlocking {
            AppLimitsStore(database!!.appLimits(), database!!.appLimitAlerts()).setLimit("com.instagram.android", 30)
            database!!.close()

            database = open()

            assertEquals(30, AppLimitsStore(database!!.appLimits(), database!!.appLimitAlerts()).limitFor("com.instagram.android"))
        }

    @Test
    fun onlyTheSelectableGridIsStored() =
        runBlocking {
            val store = AppLimitsStore(database!!.appLimits(), database!!.appLimitAlerts())

            runCatching { store.setLimit("com.instagram.android", 20) }

            assertNull(store.limitFor("com.instagram.android"))
            assertEquals(0, database!!.appLimits().count())
        }
}
