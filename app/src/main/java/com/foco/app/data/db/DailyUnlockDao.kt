package com.foco.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/** One count per consolidated day. No delete: pruning the raw rows must not reach it. */
@Dao
interface DailyUnlockDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: DailyUnlock)

    @Query("SELECT `count` FROM daily_unlock WHERE day = :day")
    suspend fun countFor(day: String): Int?
}
