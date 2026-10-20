package com.foco.app.domain

/** A recurring daily use window: minutes from local midnight, end exclusive. */
data class AppWindow(
    val startMinute: Int,
    val endMinute: Int,
)
