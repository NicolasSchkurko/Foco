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

/** The version 2 bump: stored limits keep their minutes, alerts start off and fired tiers are new. */
@RunWith(AndroidJUnit4::class)
class AppLimitAlertMigrationTest {
    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            AppLimitDatabase::class.java,
        )

    @Test
    fun theUpgradeKeepsTheStoredLimitWithAlertsOff() {
        helper.createDatabase(DB_NAME, 1).use { versionOne ->
            versionOne.execSQL(
                "INSERT INTO app_limit (packageName, minutes) VALUES ('com.a', 30)",
            )
        }

        val migrated =
            helper.runMigrationsAndValidate(
                DB_NAME,
                2,
                true,
                AppLimitDatabase.MIGRATION_1_2,
            )

        migrated
            .query(
                "SELECT minutes, alertEnabled FROM app_limit WHERE packageName = 'com.a'",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(30, cursor.getInt(0))
                assertEquals(0, cursor.getInt(1))
            }
        assertEquals(0, migrated.countOf("app_limit_alert_fired"))

        migrated.execSQL(
            "INSERT INTO app_limit_alert_fired (packageName, day, tier) " +
                "VALUES ('com.a', '2026-10-20', 75)",
        )
        assertEquals(1, migrated.countOf("app_limit_alert_fired"))
        migrated.close()
    }

    private fun SupportSQLiteDatabase.countOf(table: String): Int = query("SELECT * FROM $table").use { it.count }

    private companion object {
        const val DB_NAME = "app-limit-alert-migration-test.db"
    }
}
