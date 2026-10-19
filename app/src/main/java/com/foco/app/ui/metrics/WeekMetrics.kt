package com.foco.app.ui.metrics

import com.foco.app.data.db.DailyAggregate
import com.foco.app.model.day.DayData
import java.time.LocalDate

/** Everything the week view shows: the day bars, oldest first, and the apps' seven-day totals. */
data class WeekMetrics(
    val bars: List<WeekDayBar>,
    val ranking: List<AppWeekTotal>
)

/**
 * A bar per day of the window ending at [todayDate] (today live, a stored day summed, a day with
 * nothing absent) and the apps ranked by their total; [excludedPackages] affects the ranking only.
 */
fun deriveWeekMetrics(
    storedTotals: List<DailyAggregate>,
    todayDate: LocalDate,
    today: DayData,
    excludedPackages: Set<String> = emptySet(),
    windowDays: Int = 7
): WeekMetrics {
    require(windowDays >= 1) { "windowDays must be at least 1" }

    val bars = (windowDays - 1 downTo 0).map { daysBack ->
        val date = todayDate.minusDays(daysBack.toLong())
        if (date == todayDate) {
            WeekDayBar(date, dayTotal(today.sessions), WeekDayBar.State.Live)
        } else {
            val rows = storedTotals.filter { it.day == date.toString() }
            if (rows.isEmpty()) {
                WeekDayBar(date, 0L, WeekDayBar.State.Absent)
            } else {
                WeekDayBar(date, rows.sumOf { it.totalMillis }, WeekDayBar.State.Stored)
            }
        }
    }

    val storedByPackage = storedTotals
        .filter { it.packageName !in excludedPackages }
        .groupBy { it.packageName }
        .mapValues { (_, rows) -> rows.sumOf { it.totalMillis } to rows.map { it.day }.distinct().size }

    val liveByPackage = today.sessions
        .filter { it.packageName !in excludedPackages }
        .groupBy { it.packageName }
        .mapValues { (_, sessions) -> sessions.sumOf { it.end - it.start } to 1 }

    val ranking = (storedByPackage.keys + liveByPackage.keys).map { packageName ->
        val stored = storedByPackage[packageName]
        val live = liveByPackage[packageName]
        AppWeekTotal(
            packageName = packageName,
            totalMillis = (stored?.first ?: 0L) + (live?.first ?: 0L),
            daysUsed = (stored?.second ?: 0) + (live?.second ?: 0)
        )
    }.sortedByDescending { it.totalMillis }

    return WeekMetrics(bars = bars, ranking = ranking)
}
