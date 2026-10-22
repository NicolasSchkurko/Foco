package com.foco.app.model.export

import kotlinx.serialization.Serializable

/** One app inside one day: its total, whether raw sessions survived, and those sessions. */
@Serializable
data class ExportApp(
    val packageName: String,
    val appName: String,
    val totalMillis: Long,
    val sessionsAvailable: Boolean,
    val sessions: List<ExportSession>,
) {
    init {
        require(sessionsAvailable == sessions.isNotEmpty()) {
            "sessionsAvailable must match the sessions it carries"
        }
    }
}
