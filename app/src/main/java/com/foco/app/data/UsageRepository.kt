package com.foco.app.data

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import com.foco.app.domain.AppLimitRules
import com.foco.app.domain.AppLimitWindowRules
import com.foco.app.domain.AppWindow
import com.foco.app.domain.TodayUsageReader
import com.foco.app.domain.buildSessions
import com.foco.app.domain.clampSessionsToDay
import com.foco.app.domain.dayWindow
import com.foco.app.domain.filterSessions
import com.foco.app.domain.mergeContiguousSessions
import com.foco.app.model.UsageEvent
import com.foco.app.model.UsageEventType
import com.foco.app.model.day.DayData
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Reads UsageStatsManager and maps it to flat events; sessions come from the pure fold, clamped to
 * the day, and from the package policy. Resource: the device's UsageStats events, read on demand.
 */
class UsageRepository(
    private val context: Context,
) : TodayUsageReader {
    private val usageStats: UsageStatsManager? =
        context.getSystemService(UsageStatsManager::class.java)

    /** Apps with a launcher icon (filters services and internal packages). */
    private val launchablePackages: Set<String> by lazy {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        context.packageManager
            .queryIntentActivities(intent, 0)
            .map { it.activityInfo.packageName }
            .toSet()
    }

    /** Our own app and the current home do not count as app usage. */
    private val excludedPackages: Set<String> by lazy {
        val result = mutableSetOf(context.packageName)
        context.packageManager
            .resolveActivity(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
                0,
            )?.activityInfo
            ?.packageName
            ?.let { result += it }
        result
    }

    /** The packages with a launcher icon, for the daily-limits list (services and internals out). */
    fun launchablePackageNames(): Set<String> = launchablePackages.toSet()

    /** The current day's data. */
    fun loadToday(now: Long = System.currentTimeMillis()): DayData {
        val zone = ZoneId.systemDefault()
        return readDay(Instant.ofEpochMilli(now).atZone(zone).toLocalDate(), now, zone)
    }

    /**
     * Today's foreground time for [packageName] in milliseconds, truncated to the second: the usage
     * a daily limit counts.
     */
    override fun todayUsageMillis(
        packageName: String,
        now: Long,
    ): Long = AppLimitRules.usedSeconds(loadToday(now).sessions, packageName) * 1000L

    /**
     * The part of today's usage that falls inside [window], in milliseconds and truncated to the
     * second: the usage a windowed limit counts.
     */
    fun usageMillisInsideWindow(
        packageName: String,
        now: Long,
        window: AppWindow,
    ): Long {
        val zone = ZoneId.systemDefault()
        val sessions = loadToday(now).sessions
        return AppLimitWindowRules.usedSeconds(sessions, packageName, window, zone) * 1000L
    }

    /** Whether [packageName] is the foreground right now, from today's sessions — the monitor's source. */
    fun isForegroundNow(
        packageName: String,
        now: Long = System.currentTimeMillis(),
    ): Boolean = AppLimitRules.isForegroundAt(loadToday(now).sessions, packageName, now)

    /**
     * Events -> fold -> clamp to the day -> package policy. [dayWindow] builds the window: it looks from
     * the previous day's start and ends at `min(day end, now)`; the unlocks are that day's.
     */
    fun loadDay(
        date: LocalDate,
        now: Long = System.currentTimeMillis(),
    ): DayData = readDay(date, now, ZoneId.systemDefault())

    /** The day's read in one calendar: the query window and the merge never disagree. */
    private fun readDay(
        date: LocalDate,
        now: Long,
        zone: ZoneId,
    ): DayData {
        val window = dayWindow(date, zone, now)
        val startOfDay = window.startOfDay
        val effectiveEnd = window.effectiveEnd

        val events =
            usageStats?.queryEvents(window.lookbackStart, effectiveEnd)
                ?: return DayData(sessions = emptyList(), unlockTimes = emptyList())

        val rawEvents = mutableListOf<UsageEvent>()
        val unlockTimes = mutableListOf<Long>()
        // The platform refills one event holder: a new one per event would churn the hottest loop.
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)

            when (event.eventType) {
                UsageEvents.Event.KEYGUARD_HIDDEN ->
                    // The query window starts yesterday: only the requested day's unlocks count.
                    if (event.timeStamp >= startOfDay) unlockTimes += event.timeStamp

                UsageEvents.Event.ACTIVITY_RESUMED ->
                    event.packageName?.let {
                        rawEvents += UsageEvent(it, event.timeStamp, UsageEventType.RESUMED)
                    }

                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED,
                ->
                    event.packageName?.let {
                        rawEvents += UsageEvent(it, event.timeStamp, UsageEventType.STOPPED)
                    }
            }
        }

        val sessions =
            filterSessions(
                sessions =
                    clampSessionsToDay(
                        sessions = buildSessions(rawEvents, now = effectiveEnd),
                        window = window,
                    ),
                allowedPackages = launchablePackages,
                excludedPackages = excludedPackages,
            )
        return DayData(
            sessions = mergeContiguousSessions(sessions, zone),
            unlockTimes = unlockTimes,
        )
    }

    companion object {
        @Volatile
        private var shared: UsageRepository? = null

        /**
         * The app's reader over the platform's usage events; one per process. Shortcut: the
         * launchable set is a snapshot, so a package installed later counts from the next start.
         */
        fun forApp(context: Context): UsageRepository =
            shared ?: synchronized(this) {
                shared ?: UsageRepository(context.applicationContext).also { shared = it }
            }
    }
}
