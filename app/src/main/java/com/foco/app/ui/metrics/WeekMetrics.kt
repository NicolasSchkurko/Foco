package com.foco.app.ui.metrics

import com.foco.app.data.db.DailyAggregate
import com.foco.app.model.day.DayData
import java.time.LocalDate

/** Everything the week view shows: the day bars, oldest first, and the apps' seven-day totals. */
data class WeekMetrics(
    val bars: List<WeekDayBar>,
    val ranking: List<AppWeekTotal>,
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
    windowDays: Int = 7,
): WeekMetrics {
    require(windowDays >= 1) { "windowDays must be at least 1" }

    val storedByDay = LinkedHashMap<String, Long>()
    val storedByPackage = LinkedHashMap<String, Long>()
    val storedDaysByPackage = LinkedHashMap<String, MutableSet<String>>()

    storedTotals.forEach { row ->
        storedByDay.merge(row.day, row.totalMillis, Long::plus)
        if (row.packageName !in excludedPackages) {
            storedByPackage.merge(row.packageName, row.totalMillis, Long::plus)
            storedDaysByPackage.getOrPut(row.packageName) { linkedSetOf() }.add(row.day)
        }
    }

    val bars =
        (windowDays - 1 downTo 0).map { daysBack ->
            val date = todayDate.minusDays(daysBack.toLong())
            if (date == todayDate) {
                WeekDayBar(date, dayTotal(today.sessions), WeekDayBar.State.Live)
            } else {
                val total = storedByDay[date.toString()]
                if (total == null) {
                    WeekDayBar(date, 0L, WeekDayBar.State.Absent)
                } else {
                    WeekDayBar(date, total, WeekDayBar.State.Stored)
                }
            }
        }

    val liveByPackage =
        visibleSessions(today.sessions)
            .filter { it.packageName !in excludedPackages }
            .groupBy { it.packageName }
            .mapValues { (_, sessions) -> dayTotal(sessions) to 1 }

    val ranking =
        (storedByPackage.keys + liveByPackage.keys)
            .map { packageName ->
                val live = liveByPackage[packageName]
                AppWeekTotal(
                    packageName = packageName,
                    totalMillis = (storedByPackage[packageName] ?: 0L) + (live?.first ?: 0L),
                    daysUsed = (storedDaysByPackage[packageName]?.size ?: 0) + (live?.second ?: 0),
                )
            }.filter { it.totalMillis >= MIN_VISIBLE_SESSION_MILLIS }
            .sortedByDescending { it.totalMillis }

    return WeekMetrics(bars = bars, ranking = ranking)
}
