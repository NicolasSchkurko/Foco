package com.foco.app.model.day

import com.foco.app.model.UsageSession

/**
 * El día tal como sale de la consulta del sistema: las sesiones y las marcas de
 * desbloqueo (`KEYGUARD_HIDDEN`), en crudo. La UI deriva el conteo desde acá.
 */
data class DayData(
    val sessions: List<UsageSession>,
    val unlockTimes: List<Long>
)
