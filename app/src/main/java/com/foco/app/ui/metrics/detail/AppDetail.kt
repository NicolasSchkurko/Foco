package com.foco.app.ui.metrics.detail

import com.foco.app.domain.sessions.mergeContiguousSessions
import com.foco.app.model.UsageSession
import com.foco.app.ui.metrics.day.hourlyDistribution
import com.foco.app.ui.metrics.day.visibleSessions
import java.time.ZoneId

/**
 * One day's detail for ONE app: its visible sessions, the summary derived from them (total, count,
 * first/last, average, longest) and their hourly distribution; null if no visible session remains.
 */
fun appDayDetail(
    sessions: List<UsageSession>,
    packageName: String,
    zone: ZoneId,
): AppDayDetail? {
    val mine =
        visibleSessions(
            mergeContiguousSessions(sessions.filter { it.packageName == packageName }, zone),
        ).sortedBy { it.start }
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
        sessions = mine,
    )
}
