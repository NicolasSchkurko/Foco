package com.foco.app.data

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
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
 * Reads UsageStatsManager and maps it to flat events; sessions come from the pure fold, clamped to the day, and from
 * the package policy. It adds nothing: flat data out, the UI derives.
 */
class UsageRepository(private val context: Context) {

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
                0
            )
            ?.activityInfo?.packageName
            ?.let { result += it }
        result
    }

    /** The packages with a launcher icon, for the daily-limits list (services and internals out). */
    fun launchablePackageNames(): Set<String> = launchablePackages.toSet()

    /** The current day's data. */
    fun loadToday(now: Long = System.currentTimeMillis()): DayData =
        loadDay(
            date = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate(),
            now = now
        )

    /**
     * Events -> fold -> clamp to the day -> package policy. [dayWindow] builds the window: it looks from
     * the previous day's start and ends at `min(day end, now)`; the unlocks are that day's.
     */
    fun loadDay(date: LocalDate, now: Long = System.currentTimeMillis()): DayData {
        val window = dayWindow(date, ZoneId.systemDefault(), now)
        val startOfDay = window.startOfDay
        val dayEndExclusive = window.dayEndExclusive
        val effectiveEnd = window.effectiveEnd

        val events = usageStats?.queryEvents(window.lookbackStart, effectiveEnd)
            ?: return DayData(sessions = emptyList(), unlockTimes = emptyList())

        val rawEvents = mutableListOf<UsageEvent>()
        val unlockTimes = mutableListOf<Long>()
        while (events.hasNextEvent()) {
            val event = UsageEvents.Event()
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
                UsageEvents.Event.ACTIVITY_STOPPED ->
                    event.packageName?.let {
                        rawEvents += UsageEvent(it, event.timeStamp, UsageEventType.STOPPED)
                    }
            }
        }

        val sessions = filterSessions(
            sessions = clampSessionsToDay(
                sessions = buildSessions(rawEvents, now = effectiveEnd),
                dayStart = startOfDay,
                dayEndExclusive = dayEndExclusive
            ),
            allowedPackages = launchablePackages,
            excludedPackages = excludedPackages
        )
        return DayData(
            sessions = mergeContiguousSessions(sessions, ZoneId.systemDefault()),
            unlockTimes = unlockTimes
        )
    }
}
