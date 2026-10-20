package com.foco.app.model.export

import kotlinx.serialization.Serializable

/** The exported document: the rolling window it covers and its days, oldest first. */
@Serializable
data class WeekExport(
    val weekStart: String,
    val weekEnd: String,
    val days: List<ExportDay>,
)
