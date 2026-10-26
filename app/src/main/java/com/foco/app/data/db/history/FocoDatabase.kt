package com.foco.app.data.db.history

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.foco.app.data.db.history.consolidatedday.ConsolidatedDay
import com.foco.app.data.db.history.consolidatedday.ConsolidatedDayDao
import com.foco.app.data.db.history.dailyaggregate.DailyAggregate
import com.foco.app.data.db.history.dailyaggregate.DailyAggregateDao
import com.foco.app.data.db.history.sessionrecord.SessionRecord
import com.foco.app.data.db.history.sessionrecord.SessionRecordDao

/**
 * The app's own history: raw sessions, per-day totals and one marker per finished day carrying its
 * unlock count. Version 3 replaces `daily_unlock` with `consolidated_day`; no destructive fallback.
 */
@Database(
    entities = [SessionRecord::class, DailyAggregate::class, ConsolidatedDay::class],
    version = 3,
    exportSchema = true,
)
abstract class FocoDatabase : RoomDatabase() {
    abstract fun sessionRecords(): SessionRecordDao

    abstract fun dailyAggregates(): DailyAggregateDao

    abstract fun consolidatedDays(): ConsolidatedDayDao

    companion object {
        /** Adds `daily_unlock` to a version-1 database; every row version 1 stored stays. */
        val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `daily_unlock` (`day` TEXT NOT NULL, " +
                            "`count` INTEGER NOT NULL, PRIMARY KEY(`day`))",
                    )
                }
            }

        /**
         * Creates the marker, carries every stored count and every totals-only day, then drops
         * `daily_unlock`. Days with no count stay null, which is not the same as zero.
         */
        val MIGRATION_2_3 =
            object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `consolidated_day` (`day` TEXT NOT NULL, " +
                            "`count` INTEGER, PRIMARY KEY(`day`))",
                    )
                    db.execSQL(
                        "INSERT OR IGNORE INTO `consolidated_day` (`day`, `count`) " +
                            "SELECT `day`, `count` FROM `daily_unlock`",
                    )
                    db.execSQL(
                        "INSERT OR IGNORE INTO `consolidated_day` (`day`, `count`) " +
                            "SELECT DISTINCT `day`, NULL FROM `daily_aggregate`",
                    )
                    db.execSQL("DROP TABLE IF EXISTS `daily_unlock`")
                }
            }
    }
}
