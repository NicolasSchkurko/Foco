package com.foco.app.domain.sessions

import com.foco.app.model.UsageSession

/** Pure policy: keeps only sessions of allowed packages, minus the excluded ones. */
fun filterSessions(
    sessions: List<UsageSession>,
    allowedPackages: Set<String>,
    excludedPackages: Set<String>,
): List<UsageSession> =
    sessions.filter {
        it.packageName in allowedPackages && it.packageName !in excludedPackages
    }
