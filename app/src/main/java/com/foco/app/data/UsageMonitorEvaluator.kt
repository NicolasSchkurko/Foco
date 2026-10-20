package com.foco.app.data

import android.content.Context
import com.foco.app.domain.AppLimitRules

/**
 * Today's foreground seconds for one package, read from the platform once per check.
 * Resource: the UsageStats events of the current local day; nothing is cached or duplicated.
 */
class UsageMonitorEvaluator(
    context: Context,
    private val usage: UsageRepository = UsageRepository(context)
) {

    fun usedSecondsToday(packageName: String, now: Long = System.currentTimeMillis()): Long =
        AppLimitRules.usedSeconds(usage.loadToday(now).sessions, packageName)
}
