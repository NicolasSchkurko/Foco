package com.foco.app.data.db

import androidx.room.Entity

/**
 * One alert tier already shown for a package on a local day (ISO-8601). Resource:
 * the `app_limit_alert_fired` table in the private `limits.db`; a new day starts empty.
 */
@Entity(
    tableName = "app_limit_alert_fired",
    primaryKeys = ["packageName", "day", "tier"]
)
data class AppLimitAlertFired(
    val packageName: String,
    val day: String,
    val tier: Int
)
