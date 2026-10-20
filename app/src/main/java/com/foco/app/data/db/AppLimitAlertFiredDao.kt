package com.foco.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/** Reads, marks and clears the alert tiers already fired in `app_limit_alert_fired`. */
@Dao
interface AppLimitAlertFiredDao {

    @Query("SELECT tier FROM app_limit_alert_fired WHERE packageName = :packageName AND day = :day")
    suspend fun tiers(packageName: String, day: String): List<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun mark(rows: List<AppLimitAlertFired>)

    @Query("DELETE FROM app_limit_alert_fired WHERE packageName = :packageName AND day = :day")
    suspend fun clearDay(packageName: String, day: String): Int

    @Query("DELETE FROM app_limit_alert_fired WHERE packageName = :packageName")
    suspend fun clearPackage(packageName: String): Int

    @Query("DELETE FROM app_limit_alert_fired")
    suspend fun clearAll(): Int
}
