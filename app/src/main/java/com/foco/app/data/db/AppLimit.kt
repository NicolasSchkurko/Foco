package com.foco.app.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One daily limit per package: `0` blocks at open, `N` allows N minutes today. No row means no
 * limit. Resource: the `app_limit` table in the private `limits.db`.
 */
@Entity(tableName = "app_limit")
data class AppLimit(
    @PrimaryKey val packageName: String,
    val minutes: Int,
    @ColumnInfo(defaultValue = "0") val alertEnabled: Boolean = false
)
