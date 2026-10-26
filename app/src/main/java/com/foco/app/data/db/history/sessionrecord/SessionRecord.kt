package com.foco.app.data.db.history.sessionrecord

import androidx.room.Entity
import androidx.room.Index

/**
 * One stored session: [start, end) for [packageName]. The key is the app and the instant it
 * began, so consolidating the same day twice replaces instead of duplicating.
 */
@Entity(tableName = "session_record", primaryKeys = ["packageName", "start"], indices = [Index("start")])
data class SessionRecord(
    val packageName: String,
    val start: Long,
    val end: Long,
)
