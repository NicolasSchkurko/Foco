package com.foco.app.ui.metrics

/** Uso del dispositivo dentro de una franja horaria (0..23). */
data class HourBucket(
    val hour: Int,
    val millis: Long
)
