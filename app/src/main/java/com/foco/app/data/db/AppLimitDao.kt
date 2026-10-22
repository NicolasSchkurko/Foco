package com.foco.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Reads and writes the daily limits in `app_limit`; a cleared package loses its row. */
@Dao
interface AppLimitDao {
    @Query("SELECT * FROM app_limit")
    fun limitsFlow(): Flow<List<AppLimit>>

    @Query("SELECT * FROM app_limit WHERE packageName = :packageName")
    suspend fun limitRow(packageName: String): AppLimit?

    @Query("UPDATE app_limit SET alertEnabled = :enabled WHERE packageName = :packageName")
    suspend fun setAlertEnabled(
        packageName: String,
        enabled: Boolean,
    ): Int

    @Query("SELECT COUNT(*) FROM app_limit")
    suspend fun count(): Int

    /** Whether a limit or a window exists: one answer for the monitor's "is there work to do". */
    @Query("SELECT EXISTS(SELECT 1 FROM app_limit) OR EXISTS(SELECT 1 FROM app_limit_window)")
    suspend fun hasAnyLimitOrWindow(): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(limit: AppLimit)

    @Query("DELETE FROM app_limit WHERE packageName = :packageName")
    suspend fun delete(packageName: String): Int

    @Query("DELETE FROM app_limit")
    suspend fun clear(): Int
}
