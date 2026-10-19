package com.foco.app.data

import android.content.Context
import androidx.room.Room
import com.foco.app.data.db.DailyAggregate
import com.foco.app.data.db.DailyAggregateDao
import com.foco.app.data.db.DailyUnlock
import com.foco.app.data.db.DailyUnlockDao
import com.foco.app.data.db.FocoDatabase
import com.foco.app.data.db.SessionRecord
import com.foco.app.data.db.SessionRecordDao
import com.foco.app.domain.finishedDays
import com.foco.app.model.UsageSession
import com.foco.app.model.day.DayData
import java.time.LocalDate
import java.time.ZoneId

/**
 * Consolidates the finished days the platform still serves into the app's database, then prunes.
 * Resource: `foco.db` in private storage; created on first use, lives as long as the app.
 */
class SessionHistoryStore(
    private val sessions: SessionRecordDao,
    private val aggregates: DailyAggregateDao,
    private val unlocks: DailyUnlockDao,
    private val readDay: (LocalDate) -> DayData
) {

    /** Stores every finished day inside the platform's window that has no totals yet, then prunes raw rows. */
    suspend fun consolidate(today: LocalDate) {
        val stored = aggregates.storedDays().toSet()
        val pending = finishedDays(today).filterNot { it.toString() in stored }.sorted()
        pending.forEach { consolidateDay(it) }
        sessions.pruneBefore(rawCutoff(today))
    }

    /** Per-app totals of the seven days ending at [until], oldest first. */
    suspend fun week(until: LocalDate): List<DailyAggregate> =
        aggregates.totalsFor(until.minusDays(SIX_DAYS).toString(), until.toString())

    /** The days the store has totals for: what bounds the detail's day selector. */
    suspend fun storedDays(): Set<LocalDate> =
        aggregates.storedDays().map { LocalDate.parse(it) }.toSet()

    /** The stored raw sessions of [date], half-open on the start; empty means the day has no rows. */
    suspend fun sessionsFor(date: LocalDate): List<UsageSession> {
        val dayStart = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val nextDayStart = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return sessions.sessionsBetween(dayStart, nextDayStart)
            .map { UsageSession(it.packageName, it.start, it.end) }
    }

    /** The stored unlock count of [date]; `null` means no count was stored, `0` is a real answer. */
    suspend fun unlocksFor(date: LocalDate): Int? = unlocks.countFor(date.toString())

    /** A day with no sessions is not consolidated, so no totals and no count are stored for it. */
    private suspend fun consolidateDay(day: LocalDate) {
        val data = readDay(day)
        if (data.sessions.isEmpty()) return
        // Raw first: a failure before the totals leaves the day pending, so the next start retries it.
        sessions.upsert(data.sessions.map { SessionRecord(it.packageName, it.start, it.end) })
        aggregates.upsert(totalsOf(day, data.sessions))
        unlocks.upsert(DailyUnlock(day.toString(), data.unlockTimes.size))
    }

    private fun totalsOf(day: LocalDate, daySessions: List<UsageSession>): List<DailyAggregate> =
        daySessions.groupBy { it.packageName }.map { (packageName, list) ->
            DailyAggregate(packageName, day.toString(), list.sumOf { it.end - it.start })
        }

    /** Start of the oldest day that still keeps raw rows, on the same default calendar the reader uses. */
    private fun rawCutoff(today: LocalDate): Long =
        today.minusDays(SIX_DAYS).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    companion object {
        private const val DATABASE_NAME = "foco.db"
        private const val SIX_DAYS = 6L

        @Volatile
        private var database: FocoDatabase? = null

        /** The app's store over its private database; [readDay] is the platform reader. */
        fun forApp(context: Context, readDay: (LocalDate) -> DayData): SessionHistoryStore {
            val db = database ?: synchronized(this) {
                database ?: Room.databaseBuilder(
                    context.applicationContext,
                    FocoDatabase::class.java,
                    DATABASE_NAME
                ).addMigrations(FocoDatabase.MIGRATION_1_2).build().also { database = it }
            }
            return SessionHistoryStore(
                db.sessionRecords(),
                db.dailyAggregates(),
                db.dailyUnlocks(),
                readDay
            )
        }
    }
}
