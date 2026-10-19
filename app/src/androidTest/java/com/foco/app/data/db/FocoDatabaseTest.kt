package com.foco.app.data.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Only what the JVM fakes cannot prove: SQLite enforcing both composite keys and `REPLACE`
 * overwriting instead of failing.
 */
@RunWith(AndroidJUnit4::class)
class FocoDatabaseTest {

    private lateinit var database: FocoDatabase

    @Before
    fun open() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            FocoDatabase::class.java
        ).build()
    }

    @After
    fun close() {
        database.close()
    }

    @Test
    fun aSessionIsIdentifiedByPackageAndStart() = runBlocking {
        val dao = database.sessionRecords()

        dao.upsert(listOf(SessionRecord("com.a", 100, 200), SessionRecord("com.b", 100, 150)))
        assertEquals(2, dao.sessionsBetween(0, Long.MAX_VALUE).size)

        dao.upsert(listOf(SessionRecord("com.a", 100, 300)))
        val a = dao.sessionsBetween(0, Long.MAX_VALUE).filter { it.packageName == "com.a" }

        assertEquals(1, a.size)
        assertEquals(300, a.single().end)
    }

    @Test
    fun aDailyTotalIsOneRowPerAppAndDay() = runBlocking {
        val dao = database.dailyAggregates()

        dao.upsert(listOf(DailyAggregate("com.a", "2026-10-18", 5_000)))
        dao.upsert(listOf(DailyAggregate("com.a", "2026-10-18", 8_000)))
        dao.upsert(listOf(DailyAggregate("com.a", "2026-10-17", 2_000)))

        assertEquals(listOf("2026-10-17", "2026-10-18"), dao.storedDays().sorted())
        assertEquals(8_000L, dao.totalsFor("2026-10-18", "2026-10-18").single().totalMillis)
        assertEquals(2, dao.totalsFor("2026-10-17", "2026-10-18").size)
    }

    @Test
    fun pruningRemovesOnlyRawRowsBeforeTheCutoff() = runBlocking {
        val dao = database.sessionRecords()

        dao.upsert(listOf(SessionRecord("com.a", 100, 200), SessionRecord("com.a", 500, 600)))

        assertEquals(1, dao.pruneBefore(300))
        assertEquals(listOf(500L), dao.sessionsBetween(0, Long.MAX_VALUE).map { it.start })
    }
}
