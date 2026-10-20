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
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.foco.app.AccessibilityAccess
import com.foco.app.MainActivity
import com.foco.app.R
import com.foco.app.domain.AppLimitRules
import com.foco.app.domain.ForegroundApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Checks the foreground app's daily limit every two minutes. Resource: a foreground service and
 * its ongoing notification; lives while at least one limit and the accessibility service exist.
 */
class UsageMonitorService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var ticker: Job? = null

    private val store by lazy { AppLimitsStore.forApp(this) }
    private val evaluator by lazy { UsageMonitorEvaluator(this) }
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
        ticker =
            scope.launch {
                while (isActive) {
                    checkOnce()
                    delay(TICK_MILLIS)
                }
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
        if (!store.hasAnyLimit()) {
            stopSelf()
            return
        }
        val packageName = ForegroundApp.currentPackage() ?: return
        val limit = store.limitFor(packageName) ?: return
        val today = LocalDate.now()
        val crossed =
            evaluateLimitCheck(
                foregroundPackage = packageName,
                limitMinutes = limit,
                alertEnabled = store.alertEnabledFor(packageName),
                usedSeconds = evaluator.usedSecondsToday(packageName),
                firedTiers = store.firedTiers(packageName, today),
                onBlock = { ForegroundApp.requestBlock(packageName) },
                onNotify = { tier, minutes -> alerts.notify(packageName, tier, minutes) },
            )
        if (crossed.isNotEmpty()) store.markFired(packageName, today, crossed)
    }

    override fun onDestroy() {
        ticker?.cancel()
        scope.cancel()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun notification(): Notification =
        NotificationCompat
            .Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.usage_monitor_notification))
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

        private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /** Starts the monitor when a limit and accessibility exist; stops it otherwise. */
        fun sync(context: Context) {
            val app = context.applicationContext
            syncScope.launch {
                val needed =
                    AppLimitsStore.forApp(app).hasAnyLimit() &&
                        AccessibilityAccess.isGranted(app)
                if (needed) start(app) else stop(app)
            }
        }

        private fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, UsageMonitorService::class.java),
            )
        }

        fun stop(context: Context) {
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
