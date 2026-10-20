package com.foco.app.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The version-2 to 3 upgrade: counts and totals survive, totals-only days become null counts. */
@RunWith(AndroidJUnit4::class)
class ConsolidationMigrationTest {
    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            FocoDatabase::class.java,
        )

    private val dbName = "consolidation-migration-${System.nanoTime()}.db"

    @Test
    fun theUpgradeKeepsSessionsTotalsAndKnownCounts() {
        helper.createDatabase(dbName, 2).use { versionTwo ->
            versionTwo.execSQL(
                "INSERT INTO session_record (packageName, `start`, `end`) VALUES ('com.a', 100, 200)",
            )
            versionTwo.execSQL(
                "INSERT INTO daily_aggregate (packageName, day, totalMillis) " +
                    "VALUES ('com.a', '2026-10-18', 5000)",
            )
            versionTwo.execSQL(
                "INSERT INTO daily_aggregate (packageName, day, totalMillis) " +
                    "VALUES ('com.b', '2026-10-17', 3000)",
            )
            versionTwo.execSQL(
                "INSERT INTO daily_unlock (day, `count`) VALUES ('2026-10-18', 3)",
            )
        }

        val migrated = helper.runMigrationsAndValidate(dbName, 3, true, FocoDatabase.MIGRATION_2_3)

        assertEquals(1, migrated.countOf("session_record"))
        assertEquals(2, migrated.countOf("daily_aggregate"))
        assertEquals(2, migrated.countOf("consolidated_day"))
        assertEquals(3, migrated.intAt("2026-10-18"))
        assertTrue("a totals-only day has no known count", migrated.nullAt("2026-10-17"))
        assertEquals(0, migrated.tableCount("daily_unlock"))
        migrated.close()
    }

    private fun SupportSQLiteDatabase.countOf(table: String): Int = query("SELECT * FROM `$table`").use { it.count }

    private fun SupportSQLiteDatabase.tableCount(table: String): Int =
        query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?", arrayOf(table))
            .use { it.count }

    private fun SupportSQLiteDatabase.intAt(day: String): Int =
        query("SELECT `count` FROM consolidated_day WHERE day = ?", arrayOf(day)).use {
            it.moveToFirst()
            it.getInt(0)
        }

    private fun SupportSQLiteDatabase.nullAt(day: String): Boolean =
        query("SELECT `count` FROM consolidated_day WHERE day = ?", arrayOf(day)).use {
            it.moveToFirst()
            it.isNull(0)
        }
}
