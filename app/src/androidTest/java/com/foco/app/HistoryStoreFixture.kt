package com.foco.app

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.foco.app.data.db.FocoDatabase
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.ZoneId

/**
 * The app's history database, opened by a test: the store keeps it private, and a test that needs
 * a day with no data has to clear it. The name is the store's own private constant.
 */
private const val HISTORY_DATABASE = "foco.db"

/**
 * Removes [day] from the app's history: its raw rows, its per-day totals and its marker. The
 * week view reads the totals and the detail reads the rows, so the day reads as "no data".
 */
fun clearTheHistoryDay(day: LocalDate) {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val database =
        Room
            .databaseBuilder(context, FocoDatabase::class.java, HISTORY_DATABASE)
            .build()
    try {
        val zone = ZoneId.systemDefault()
        val start =
            day
                .atStartOfDay(zone)
                .toInstant()
                .toEpochMilli()
        val end =
            day
                .plusDays(1)
                .atStartOfDay(zone)
                .toInstant()
                .toEpochMilli()
        runBlocking {
            database.sessionRecords().deleteBetween(start, end)
            val cutoff = day.plusDays(1).toString()
            database.dailyAggregates().deleteBefore(cutoff)
            database.consolidatedDays().deleteBefore(cutoff)
        }
    } finally {
        database.close()
    }
}
