package com.foco.app.data.db.history.consolidatedday

import androidx.room.Entity

/**
 * One row per finished day: the day key and its unlock count (null when it was never stored).
 * The row is the completion marker and is written last.
 */
@Entity(tableName = "consolidated_day", primaryKeys = ["day"])
data class ConsolidatedDay(
    val day: String,
    val count: Int?,
)
