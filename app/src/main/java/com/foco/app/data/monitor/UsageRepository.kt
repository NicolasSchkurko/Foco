package com.foco.app.data.monitor

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import com.foco.app.domain.history.DayWindow
import com.foco.app.domain.history.TodayUsageReader
import com.foco.app.domain.history.dayWindow
import com.foco.app.domain.limits.AppLimitRules
import com.foco.app.domain.limits.AppLimitWindowRules
import com.foco.app.domain.limits.AppWindow
import com.foco.app.domain.sessions.buildSessions
import com.foco.app.domain.sessions.clampSessionsToDay
import com.foco.app.domain.sessions.filterSessions
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

    fun launchablePackageNames(): Set<String> = launchablePackages.toSet()

    /**
     * Reads all sessions of [date], up to [now] (defaults to current system time).
     * Filtering rules: only launchable packages, excludes our own app and current launcher.
     */
    fun loadDay(
        date: LocalDate,
        now: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): DayData {
        val window = dayWindow(date, zone, now)
        val rawEvents = readEvents(window.lookbackStart, window.effectiveEnd)
        val sessions = buildSessions(rawEvents, window.effectiveEnd)
        val clamped = clampSessionsToDay(sessions, window)
        val filtered = filterSessions(clamped, launchablePackages, excludedPackages)
        val unlocks = readUnlockTimes(date, window, rawEvents)
        return DayData(filtered, unlocks)
    }

    override fun todayUsageMillis(
        packageName: String,
        now: Long,
    ): Long {
        val zone = ZoneId.systemDefault()
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val dayData = loadDay(today, now, zone)
        return AppLimitRules.usedSeconds(dayData.sessions, packageName) * 1_000L
    }

    fun usageMillisInsideWindow(
        packageName: String,
        now: Long,
        window: AppWindow,
    ): Long {
        val zone = ZoneId.systemDefault()
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val dayData = loadDay(today, now, zone)
        return AppLimitWindowRules.usedSeconds(dayData.sessions, packageName, window, zone) * 1_000L
    }

    fun isForegroundNow(packageName: String): Boolean {
        val now = System.currentTimeMillis()
        val events = readEvents(now - 10_000L, now)
        val lastEvent = events.filter { it.packageName == packageName }.maxByOrNull { it.timestamp }
        return lastEvent?.type == UsageEventType.RESUMED
    }

    private fun readEvents(
        start: Long,
        end: Long,
    ): List<UsageEvent> {
        val events = usageStats?.queryEvents(start, end) ?: return emptyList()
        val result = mutableListOf<UsageEvent>()
        val platformEvent = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(platformEvent)
            val type =
                when (platformEvent.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED -> UsageEventType.RESUMED
                    UsageEvents.Event.ACTIVITY_PAUSED,
                    UsageEvents.Event.ACTIVITY_STOPPED,
                    -> UsageEventType.STOPPED

                    else -> null
                }
            if (type != null && platformEvent.packageName != null) {
                result += UsageEvent(platformEvent.packageName, platformEvent.timeStamp, type)
            }
        }
        return result
    }

    /** Unlocks: KEYGUARD_GOING_AWAY inside the day's boundaries, deduplicated within 2 s. */
    private fun readUnlockTimes(
        date: LocalDate,
        window: DayWindow,
        events: List<UsageEvent>,
    ): List<Long> {
        val queryEvents = usageStats?.queryEvents(window.startOfDay, window.effectiveEnd) ?: return emptyList()
        val rawUnlocks = mutableListOf<Long>()
        val platformEvent = UsageEvents.Event()
        while (queryEvents.hasNextEvent()) {
            queryEvents.getNextEvent(platformEvent)
            if (platformEvent.eventType == KEYGUARD_GOING_AWAY) {
                rawUnlocks += platformEvent.timeStamp
            }
        }
        val sorted = rawUnlocks.sorted()
        val deduplicated = mutableListOf<Long>()
        for (ts in sorted) {
            val last = deduplicated.lastOrNull()
            if (last == null || ts - last >= UNLOCK_DEDUPLICATION_WINDOW_MS) {
                deduplicated += ts
            }
        }
        return deduplicated
    }

    companion object {
        const val KEYGUARD_GOING_AWAY = 26
        const val UNLOCK_DEDUPLICATION_WINDOW_MS = 2_000L

        @Volatile
        private var instance: UsageRepository? = null

        fun forApp(context: Context): UsageRepository =
            instance ?: synchronized(this) {
                instance ?: UsageRepository(context.applicationContext).also { instance = it }
            }
    }
}
