package com.foco.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * The daily limits and the alert tiers already fired, version 2. Resource: `limits.db` in private
 * storage, created on first use and kept as long as the app.
 */
@Database(
    entities = [AppLimit::class, AppLimitAlertFired::class],
    version = 2,
    exportSchema = true
)
abstract class AppLimitDatabase : RoomDatabase() {

    abstract fun appLimits(): AppLimitDao

    abstract fun appLimitAlerts(): AppLimitAlertFiredDao

    companion object {
        /** Adds the per-app alert flag and the fired-tier table; every version-1 row stays. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `app_limit` ADD COLUMN `alertEnabled` INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `app_limit_alert_fired` " +
                        "(`packageName` TEXT NOT NULL, `day` TEXT NOT NULL, `tier` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`packageName`, `day`, `tier`))"
                )
            }
        }
    }
}
