package com.foco.app.model.export

import kotlinx.serialization.Serializable

/** One day of the window: its total and the apps that contributed to it. */
@Serializable
data class ExportDay(
    val date: String,
    val totalMillis: Long,
    val apps: List<ExportApp>,
)
