package com.foco.app.data

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import com.foco.app.data.db.ConsolidatedDay
import com.foco.app.data.db.ConsolidatedDayDao
import com.foco.app.data.db.DailyAggregate
import com.foco.app.data.db.DailyAggregateDao
import com.foco.app.data.db.FocoDatabase
import com.foco.app.data.db.SessionRecord
import com.foco.app.data.db.SessionRecordDao
import com.foco.app.domain.dayWindowRange
import com.foco.app.domain.finishedDays
import com.foco.app.model.UsageSession
import com.foco.app.model.day.DayData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Consolidates the finished days the platform still serves into `foco.db` (private storage, created
 * on first use, lives as long as the app), keeps today and the seven days before it, prunes the rest.
 */
class SessionHistoryStore(
    private val sessions: SessionRecordDao,
    private val aggregates: DailyAggregateDao,
    private val consolidatedDays: ConsolidatedDayDao,
    private val readDay: (LocalDate) -> DayData,
    private val transaction: suspend (body: suspend () -> Unit) -> Unit = { body -> body() },
) {
    /** Stores every finished day inside the platform's window that has no marker yet, then prunes. */
    suspend fun consolidate(today: LocalDate) {
        val stored = consolidatedDays.storedDays().toSet()
        val pending = finishedDays(today).filterNot { it.toString() in stored }.sorted()
        pending.forEach { consolidateDay(it) }
        val cutoffDay = pruneCutoffDay(today)
        sessions.pruneBefore(rawCutoff(cutoffDay))
        aggregates.deleteBefore(cutoffDay.toString())
        consolidatedDays.deleteBefore(cutoffDay.toString())
    }

    /** Per-app totals of the seven days ending at [until], oldest first. */
    suspend fun week(until: LocalDate): List<DailyAggregate> =
        aggregates.totalsFor(dayWindowRange(until).start.toString(), until.toString())

    /** The days the store has a marker for: what bounds the detail's day selector, as a stream. */
    fun storedDaysFlow(): Flow<Set<LocalDate>> = consolidatedDays.storedDaysFlow().map { days -> days.map { LocalDate.parse(it) }.toSet() }

    /** The stored raw sessions of [date], half-open on the start; empty means the day has no rows. */
    suspend fun sessionsFor(date: LocalDate): List<UsageSession> {
        val dayStart = rawStart(date)
        val nextDayStart = rawStart(date.plusDays(1))
        return sessions
            .sessionsBetween(dayStart, nextDayStart)
            .map { UsageSession(it.packageName, it.start, it.end) }
    }

    /** The stored raw sessions of every day in [from, to), keyed by day; one range read. */
    suspend fun sessionsBetween(
        from: LocalDate,
        to: LocalDate,
    ): Map<LocalDate, List<UsageSession>> {
        val zone = ZoneId.systemDefault()
        val rows = sessions.sessionsBetween(rawStart(from), rawStart(to))
        return rows
            .groupBy { row -> Instant.ofEpochMilli(row.start).atZone(zone).toLocalDate() }
            .mapValues { (_, dayRows) -> dayRows.map { UsageSession(it.packageName, it.start, it.end) } }
    }

    /** The stored unlock count of [date]; `null` means no count was stored, `0` is a real answer. */
    suspend fun unlocksFor(date: LocalDate): Int? = consolidatedDays.countFor(date.toString())

    /**
     * A day with no sessions is not consolidated, so no totals and no marker are stored for it.
     * The day's rows are replaced inside one transaction: a failure leaves the day as it was.
     */
    private suspend fun consolidateDay(day: LocalDate) {
        val data = readDay(day)
        if (data.sessions.isEmpty()) return
        // Raw first: a failure before the marker leaves the day pending, so the next start retries it.
        // The delete makes a retry replace an earlier read instead of merging old starts.
        transaction {
            sessions.deleteBetween(rawStart(day), rawStart(day.plusDays(1)))
            sessions.upsert(data.sessions.map { SessionRecord(it.packageName, it.start, it.end) })
        }
        aggregates.upsert(totalsOf(day, data.sessions))
        consolidatedDays.upsert(ConsolidatedDay(day.toString(), data.unlockTimes.size))
    }

    private fun totalsOf(
        day: LocalDate,
        daySessions: List<UsageSession>,
    ): List<DailyAggregate> =
        daySessions.groupBy { it.packageName }.map { (packageName, list) ->
            DailyAggregate(packageName, day.toString(), list.sumOf { it.end - it.start })
        }

    /** The oldest day the store keeps: today and the seven days before it, so a lagging run can rebuild. */
    private fun pruneCutoffDay(today: LocalDate): LocalDate = today.minusDays(KEEP_DAYS - 1)

    /** Start of [day], on the same default calendar the reader uses. */
    private fun rawStart(day: LocalDate): Long = day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    /** Start of [cutoffDay], on the same default calendar the reader uses. */
    private fun rawCutoff(cutoffDay: LocalDate): Long = rawStart(cutoffDay)

    companion object {
        private const val DATABASE_NAME = "foco.db"
        private const val KEEP_DAYS = 8L

        @Volatile
        private var database: FocoDatabase? = null

        /** The app's store over its private database; [readDay] is the platform reader. */
        fun forApp(
            context: Context,
            readDay: (LocalDate) -> DayData,
        ): SessionHistoryStore {
            val db =
                database ?: synchronized(this) {
                    database ?: Room
                        .databaseBuilder(
                            context.applicationContext,
                            FocoDatabase::class.java,
                            DATABASE_NAME,
                        ).addMigrations(FocoDatabase.MIGRATION_1_2, FocoDatabase.MIGRATION_2_3)
                        .build()
                        .also { database = it }
                }
            return SessionHistoryStore(
                db.sessionRecords(),
                db.dailyAggregates(),
                db.consolidatedDays(),
                readDay,
            ) { body -> db.withTransaction { body() } }
        }
    }
}
