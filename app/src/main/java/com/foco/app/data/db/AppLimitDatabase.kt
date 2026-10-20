package com.foco.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The daily limits: one `app_limit` table keyed by package, version 1, exported schema.
 * Resource: `limits.db` in private storage, created on first use and kept as long as the app.
 */
@Database(entities = [AppLimit::class], version = 1, exportSchema = true)
abstract class AppLimitDatabase : RoomDatabase() {

    abstract fun appLimits(): AppLimitDao
}
