package com.foco.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * The app's own history: raw sessions, per-day totals and each finished day's unlock count.
 * Version 2 adds `daily_unlock`; `app/schemas/` holds the migration baseline, no destructive fallback.
 */
@Database(
    entities = [SessionRecord::class, DailyAggregate::class, DailyUnlock::class],
    version = 2,
    exportSchema = true
)
abstract class FocoDatabase : RoomDatabase() {

    abstract fun sessionRecords(): SessionRecordDao

    abstract fun dailyAggregates(): DailyAggregateDao

    abstract fun dailyUnlocks(): DailyUnlockDao

    companion object {
        /** Adds `daily_unlock` to a version-1 database; every row version 1 stored stays. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `daily_unlock` (`day` TEXT NOT NULL, " +
                        "`count` INTEGER NOT NULL, PRIMARY KEY(`day`))"
                )
            }
        }
    }
}
