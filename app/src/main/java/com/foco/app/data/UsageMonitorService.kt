package com.foco.app.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.foco.app.AccessibilityAccess
import com.foco.app.MainActivity
import com.foco.app.R
import com.foco.app.domain.AppLimitRules
import com.foco.app.domain.AppLimitWindowRules
import com.foco.app.domain.BlockReason
import com.foco.app.domain.ForegroundApp
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.ZoneId

/**
 * Checks the foreground app's daily limit every two minutes. Resource: a foreground service and
 * its ongoing notification; lives while at least one limit and the accessibility service exist.
 */
class UsageMonitorService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var ticker: Job? = null

    private val store by lazy { AppLimitsStore.forApp(this) }
    private val evaluator by lazy { UsageMonitorEvaluator.forApp(this) }
    private val alerts by lazy { AppLimitAlertNotifier(this) }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification(),
            foregroundType(),
        )
        ticker = scope.launch { tick() }
    }

    /** The check on a fixed schedule: the next tick is two minutes after this one started. */
    private suspend fun tick() {
        while (currentCoroutineContext().isActive) {
            val startedAt = SystemClock.elapsedRealtime()
            checkOnce()
            val elapsed = SystemClock.elapsedRealtime() - startedAt
            delay((TICK_MILLIS - elapsed).coerceAtLeast(MIN_GAP_MILLIS))
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (!AccessibilityAccess.isGranted(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private suspend fun checkOnce() {
        if (!store.hasAnyLimitOrWindow()) {
            stopSelf()
            return
        }
        val packageName = ForegroundApp.currentPackage() ?: return
        val now = System.currentTimeMillis()
        val window = store.windowFor(packageName)
        if (AppLimitWindowRules.blocksAt(now, window, ZoneId.systemDefault())) {
            ForegroundApp.requestBlock(packageName, BlockReason.OUTSIDE_WINDOW)
            return
        }
        val limit = store.limitRowFor(packageName) ?: return
        val today = LocalDate.now()
        // A zero-minute limit is always over: its usage is never read.
        val usedSeconds =
            if (limit.minutes == AppLimitRules.BLOCK_IMMEDIATELY_MINUTES) {
                0L
            } else {
                evaluator.usedSecondsToday(packageName, now, window)
            }
        val crossed =
            evaluateLimitCheck(
                foregroundPackage = packageName,
                limitMinutes = limit.minutes,
                alertEnabled = limit.alertEnabled,
                usedSeconds = usedSeconds,
                firedTiers = store.firedTiers(packageName, today),
                onBlock = { ForegroundApp.requestBlock(packageName, BlockReason.LIMIT) },
                onNotify = { tier, minutes -> alerts.notify(packageName, tier, minutes) },
            )
        if (crossed.isNotEmpty()) store.markFired(packageName, today, crossed)
    }

    override fun onDestroy() {
        ticker?.cancel()
        scope.cancel()
        applied = false
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun notification(): Notification =
        NotificationCompat
            .Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.usage_monitor_channel))
            .setContentText(getString(R.string.usage_monitor_notification_detail))
            .setContentIntent(openAppIntent())
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    private fun openAppIntent(): PendingIntent =
        PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.usage_monitor_channel),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }

    private fun foregroundType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }

    companion object {
        private const val CHANNEL_ID = "usage_monitor"
        private const val NOTIFICATION_ID = 1
        private const val TICK_MILLIS = 2 * 60 * 1000L
        private const val MIN_GAP_MILLIS = 1_000L
        private const val TAG = "FocoMonitor"

        private val syncScope =
            CoroutineScope(
                SupervisorJob() +
                    Dispatchers.IO +
                    CoroutineExceptionHandler { _, failure -> Log.w(TAG, "the monitor sync failed", failure) },
            )

        /** Serializes the start/stop decision: two syncs cannot interleave their states. */
        private val syncGate = Mutex()

        /** The state the last sync applied; null until the first one settles. */
        @Volatile
        private var applied: Boolean? = null

        /** Starts the monitor when a limit and accessibility exist; stops it otherwise. */
        fun sync(
            context: Context,
            onSettled: () -> Unit = {},
        ) {
            val app = context.applicationContext
            syncScope.launch {
                try {
                    syncGate.withLock {
                        val needed =
                            AppLimitsStore.forApp(app).hasAnyLimitOrWindow() &&
                                AccessibilityAccess.isGranted(app)
                        if (needed == applied) return@withLock
                        if (needed) {
                            applied = start(app)
                        } else {
                            stop(app)
                            applied = false
                        }
                    }
                } finally {
                    onSettled()
                }
            }
        }

        /**
         * Starts the foreground service; false when the platform denied the start (API 31+ refuses
         * a background start), so the monitor stays stopped and the next sync retries.
         */
        private fun start(context: Context): Boolean =
            try {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, UsageMonitorService::class.java),
                )
                true
            } catch (denied: IllegalStateException) {
                Log.w(TAG, "the platform denied the monitor start; the next sync retries", denied)
                false
            }

        fun stop(context: Context) {
            applied = false
            context.stopService(Intent(context, UsageMonitorService::class.java))
        }
    }
}

/**
 * One monitor check: the block at 100 %, or the highest uncrossed alert tier while the app is open.
 * Returns the crossed tiers only when the notification was posted, so the caller marks them fired.
 */
internal fun evaluateLimitCheck(
    foregroundPackage: String?,
    limitMinutes: Int?,
    alertEnabled: Boolean,
    usedSeconds: Long,
    firedTiers: Set<Int>,
    onBlock: () -> Unit,
    onNotify: (tier: Int, limitMinutes: Int) -> Boolean,
): List<Int> {
    if (foregroundPackage == null || limitMinutes == null) return emptyList()
    if (AppLimitRules.isAtOrOverLimit(limitMinutes, usedSeconds)) {
        onBlock()
        return emptyList()
    }
    if (!alertEnabled) return emptyList()
    val crossed = AppLimitRules.crossedAlertTiers(limitMinutes, usedSeconds)
    if (crossed.isEmpty()) return emptyList()
    val highest = crossed.last()
    if (highest in firedTiers) return emptyList()
    return if (onNotify(highest, limitMinutes)) crossed else emptyList()
}
