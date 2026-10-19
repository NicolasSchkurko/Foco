package com.foco.app.ui.metrics

import com.foco.app.model.UsageSession
import java.time.ZoneId

/**
 * One day's detail for ONE app: sorted sessions and summary (total, count, first/last,
 * average, longest) with its hourly distribution; null if it has no sessions that day.
 */
fun appDayDetail(sessions: List<UsageSession>, packageName: String, zone: ZoneId): AppDayDetail? {
    val mine = sessions.filter { it.packageName == packageName }.sortedBy { it.start }
    if (mine.isEmpty()) return null

    val total = mine.sumOf { it.end - it.start }
    return AppDayDetail(
        packageName = packageName,
        totalMillis = total,
        sessionCount = mine.size,
        firstStart = mine.first().start,
        lastEnd = mine.last().end,
        averageMillis = total / mine.size,
        longestMillis = mine.maxOf { it.end - it.start },
        hourly = hourlyDistribution(mine, zone),
        sessions = mine
    )
}
