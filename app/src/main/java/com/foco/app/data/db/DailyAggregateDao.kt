package com.foco.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/** Per-app, per-day totals: what survives pruning and what the week view reads. */
@Dao
interface DailyAggregateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rows: List<DailyAggregate>)

    @Query("SELECT DISTINCT day FROM daily_aggregate")
    suspend fun storedDays(): List<String>

    @Query("SELECT * FROM daily_aggregate WHERE day >= :from AND day <= :to ORDER BY day, packageName")
    suspend fun totalsFor(from: String, to: String): List<DailyAggregate>
}
