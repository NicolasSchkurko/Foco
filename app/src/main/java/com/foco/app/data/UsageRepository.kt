package com.foco.app.data

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import com.foco.app.domain.buildSessions
import com.foco.app.domain.clampSessionsToDay
import com.foco.app.domain.filterSessions
import com.foco.app.model.UsageEvent
import com.foco.app.model.UsageEventType
import com.foco.app.model.day.DayData
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Lee UsageStatsManager y mapea los eventos a planos; las sesiones del día salen del
 * fold puro ([buildSessions]) recortado al día ([clampSessionsToDay]) y de la política
 * de paquetes ([filterSessions]). No agrega nada: devuelve lo plano y la UI deriva.
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
     * Datos de un día: eventos del sistema → fold puro → recorte al día → política de paquetes.
     * La consulta mira desde el inicio del día anterior, para ver el evento que abrió una sesión
     * que ya estaba abierta al empezar el día; cierra en `min(fin del día, now)`. Los
     * `KEYGUARD_HIDDEN` de la ventana del día son los desbloqueos.
     */
    fun loadDay(date: LocalDate, now: Long = System.currentTimeMillis()): DayData {
        val zone = ZoneId.systemDefault()
        val startOfDay = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val dayEndExclusive = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val effectiveEnd = minOf(dayEndExclusive, now)
        val lookbackStart = date.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val events = usageStats?.queryEvents(lookbackStart, effectiveEnd)
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
