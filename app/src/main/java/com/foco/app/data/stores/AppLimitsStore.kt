package com.foco.app.data.stores

import android.content.Context
import androidx.room.Room
import com.foco.app.data.db.limits.AppLimitDatabase
import com.foco.app.data.db.limits.applimit.AppLimit
import com.foco.app.data.db.limits.applimit.AppLimitDao
import com.foco.app.data.db.limits.applimitalertfired.AppLimitAlertFired
import com.foco.app.data.db.limits.applimitalertfired.AppLimitAlertFiredDao
import com.foco.app.data.db.limits.applimitwindow.AppLimitWindow
import com.foco.app.data.db.limits.applimitwindow.AppLimitWindowDao
import com.foco.app.domain.limits.AppLimitRules
import com.foco.app.domain.limits.AppLimitWindowRules
import com.foco.app.domain.limits.AppWindow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import java.time.LocalDate

/**
 * The daily limits, alert state and use windows. Resource: `limits.db` (Room) in private storage,
 * living as long as the app, so a limit, its fired tiers and a window survive reboot.
 */
class AppLimitsStore(
    private val limits: AppLimitDao,
    private val alertFired: AppLimitAlertFiredDao,
    private val windows: AppLimitWindowDao,
) {
    /** Every limit row; read once and shared by the two projections below. */
    private val limitRows: Flow<List<AppLimit>> =
        limits.limitsFlow().shareIn(storeScope, SharingStarted.WhileSubscribed(), replay = 1)

    /** Every configured limit, keyed by package; a missing key means no limit. */
    val limitsFlow: Flow<Map<String, Int>> =
        limitRows.map { rows -> rows.associate { it.packageName to it.minutes } }

    /** Every per-app alert flag, keyed by package; a missing key means no limit row. */
    val alertsFlow: Flow<Map<String, Boolean>> =
        limitRows.map { rows -> rows.associate { it.packageName to it.alertEnabled } }

    /** Every recurring window, keyed by package; a missing key means no window. */
    val windowsFlow: Flow<Map<String, AppWindow>> =
        windows.windowsFlow().map { rows ->
            rows
                .mapNotNull { row -> AppWindow.of(row.startMinute, row.endMinute)?.let { row.packageName to it } }
                .toMap()
        }

    /** The package's limit row: its minutes and whether its alerts are on. */
    suspend fun limitRowFor(packageName: String): AppLimit? = limits.limitRow(packageName)

    suspend fun limitFor(packageName: String): Int? = limits.limitRow(packageName)?.minutes

    suspend fun alertEnabledFor(packageName: String): Boolean = limits.limitRow(packageName)?.alertEnabled ?: false

    /** The package's window; a stored row that is not a valid window is skipped, not accepted. */
    suspend fun windowFor(packageName: String): AppWindow? =
        windows
            .windowFor(packageName)
            ?.let { AppWindow.of(it.startMinute, it.endMinute) }

    suspend fun hasAnyLimit(): Boolean = limits.count() > 0

    /** Whether a limit or a window exists, so the monitor has work to do. */
    suspend fun hasAnyLimitOrWindow(): Boolean = limits.hasAnyLimitOrWindow()

    /**
     * Stores [minutes] for [packageName]; only the zero/15-minute grid is accepted. Changing the
     * limit clears today's fired tiers, and a zero limit turns alerts off.
     */
    suspend fun setLimit(
        packageName: String,
        minutes: Int,
        day: LocalDate = LocalDate.now(),
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

    /** Stores a recurring window; only a 15-minute grid pair with different ends is accepted. */
    suspend fun setWindow(
        packageName: String,
        startMinute: Int,
        endMinute: Int,
    ) {
        require(AppLimitWindowRules.isValid(startMinute, endMinute)) {
            "not a selectable window: $startMinute-$endMinute"
        }
        windows.upsert(AppLimitWindow(packageName, startMinute, endMinute))
    }

    /** Removes the window of [packageName]: no row, no time-of-day restriction. */
    suspend fun clearWindow(packageName: String) {
        windows.delete(packageName)
    }

    /** Enables or disables alerts; enabling is only possible with a positive limit. */
    suspend fun setAlertEnabled(
        packageName: String,
        enabled: Boolean,
        day: LocalDate = LocalDate.now(),
    ) {
        val minutes = limits.limitRow(packageName)?.minutes ?: return
        if (enabled && minutes <= 0) return
        limits.setAlertEnabled(packageName, enabled)
        alertFired.clearDay(packageName, day.toString())
    }

    suspend fun firedTiers(
        packageName: String,
        day: LocalDate,
    ): Set<Int> = alertFired.tiers(packageName, day.toString()).toSet()

    suspend fun markFired(
        packageName: String,
        day: LocalDate,
        tiers: List<Int>,
    ) {
        alertFired.mark(tiers.map { AppLimitAlertFired(packageName, day.toString(), it) })
    }

    suspend fun clearAll() {
        limits.clear()
        alertFired.clearAll()
        windows.clear()
    }

    companion object {
        private const val DATABASE_NAME = "limits.db"

        /** The scope the shared limit rows run in; it lives as long as the app. */
        private val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        @Volatile
        private var database: AppLimitDatabase? = null

        @Volatile
        private var store: AppLimitsStore? = null

        /** The app's store over its private limits database; one per process. */
        fun forApp(context: Context): AppLimitsStore =
            store ?: synchronized(this) {
                store ?: run {
                    val db = databaseFor(context)
                    AppLimitsStore(db.appLimits(), db.appLimitAlerts(), db.appLimitWindows())
                        .also { store = it }
                }
            }

        /** The app's database over `limits.db`; one per process. */
        private fun databaseFor(context: Context): AppLimitDatabase =
            database ?: synchronized(this) {
                database ?: Room
                    .databaseBuilder(
                        context.applicationContext,
                        AppLimitDatabase::class.java,
                        DATABASE_NAME,
                    ).addMigrations(AppLimitDatabase.MIGRATION_1_2, AppLimitDatabase.MIGRATION_2_3)
                    .build()
                    .also { database = it }
            }
    }
}
