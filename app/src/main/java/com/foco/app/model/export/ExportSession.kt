package com.foco.app.model.export

import kotlinx.serialization.Serializable

/** One raw session: ISO-8601 timestamps with the local offset, and its duration. */
@Serializable
data class ExportSession(
    val start: String,
    val end: String,
    val durationMillis: Long
)
