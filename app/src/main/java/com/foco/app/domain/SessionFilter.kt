package com.foco.app.domain

import com.foco.app.model.UsageSession

/** Política pura: conserva solo las sesiones de paquetes permitidos, menos los excluidos. */
fun filterSessions(
    sessions: List<UsageSession>,
    allowedPackages: Set<String>,
    excludedPackages: Set<String>
): List<UsageSession> = sessions.filter {
    it.packageName in allowedPackages && it.packageName !in excludedPackages
}
