package com.foco.app.data

import android.content.Context
import androidx.room.Room
import com.foco.app.data.db.DailyAggregate
import com.foco.app.data.db.DailyAggregateDao
import com.foco.app.data.db.FocoDatabase
import com.foco.app.data.db.SessionRecord
import com.foco.app.data.db.SessionRecordDao
import com.foco.app.domain.availableDays
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
    private val readDay: (LocalDate) -> DayData,
    private val zone: ZoneId = ZoneId.systemDefault()
) {

    /** Stores every finished day inside the platform's window that has no totals yet, then prunes raw rows. */
    suspend fun consolidate(today: LocalDate) {
        val stored = aggregates.storedDays().toSet()
        val pending = availableDays(today).drop(1).filterNot { it.toString() in stored }.sorted()
        pending.forEach { consolidateDay(it) }
        sessions.pruneBefore(rawCutoff(today))
    }

    /** Per-app totals of the seven days ending at [until], oldest first. */
    suspend fun week(until: LocalDate): List<DailyAggregate> =
        aggregates.totalsFor(until.minusDays(SIX_DAYS).toString(), until.toString())

    private suspend fun consolidateDay(day: LocalDate) {
        val daySessions = readDay(day).sessions
        if (daySessions.isEmpty()) return
        // Raw first: a failure before the totals leaves the day pending, so the next start retries it.
        sessions.upsert(daySessions.map { SessionRecord(it.packageName, it.start, it.end) })
        aggregates.upsert(totalsOf(day, daySessions))
    }

    private fun totalsOf(day: LocalDate, daySessions: List<UsageSession>): List<DailyAggregate> =
        daySessions.groupBy { it.packageName }.map { (packageName, list) ->
            DailyAggregate(packageName, day.toString(), list.sumOf { it.end - it.start })
        }

    /** Start of the oldest day that still keeps raw rows: today and the previous six. */
    private fun rawCutoff(today: LocalDate): Long =
        today.minusDays(SIX_DAYS).atStartOfDay(zone).toInstant().toEpochMilli()

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
                ).build().also { database = it }
            }
            return SessionHistoryStore(db.sessionRecords(), db.dailyAggregates(), readDay)
        }
    }
}
