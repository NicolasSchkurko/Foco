package com.foco.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Reads and writes the recurring use windows in `app_limit_window`; a cleared package loses its row. */
@Dao
interface AppLimitWindowDao {
    @Query("SELECT * FROM app_limit_window")
    fun windowsFlow(): Flow<List<AppLimitWindow>>

    @Query("SELECT * FROM app_limit_window WHERE packageName = :packageName")
    suspend fun windowFor(packageName: String): AppLimitWindow?

    @Query("SELECT COUNT(*) FROM app_limit_window")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(window: AppLimitWindow)

    @Query("DELETE FROM app_limit_window WHERE packageName = :packageName")
    suspend fun delete(packageName: String): Int

    @Query("DELETE FROM app_limit_window")
    suspend fun clear(): Int
}
