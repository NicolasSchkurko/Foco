package com.foco.app.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.foco.app.data.db.history.FocoDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The first bump: what version 1 stored survives, and the day reads as having no stored count. */
@RunWith(AndroidJUnit4::class)
class UnlockMigrationTest {
    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            FocoDatabase::class.java,
        )

    @Test
    fun theUpgradeKeepsWhatVersionOneStored() {
        helper.createDatabase(DB_NAME, 1).use { versionOne ->
            versionOne.execSQL(
                "INSERT INTO session_record (packageName, `start`, `end`) VALUES ('com.a', 100, 200)",
            )
            versionOne.execSQL(
                "INSERT INTO daily_aggregate (packageName, day, totalMillis) " +
                    "VALUES ('com.a', '2026-10-18', 5000)",
            )
        }

        val migrated = helper.runMigrationsAndValidate(DB_NAME, 2, true, FocoDatabase.MIGRATION_1_2)

        assertEquals(1, migrated.countOf("session_record"))
        assertEquals(1, migrated.countOf("daily_aggregate"))
        assertEquals(0, migrated.countOf("daily_unlock"))
        migrated.close()
    }

    private fun SupportSQLiteDatabase.countOf(table: String): Int = query("SELECT * FROM $table").use { it.count }

    private companion object {
        const val DB_NAME = "unlock-migration-test.db"
    }
}
