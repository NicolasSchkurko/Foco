package com.foco.app.ui.metrics

/** Device usage inside one hour slot (0..23). */
data class HourBucket(
    val hour: Int,
    val millis: Long
)
