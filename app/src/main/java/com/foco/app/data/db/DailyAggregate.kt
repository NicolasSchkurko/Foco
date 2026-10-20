package com.foco.app.data.db

import androidx.room.Entity

/**
 * One row per app and day ([day] is ISO-8601, so it sorts as text): the totals that survive
 * pruning and that the week view reads.
 */
@Entity(tableName = "daily_aggregate", primaryKeys = ["packageName", "day"])
data class DailyAggregate(
    val packageName: String,
    val day: String,
    val totalMillis: Long,
)
