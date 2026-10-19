package com.foco.app.data

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import com.foco.app.domain.buildSessions
import com.foco.app.domain.clampSessionsToDay
import com.foco.app.domain.dayWindow
import com.foco.app.domain.filterSessions
import com.foco.app.model.UsageEvent
import com.foco.app.model.UsageEventType
import com.foco.app.model.day.DayData
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Lee UsageStatsManager y mapea a eventos planos; las sesiones salen del fold puro recortado al día y de
 * la política de paquetes. No agrega nada: devuelve lo plano y la UI deriva.
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
     * Eventos → fold → recorte al día → política de paquetes. La ventana la arma [dayWindow]: mira desde
     * el inicio del día anterior y cierra en `min(fin del día, now)`; los desbloqueos son del día.
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
                    // La ventana de la consulta arranca ayer: sólo cuentan los del día pedido.
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

        return DayData(
            sessions = filterSessions(
                sessions = clampSessionsToDay(
                    sessions = buildSessions(rawEvents, now = effectiveEnd),
                    dayStart = startOfDay,
                    dayEndExclusive = dayEndExclusive
                ),
                allowedPackages = launchablePackages,
                excludedPackages = excludedPackages
            ),
            unlockTimes = unlockTimes
        )
    }
}
