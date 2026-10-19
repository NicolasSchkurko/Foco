package com.foco.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The app's own history: raw sessions and per-day totals. Version 1 is the baseline; the exported
 * schema lives in `app/schemas/` and no destructive fallback is allowed.
 */
@Database(entities = [SessionRecord::class, DailyAggregate::class], version = 1, exportSchema = true)
abstract class FocoDatabase : RoomDatabase() {

    abstract fun sessionRecords(): SessionRecordDao

    abstract fun dailyAggregates(): DailyAggregateDao
}
