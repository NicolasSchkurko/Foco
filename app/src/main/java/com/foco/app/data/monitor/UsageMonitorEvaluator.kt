package com.foco.app.data.monitor

import android.content.Context
import com.foco.app.domain.history.TodayUsageReader
import com.foco.app.domain.limits.AppLimitRules
import com.foco.app.domain.limits.AppLimitWindowRules
import com.foco.app.domain.limits.AppWindow
import com.foco.app.domain.limits.BlockReason
import java.time.ZoneId

/**
 * The monitor's decision for one package: outside its window, over its limit, or nothing to
 * enforce. Resource: the platform usage of the current day, read only when a limit can fire.
 */
class UsageMonitorEvaluator(
    private val usage: TodayUsageReader,
    private val usageInsideWindow: (packageName: String, now: Long, window: AppWindow) -> Long,
) {
    /**
     * The reason to block [packageName], or null when there is nothing to enforce. The usage is
     * read only when a limit can fire; a package with neither a limit nor a window is never read.
     */
    fun evaluate(
        packageName: String,
        limitMinutes: Int?,
        window: AppWindow?,
        now: Long = System.currentTimeMillis(),
    ): BlockReason? {
        if (limitMinutes == null && window == null) return null
        if (window != null && AppLimitWindowRules.blocksAt(now, window, ZoneId.systemDefault())) {
            return BlockReason.OUTSIDE_WINDOW
        }
        if (limitMinutes == null) return null
        if (limitMinutes == AppLimitRules.BLOCK_IMMEDIATELY_MINUTES) return BlockReason.LIMIT
        val usedSeconds = usedSecondsToday(packageName, now, window)
        return if (AppLimitRules.isAtOrOverLimit(limitMinutes, usedSeconds)) BlockReason.LIMIT else null
    }

    /**
     * The budget used today. With a [window], only the portions of the day's sessions inside it
     * count; without one, the whole session time counts.
     */
    fun usedSecondsToday(
        packageName: String,
        now: Long = System.currentTimeMillis(),
        window: AppWindow? = null,
    ): Long {
        val millis =
            if (window == null) {
                usage.todayUsageMillis(packageName, now)
            } else {
                usageInsideWindow(packageName, now, window)
            }
        return millis / 1000
    }

    companion object {
        /** The evaluator over the app's shared usage reader. */
        fun forApp(context: Context): UsageMonitorEvaluator {
            val repository = UsageRepository.forApp(context)
            return UsageMonitorEvaluator(repository, repository::usageMillisInsideWindow)
        }
    }
}
