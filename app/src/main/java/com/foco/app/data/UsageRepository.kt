package com.foco.app.data

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Lee las estadísticas de uso del sistema (UsageStatsManager) y las transforma
 * en sesiones por app + buckets por hora para el día actual.
 *
 * Funciona sobre eventos: ACTIVITY_RESUMED abre una sesión, ACTIVITY_PAUSED la cierra.
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

    fun loadToday(now: Long = System.currentTimeMillis()): DayUsage {
        val zone = ZoneId.systemDefault()
        val startOfDay = Instant.ofEpochMilli(now)
            .atZone(zone)
            .toLocalDate()
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()

        val events = usageStats?.queryEvents(startOfDay, now) ?: return emptyDay()

        val sessions = mutableListOf<RawSession>()
        var currentPackage: String? = null
        var currentStart = 0L

        while (events.hasNextEvent()) {
            val event = UsageEvents.Event()
            events.getNextEvent(event)
            val pkg = event.packageName

            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    if (pkg == null) continue
                    val open = currentPackage
                    when {
                        open == null -> {
                            currentPackage = pkg
                            currentStart = event.timeStamp
                        }
                        open != pkg && event.timeStamp > currentStart -> {
                            // Cambio directo entre apps: cerramos la anterior al inicio de la nueva.
                            sessions += RawSession(open, currentStart, event.timeStamp)
                            currentPackage = pkg
                            currentStart = event.timeStamp
                        }
                        // Misma app (transición interna de activities): no cortamos la sesión.
                    }
                }

                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED -> {
                    val open = currentPackage
                    if (open != null && pkg == open && event.timeStamp > currentStart) {
                        sessions += RawSession(open, currentStart, event.timeStamp)
                        currentPackage = null
                    }
                }
            }
        }

        // App en foreground en este momento: sesión abierta hasta "now".
        currentPackage?.let { open ->
            if (now > currentStart) sessions += RawSession(open, currentStart, now)
        }

        val filtered = sessions.filter {
            it.packageName in launchablePackages && it.packageName !in excludedPackages
        }
        if (filtered.isEmpty()) return emptyDay()

        val byPackage = linkedMapOf<String, MutableList<RawSession>>()
        filtered.forEach { session ->
            byPackage.getOrPut(session.packageName) { mutableListOf() } += session
        }

        val pm = context.packageManager
        val entries = byPackage
            .map { (pkg, list) ->
                AppUsageEntry(
                    packageName = pkg,
                    label = labelFor(pm, pkg),
                    totalMillis = list.sumOf { it.end - it.start },
                    lastUsedAt = list.maxOf { it.end },
                    sessions = list.size
                )
            }
            .sortedByDescending { it.totalMillis }

        val hourly = LongArray(24)
        filtered.forEach { session -> addToHourBuckets(hourly, session.start, session.end, zone) }

        return DayUsage(
            totalMillis = entries.sumOf { it.totalMillis },
            entries = entries,
            hourly = hourly.mapIndexed { hour, millis -> HourBucket(hour, millis) }
        )
    }

    private fun emptyDay(): DayUsage = DayUsage(
        totalMillis = 0L,
        entries = emptyList(),
        hourly = (0..23).map { HourBucket(it, 0L) }
    )

    private fun labelFor(pm: PackageManager, packageName: String): String =
        runCatching { pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString() }
            .getOrDefault(packageName)

    /** Reparte una sesión entre las horas que toca (si cruza el límite de hora). */
    private fun addToHourBuckets(buckets: LongArray, start: Long, end: Long, zone: ZoneId) {
        var cursor = start
        while (cursor < end) {
            val zoned = Instant.ofEpochMilli(cursor).atZone(zone)
            val nextHour = zoned.truncatedTo(ChronoUnit.HOURS).plusHours(1).toInstant().toEpochMilli()
            val chunkEnd = minOf(end, nextHour)
            buckets[zoned.hour] += chunkEnd - cursor
            cursor = chunkEnd
        }
    }

    private data class RawSession(val packageName: String, val start: Long, val end: Long)
}
