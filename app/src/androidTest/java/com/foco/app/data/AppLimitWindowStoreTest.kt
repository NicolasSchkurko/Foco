package com.foco.app.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.foco.app.data.db.AppLimitDatabase
import com.foco.app.data.db.AppLimitWindow
import com.foco.app.domain.AppWindow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Category: local store (API 36). The real `limits.db` file keeps a window with or without a limit. */
@RunWith(AndroidJUnit4::class)
class AppLimitWindowStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val databaseName = "limits-window-store-test.db"
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
    fun setReadAndClearOneWindow() =
        runBlocking {
            val store = store()

            store.setWindow("com.instagram.android", 9 * 60, 23 * 60)
            assertEquals(AppWindow(9 * 60, 23 * 60), store.windowFor("com.instagram.android"))
            assertTrue(store.hasAnyLimitOrWindow())

            store.clearWindow("com.instagram.android")
            assertNull(store.windowFor("com.instagram.android"))
            assertTrue(!store.hasAnyLimitOrWindow())
        }

    @Test
    fun aWindowLivesWithoutALimit() =
        runBlocking {
            val store = store()

            store.setWindow("com.instagram.android", 22 * 60, 6 * 60)

            assertNull(store.limitFor("com.instagram.android"))
            assertEquals(AppWindow(22 * 60, 6 * 60), store.windowFor("com.instagram.android"))
        }

    @Test
    fun onlyTheSelectableGridWithDifferentEndsIsStored() =
        runBlocking {
            val store = store()

            runCatching { store.setWindow("com.instagram.android", 20, 23 * 60) }
            runCatching { store.setWindow("com.instagram.android", 9 * 60, 9 * 60) }

            assertNull(store.windowFor("com.instagram.android"))
            assertEquals(0, database!!.appLimitWindows().count())
        }

    @Test
    fun theFlowCarriesEveryWindowByPackage() =
        runBlocking {
            val store = store()

            store.setWindow("com.instagram.android", 9 * 60, 23 * 60)
            store.setWindow("com.youtube", 22 * 60, 6 * 60)

            assertEquals(
                mapOf(
                    "com.instagram.android" to AppWindow(9 * 60, 23 * 60),
                    "com.youtube" to AppWindow(22 * 60, 6 * 60),
                ),
                store.windowsFlow.first(),
            )
        }

    @Test
    fun aWindowSurvivesClosingAndReopeningTheDatabase() =
        runBlocking {
            store().setWindow("com.instagram.android", 9 * 60, 23 * 60)
            database!!.close()

            database = open()

            assertEquals(AppWindow(9 * 60, 23 * 60), store().windowFor("com.instagram.android"))
        }

    @Test
    fun aStoredRowThatIsNotAWindowIsSkipped() =
        runBlocking {
            database!!.appLimitWindows().upsert(AppLimitWindow("com.instagram.android", 9 * 60, 9 * 60))

            assertNull("an invalid row is not a window", store().windowFor("com.instagram.android"))
            assertEquals(emptyMap<String, AppWindow>(), store().windowsFlow.first())
        }
}
