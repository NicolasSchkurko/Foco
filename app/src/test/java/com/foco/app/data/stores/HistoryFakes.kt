package com.foco.app.data.stores

import com.foco.app.data.db.ConsolidatedDay
import com.foco.app.data.db.ConsolidatedDayDao
import com.foco.app.data.db.DailyAggregate
import com.foco.app.data.db.DailyAggregateDao
import com.foco.app.data.db.SessionRecord
import com.foco.app.data.db.SessionRecordDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory DAOs, so the store's logic runs on the JVM without an Android runtime. */
class FakeSessionRecordDao : SessionRecordDao {
    val rows = mutableMapOf<Pair<String, Long>, SessionRecord>()

    /** The write calls, in order: the transaction tests watch them. */
    val calls = mutableListOf<String>()
    var failNextWrite = false

    override suspend fun upsert(records: List<SessionRecord>) {
        calls += "upsert"
        if (failNextWrite) {
            failNextWrite = false
            error("the raw row write failed")
        }
        records.forEach { rows[it.packageName to it.start] = it }
    }

    override suspend fun deleteBetween(
        from: Long,
        to: Long,
    ): Int {
        calls += "deleteBetween"
        val before = rows.size
        rows.values.removeIf { it.start >= from && it.start < to }
        return before - rows.size
    }

    override suspend fun sessionsBetween(
        from: Long,
        to: Long,
    ): List<SessionRecord> = rows.values.filter { it.start >= from && it.start < to }.sortedBy { it.start }

    override suspend fun pruneBefore(cutoff: Long): Int {
        val before = rows.size
        rows.values.removeIf { it.end < cutoff }
        return before - rows.size
    }
}

/** In-memory totals: the same composite identity and range read as the Room DAO. */
class FakeDailyAggregateDao : DailyAggregateDao {
    val rows = mutableMapOf<Pair<String, String>, DailyAggregate>()

    private val writes = MutableStateFlow(0)

    override suspend fun upsert(rows: List<DailyAggregate>) {
        rows.forEach { this.rows[it.packageName to it.day] = it }
        writes.value++
    }

    override suspend fun storedDays(): List<String> = daysNow()

    override suspend fun totalsFor(
        from: String,
        to: String,
    ): List<DailyAggregate> = rows.values.filter { it.day in from..to }.sortedWith(compareBy({ it.day }, { it.packageName }))

    override suspend fun deleteBefore(cutoffDay: String): Int {
        val before = rows.size
        rows.values.removeIf { it.day < cutoffDay }
        val removed = before - rows.size
        if (removed > 0) writes.value++
        return removed
    }

    private fun daysNow(): List<String> = rows.keys.map { it.second }.distinct()
}

/** In-memory markers: one row per day, replaced on the day, with a switch to fail a write. */
class FakeConsolidatedDayDao : ConsolidatedDayDao {
    val rows = mutableMapOf<String, ConsolidatedDay>()
    var failNextWrite = false

    private val writes = MutableStateFlow(0)

    override suspend fun upsert(row: ConsolidatedDay) {
        if (failNextWrite) {
            failNextWrite = false
            error("marker write failed")
        }
        rows[row.day] = row
        writes.value++
    }

    override suspend fun storedDays(): List<String> = rows.keys.toList()

    override fun storedDaysFlow(): Flow<List<String>> = writes.map { rows.keys.toList() }

    override suspend fun countFor(day: String): Int? = rows[day]?.count

    override suspend fun deleteBefore(cutoffDay: String): Int {
        val before = rows.size
        rows.keys.removeIf { it < cutoffDay }
        val removed = before - rows.size
        if (removed > 0) writes.value++
        return removed
    }
}
