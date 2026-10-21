package com.foco.app.data

import android.content.Context
import com.foco.app.domain.AppLimitRules
import com.foco.app.domain.AppLimitWindowRules
import com.foco.app.domain.AppWindow

/**
 * Today's foreground seconds for one package, read from the platform once per check.
 * Resource: the UsageStats events of the current local day; nothing is cached or duplicated.
 */
class UsageMonitorEvaluator(
    context: Context,
    private val usage: UsageRepository = UsageRepository(context),
) {
    /**
     * The budget used today. With a [window], only the portions of the day's sessions inside it
     * count; without one, the whole session time counts.
     */
    fun usedSecondsToday(
        packageName: String,
        now: Long = System.currentTimeMillis(),
        window: AppWindow? = null,
    ): Long {
        val sessions = usage.loadToday(now).sessions
        return if (window == null) {
            AppLimitRules.usedSeconds(sessions, packageName)
        } else {
            AppLimitWindowRules.usedSeconds(sessions, packageName, window)
        }
    }

    /** Whether [packageName] is the foreground right now, from today's sessions — the monitor's source. */
    fun isForegroundNow(
        packageName: String,
        now: Long = System.currentTimeMillis(),
    ): Boolean = AppLimitRules.isForegroundAt(usage.loadToday(now).sessions, packageName, now)
}
