package com.foco.app.data

import android.content.Context
import androidx.room.Room
import com.foco.app.data.db.AppLimit
import com.foco.app.data.db.AppLimitAlertFired
import com.foco.app.data.db.AppLimitAlertFiredDao
import com.foco.app.data.db.AppLimitDao
import com.foco.app.data.db.AppLimitDatabase
import com.foco.app.domain.AppLimitRules
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The daily limits and their alert state. Resource: `limits.db` (Room) in private storage; created
 * on first use and living as long as the app, so a limit and its fired tiers survive reboot.
 */
class AppLimitsStore(
    private val limits: AppLimitDao,
    private val alertFired: AppLimitAlertFiredDao
) {

    /** Every configured limit, keyed by package; a missing key means no limit. */
    val limitsFlow: Flow<Map<String, Int>> =
        limits.limitsFlow().map { rows -> rows.associate { it.packageName to it.minutes } }

    /** Every per-app alert flag, keyed by package; a missing key means alerts are off. */
    val alertsFlow: Flow<Map<String, Boolean>> =
        limits.limitsFlow().map { rows -> rows.associate { it.packageName to it.alertEnabled } }

    suspend fun limitFor(packageName: String): Int? = limits.minutesFor(packageName)

    suspend fun alertEnabledFor(packageName: String): Boolean =
        limits.alertEnabledFor(packageName) ?: false

    suspend fun hasAnyLimit(): Boolean = limits.count() > 0

    /**
     * Stores [minutes] for [packageName]; only the zero/15-minute grid is accepted. Changing the
     * limit clears today's fired tiers, and a zero limit turns alerts off.
     */
    suspend fun setLimit(
        packageName: String,
        minutes: Int,
        day: LocalDate = LocalDate.now()
    ) {
        require(AppLimitRules.isSelectable(minutes)) { "not a selectable limit: $minutes" }
        val keepAlerts = alertEnabledFor(packageName) && minutes > 0
        limits.upsert(AppLimit(packageName, minutes, keepAlerts))
        alertFired.clearDay(packageName, day.toString())
    }

    /** Removes the limit of [packageName]: no row, no rule, no fired tiers. */
    suspend fun clearLimit(packageName: String) {
        limits.delete(packageName)
        alertFired.clearPackage(packageName)
    }

    /** Enables or disables alerts; enabling is only possible with a positive limit. */
    suspend fun setAlertEnabled(
        packageName: String,
        enabled: Boolean,
        day: LocalDate = LocalDate.now()
    ) {
        val minutes = limits.minutesFor(packageName) ?: return
        if (enabled && minutes <= 0) return
        limits.setAlertEnabled(packageName, enabled)
        alertFired.clearDay(packageName, day.toString())
    }

    suspend fun firedTiers(packageName: String, day: LocalDate): Set<Int> =
        alertFired.tiers(packageName, day.toString()).toSet()

    suspend fun markFired(packageName: String, day: LocalDate, tiers: List<Int>) {
        alertFired.mark(tiers.map { AppLimitAlertFired(packageName, day.toString(), it) })
    }

    suspend fun clearAll() {
        limits.clear()
        alertFired.clearAll()
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
                ).addMigrations(AppLimitDatabase.MIGRATION_1_2)
                    .build()
                    .also { database = it }
            }
            return AppLimitsStore(db.appLimits(), db.appLimitAlerts())
        }
    }
}
