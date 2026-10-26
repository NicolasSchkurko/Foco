package com.foco.app.data.db.limits.applimitwindow

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One recurring daily use window per package: minutes from local midnight, end exclusive.
 * No row means no window. Resource: the `app_limit_window` table in the private `limits.db`.
 */
@Entity(tableName = "app_limit_window")
data class AppLimitWindow(
    @PrimaryKey val packageName: String,
    val startMinute: Int,
    val endMinute: Int,
)
