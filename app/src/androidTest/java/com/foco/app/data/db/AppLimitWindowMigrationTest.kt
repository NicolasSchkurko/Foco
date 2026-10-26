package com.foco.app.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.foco.app.data.db.limits.AppLimitDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The version-2 to 3 upgrade adds `app_limit_window` and keeps every existing limit. */
@RunWith(AndroidJUnit4::class)
class AppLimitWindowMigrationTest {
    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            AppLimitDatabase::class.java,
        )

    private val dbName = "app-limit-window-migration-${System.nanoTime()}.db"

    @Test
    fun theUpgradeKeepsLimitsAndAddsAnEmptyWindowTable() {
        helper.createDatabase(dbName, 2).use { versionTwo ->
            versionTwo.execSQL(
                "INSERT INTO app_limit (packageName, minutes, alertEnabled) VALUES ('com.a', 30, 1)",
            )
        }

        val migrated = helper.runMigrationsAndValidate(dbName, 3, true, AppLimitDatabase.MIGRATION_2_3)

        assertEquals(1, migrated.countOf("app_limit"))
        assertEquals(0, migrated.countOf("app_limit_window"))
        migrated.close()
    }

    private fun SupportSQLiteDatabase.countOf(table: String): Int = query("SELECT * FROM `$table`").use { it.count }
}
