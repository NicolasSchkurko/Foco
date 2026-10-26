package com.foco.app.data.db.limits.applimit

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One daily limit per package: `0` (the block-immediately sentinel) blocks at open, `N` allows N
 * minutes today. No row means no limit. Resource: the `app_limit` table in `limits.db`.
 */
@Entity(tableName = "app_limit")
data class AppLimit(
    @PrimaryKey val packageName: String,
    val minutes: Int,
    @ColumnInfo(defaultValue = "0") val alertEnabled: Boolean = false,
)
