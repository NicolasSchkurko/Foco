package com.foco.app.data

import android.content.Context
import com.foco.app.domain.buildWeekExport
import com.foco.app.domain.dayWindowRange
import com.foco.app.model.export.WeekExport
import java.time.LocalDate
import java.time.ZoneId

/**
 * Reads the window the home shows — the stored week plus today's live day — and turns it into the
 * export. A failed read propagates: the caller shares nothing rather than a partial file.
 */
class WeekExportService(
    context: Context,
    private val labels: (String) -> String,
    private val zone: ZoneId = ZoneId.systemDefault(),
) {
    private val repository = UsageRepository.forApp(context.applicationContext)
    private val store =
        SessionHistoryStore.forApp(context.applicationContext) { day ->
            repository.loadDay(day)
        }

    suspend fun export(today: LocalDate): WeekExport {
        val stored = store.week(today)
        val totals =
            stored
                .groupBy { LocalDate.parse(it.day) }
                .mapValues { (_, rows) -> rows.associate { it.packageName to it.totalMillis } }
        val window = dayWindowRange(today)
        val sessions = store.sessionsBetween(window.start, today)
        return buildWeekExport(
            weekStart = window.start,
            weekEnd = today,
            storedTotalsByDay = totals,
            sessionsByDay = sessions,
            todaySessions = repository.loadDay(today).sessions,
            labels = labels,
            zone = zone,
        )
    }
}
