package com.foco.app.data.db.history.consolidatedday

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** One marker per finished day; the count may be unknown (null). Deleted with its day's rows. */
@Dao
interface ConsolidatedDayDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: ConsolidatedDay)

    @Query("SELECT day FROM consolidated_day")
    suspend fun storedDays(): List<String>

    @Query("SELECT day FROM consolidated_day")
    fun storedDaysFlow(): Flow<List<String>>

    @Query("SELECT `count` FROM consolidated_day WHERE day = :day")
    suspend fun countFor(day: String): Int?

    @Query("DELETE FROM consolidated_day WHERE day < :cutoffDay")
    suspend fun deleteBefore(cutoffDay: String): Int
}
