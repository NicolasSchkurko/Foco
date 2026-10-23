package com.foco.app.ui.metrics.day

/** Device usage inside one hour slot (0..23). */
data class HourBucket(
    val hour: Int,
    val millis: Long,
)
