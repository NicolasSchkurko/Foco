package com.foco.app.model.day

import com.foco.app.model.UsageSession

/**
 * A day as it comes out of the system query: the sessions and the
 * unlock marks (`KEYGUARD_HIDDEN`), raw. The UI derives the count from here.
 */
data class DayData(
    val sessions: List<UsageSession>,
    val unlockTimes: List<Long>,
)
