package com.foco.app.ui.dashboard

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.foco.app.data.db.DailyAggregate
import com.foco.app.model.UsageSession
import com.foco.app.model.day.DayData
import com.foco.app.ui.metrics.day.DayMetrics
import com.foco.app.ui.metrics.day.deriveDayMetrics
import java.time.ZoneId

/** What one ring position renders once its data is ready; the home caches one entry per position. */
sealed interface RingContent {
    data class Day(
        val data: DayData,
        val unlocks: Int?,
        val metrics: DayMetrics,
    ) : RingContent

    data class Week(
        val totals: List<DailyAggregate>,
    ) : RingContent
}

/**
 * Saveable holder for the ring's content map, so a restored position (rotation, Settings round-trip)
 * renders its already-loaded content without the placeholder.
 */
val ringContentSaver: Saver<SnapshotStateMap<String, RingContent>, Any> =
    Saver(
        save = { map ->
            map.map { (key, content) ->
                when (content) {
                    is RingContent.Day ->
                        listOf(
                            "D",
                            key,
                            content.data.sessions.map { listOf(it.packageName, it.start, it.end) },
                            content.data.unlockTimes,
                            content.unlocks,
                        )
                    is RingContent.Week ->
                        listOf(
                            "W",
                            key,
                            content.totals.map { listOf(it.packageName, it.day, it.totalMillis) },
                        )
                }
            }
        },
        restore = { saved ->
            val map = mutableStateMapOf<String, RingContent>()
            (saved as? List<*>)?.forEach { entry ->
                val e = entry as? List<*>
                when (e?.getOrNull(0)) {
                    "D" -> {
                        val key = e.getOrNull(1) as? String ?: return@forEach
                        val sessions =
                            (e.getOrNull(2) as? List<*>)?.mapNotNull { s ->
                                val se = s as? List<*>
                                val pkg = se?.getOrNull(0) as? String
                                val start = (se?.getOrNull(1) as? Number)?.toLong()
                                val end = (se?.getOrNull(2) as? Number)?.toLong()
                                if (pkg != null && start != null && end != null) UsageSession(pkg, start, end) else null
                            } ?: emptyList()
                        val unlocks = (e.getOrNull(3) as? List<*>)?.mapNotNull { (it as? Number)?.toLong() } ?: emptyList()
                        val count = e.getOrNull(4) as? Int
                        // The metrics are derived again on restore: they are not saved.
                        val metrics = deriveDayMetrics(sessions, ZoneId.systemDefault())
                        map[key] = RingContent.Day(DayData(sessions, unlocks), count, metrics)
                    }
                    "W" -> {
                        val key = e.getOrNull(1) as? String ?: return@forEach
                        val totals =
                            (e.getOrNull(2) as? List<*>)?.mapNotNull { t ->
                                val te = t as? List<*>
                                val pkg = te?.getOrNull(0) as? String
                                val day = te?.getOrNull(1) as? String
                                val total = (te?.getOrNull(2) as? Number)?.toLong()
                                if (pkg != null && day != null && total != null) DailyAggregate(pkg, day, total) else null
                            } ?: emptyList()
                        map[key] = RingContent.Week(totals)
                    }
                }
            }
            map
        },
    )
