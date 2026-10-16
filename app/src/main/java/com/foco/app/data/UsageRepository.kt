package com.foco.app.data

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import java.time.Instant
import java.time.ZoneId

/**
 * Lee las estadísticas de uso del sistema (UsageStatsManager) y las mapea a
 * eventos planos; las sesiones del día salen del fold puro ([buildSessions])
 * más la política de paquetes ([filterSessions]).
 *
 * No agrega nada: devuelve sesiones y la UI deriva sus métricas.
 */
class UsageRepository(private val context: Context) {

    private val usageStats: UsageStatsManager? =
        context.getSystemService(UsageStatsManager::class.java)

    /** Apps con ícono de lanzador (filtra servicios y paquetes internos). */
    private val launchablePackages: Set<String> by lazy {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        context.packageManager
            .queryIntentActivities(intent, 0)
            .map { it.activityInfo.packageName }
            .toSet()
    }

    /** Nuestra app y la home actual no aportan al uso "de apps". */
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

    /** Sesiones del día actual: eventos del sistema → fold puro → política pura. */
    fun loadToday(now: Long = System.currentTimeMillis()): List<UsageSession> {
        val zone = ZoneId.systemDefault()
        val startOfDay = Instant.ofEpochMilli(now)
            .atZone(zone)
            .toLocalDate()
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()

        val events = usageStats?.queryEvents(startOfDay, now) ?: return emptyList()

        val rawEvents = mutableListOf<UsageEvent>()
        while (events.hasNextEvent()) {
            val event = UsageEvents.Event()
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue

            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED ->
                    rawEvents += UsageEvent(pkg, event.timeStamp, UsageEventType.RESUMED)

                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED ->
                    rawEvents += UsageEvent(pkg, event.timeStamp, UsageEventType.STOPPED)
            }
        }

        return filterSessions(
            sessions = buildSessions(rawEvents, now),
            allowedPackages = launchablePackages,
            excludedPackages = excludedPackages
        )
    }
}
