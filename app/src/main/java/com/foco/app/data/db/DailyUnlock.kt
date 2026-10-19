package com.foco.app.data.db

import androidx.room.Entity

/**
 * One row per consolidated day ([day] is ISO-8601, so it sorts as text): the unlock count of a
 * finished day, written from the same platform read that stores the day's totals.
 */
@Entity(tableName = "daily_unlock", primaryKeys = ["day"])
data class DailyUnlock(
    val day: String,
    val count: Int
)
