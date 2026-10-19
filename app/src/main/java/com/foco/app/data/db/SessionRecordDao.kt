package com.foco.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/** Raw sessions of the days already consolidated; pruned once they leave the seven-day window. */
@Dao
interface SessionRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(records: List<SessionRecord>)

    @Query("SELECT * FROM session_record WHERE `start` >= :from AND `start` < :to ORDER BY `start`")
    suspend fun sessionsBetween(from: Long, to: Long): List<SessionRecord>

    @Query("DELETE FROM session_record WHERE `end` < :cutoff")
    suspend fun pruneBefore(cutoff: Long): Int
}
