package com.foco.app.data

import java.time.LocalDate

/**
 * Días que el selector de la pantalla de detalle puede ofrecer: hoy y los previos
 * dentro de la ventana que el sistema retiene (7 por defecto).
 */
fun availableDays(today: LocalDate, windowDays: Int = 7): List<LocalDate> {
    require(windowDays >= 1) { "windowDays must be at least 1" }
    return (0 until windowDays).map { today.minusDays(it.toLong()) }
}
