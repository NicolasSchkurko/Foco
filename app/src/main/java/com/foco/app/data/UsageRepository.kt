package com.foco.app.data

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import com.foco.app.domain.buildSessions
import com.foco.app.domain.filterSessions
import com.foco.app.model.UsageEvent
import com.foco.app.model.UsageEventType
import com.foco.app.model.day.DayData
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Lee las estadísticas de uso del sistema (UsageStatsManager) y las mapea a
 * eventos planos; las sesiones del día salen del fold puro ([buildSessions])
 * más la política de paquetes ([filterSessions]). Las marcas `KEYGUARD_HIDDEN`
 * de la misma consulta son los desbloqueos del día.
 *
 * No agrega nada: devuelve los datos planos del día y la UI deriva sus métricas.
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

    /** Datos del día actual. */
    fun loadToday(now: Long = System.currentTimeMillis()): DayData =
        loadDay(
            date = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate(),
            now = now
        )

    /**
     * Datos de un día cualquiera: eventos del sistema → fold puro → política pura.
     * Ventana `[inicio del día, min(fin del día, now)]`: para días pasados una sesión
     * abierta se cierra al final del día; para hoy, en "ahora". Las marcas
     * `KEYGUARD_HIDDEN` del mismo stream se devuelven aparte en [DayData.unlockTimes].
     */
    fun loadDay(date: LocalDate, now: Long = System.currentTimeMillis()): DayData {
        val zone = ZoneId.systemDefault()
        val startOfDay = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val dayEndExclusive = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val effectiveEnd = minOf(dayEndExclusive, now)

        val events = usageStats?.queryEvents(startOfDay, effectiveEnd)
            ?: return DayData(sessions = emptyList(), unlockTimes = emptyList())

        val rawEvents = mutableListOf<UsageEvent>()
        val unlockTimes = mutableListOf<Long>()
        while (events.hasNextEvent()) {
            val event = UsageEvents.Event()
            events.getNextEvent(event)

            when (event.eventType) {
                UsageEvents.Event.KEYGUARD_HIDDEN ->
                    unlockTimes += event.timeStamp

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

        return DayData(
            sessions = filterSessions(
                sessions = buildSessions(rawEvents, now = effectiveEnd),
                allowedPackages = launchablePackages,
                excludedPackages = excludedPackages
            ),
            unlockTimes = unlockTimes
        )
    }
}
