package com.foco.app.data

import com.foco.app.data.db.DailyAggregate
import com.foco.app.data.db.DailyAggregateDao
import com.foco.app.data.db.SessionRecord
import com.foco.app.data.db.SessionRecordDao

/** In-memory DAOs, so the store's logic runs on the JVM without an Android runtime. */
class FakeSessionRecordDao : SessionRecordDao {

    val rows = mutableMapOf<Pair<String, Long>, SessionRecord>()

    override suspend fun upsert(records: List<SessionRecord>) {
        records.forEach { rows[it.packageName to it.start] = it }
    }

    override suspend fun sessionsBetween(from: Long, to: Long): List<SessionRecord> =
        rows.values.filter { it.start >= from && it.start < to }.sortedBy { it.start }

    override suspend fun pruneBefore(cutoff: Long): Int {
        val before = rows.size
        rows.values.removeIf { it.end < cutoff }
        return before - rows.size
    }
}

/** In-memory totals: the same composite identity and range read as the Room DAO. */
class FakeDailyAggregateDao : DailyAggregateDao {

    val rows = mutableMapOf<Pair<String, String>, DailyAggregate>()

    override suspend fun upsert(rows: List<DailyAggregate>) {
        rows.forEach { this.rows[it.packageName to it.day] = it }
    }

    override suspend fun storedDays(): List<String> = rows.keys.map { it.second }.distinct()

    override suspend fun totalsFor(from: String, to: String): List<DailyAggregate> =
        rows.values.filter { it.day in from..to }.sortedWith(compareBy({ it.day }, { it.packageName }))
}
