package com.foco.app.data.db.limits

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.foco.app.data.db.limits.applimit.AppLimit
import com.foco.app.data.db.limits.applimit.AppLimitDao
import com.foco.app.data.db.limits.applimitalertfired.AppLimitAlertFired
import com.foco.app.data.db.limits.applimitalertfired.AppLimitAlertFiredDao
import com.foco.app.data.db.limits.applimitwindow.AppLimitWindow
import com.foco.app.data.db.limits.applimitwindow.AppLimitWindowDao

/**
 * The daily limits, the alert tiers already fired and the recurring use windows, version 3.
 * Resource: `limits.db` in private storage, created on first use and kept as long as the app.
 */
@Database(
    entities = [AppLimit::class, AppLimitAlertFired::class, AppLimitWindow::class],
    version = 3,
    exportSchema = true,
)
abstract class AppLimitDatabase : RoomDatabase() {
    abstract fun appLimits(): AppLimitDao

    abstract fun appLimitAlerts(): AppLimitAlertFiredDao

    abstract fun appLimitWindows(): AppLimitWindowDao

    companion object {
        /** Adds the per-app alert flag and the fired-tier table; every version-1 row stays. */
        val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "ALTER TABLE `app_limit` ADD COLUMN `alertEnabled` INTEGER NOT NULL DEFAULT 0",
                    )
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `app_limit_alert_fired` " +
                            "(`packageName` TEXT NOT NULL, `day` TEXT NOT NULL, `tier` INTEGER NOT NULL, " +
                            "PRIMARY KEY(`packageName`, `day`, `tier`))",
                    )
                }
            }

        /** Adds the use-window table; it is independent of the limit, so no row means no window. */
        val MIGRATION_2_3 =
            object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `app_limit_window` " +
                            "(`packageName` TEXT NOT NULL, `startMinute` INTEGER NOT NULL, " +
                            "`endMinute` INTEGER NOT NULL, PRIMARY KEY(`packageName`))",
                    )
                }
            }
    }
}
