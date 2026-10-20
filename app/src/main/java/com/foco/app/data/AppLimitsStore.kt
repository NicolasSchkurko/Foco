package com.foco.app.data

import android.content.Context
import androidx.room.Room
import com.foco.app.data.db.AppLimit
import com.foco.app.data.db.AppLimitDao
import com.foco.app.data.db.AppLimitDatabase
import com.foco.app.domain.AppLimitRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The daily limits. Resource: `limits.db` (Room) in private storage; created on first use and
 * living as long as the app, so a limit survives process death and reboot.
 */
class AppLimitsStore(private val limits: AppLimitDao) {

    /** Every configured limit, keyed by package; a missing key means no limit. */
    val limitsFlow: Flow<Map<String, Int>> =
        limits.limitsFlow().map { rows -> rows.associate { it.packageName to it.minutes } }

    suspend fun limitFor(packageName: String): Int? = limits.minutesFor(packageName)

    suspend fun hasAnyLimit(): Boolean = limits.count() > 0

    /** Stores [minutes] for [packageName]; only the zero/15-minute grid is accepted. */
    suspend fun setLimit(packageName: String, minutes: Int) {
        require(AppLimitRules.isSelectable(minutes)) { "not a selectable limit: $minutes" }
        limits.upsert(AppLimit(packageName, minutes))
    }

    /** Removes the limit of [packageName]: no row, no rule. */
    suspend fun clearLimit(packageName: String) {
        limits.delete(packageName)
    }

    suspend fun clearAll() {
        limits.clear()
    }

    companion object {
        private const val DATABASE_NAME = "limits.db"

        @Volatile
        private var database: AppLimitDatabase? = null

        /** The app's store over its private limits database. */
        fun forApp(context: Context): AppLimitsStore {
            val db = database ?: synchronized(this) {
                database ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppLimitDatabase::class.java,
                    DATABASE_NAME
                ).build().also { database = it }
            }
            return AppLimitsStore(db.appLimits())
        }
    }
}
