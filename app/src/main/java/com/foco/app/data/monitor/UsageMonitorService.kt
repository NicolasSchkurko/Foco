package com.foco.app.data.monitor

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
import com.foco.app.data.alerts.AppLimitAlertNotifier
import com.foco.app.data.stores.AppLimitsStore
import com.foco.app.domain.limits.AppLimitRules
import com.foco.app.domain.monitor.ForegroundApp
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

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val notification = buildOngoingNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val type =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    0
                }
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        startTicker()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun startTicker() {
        if (ticker?.isActive == true) return
        val exceptionHandler =
            CoroutineExceptionHandler { _, failure ->
                Log.w("UsageMonitorService", "Periodic evaluation loop failed, keeping loop", failure)
            }
        ticker =
            scope.launch(exceptionHandler) {
                while (currentCoroutineContext().isActive) {
                    val start = SystemClock.elapsedRealtime()
                    evaluateCurrentPackage()
                    val duration = SystemClock.elapsedRealtime() - start
                    val wait = (EVALUATION_INTERVAL_MS - duration).coerceAtLeast(1_000L)
                    delay(wait)
                }
            }
    }

    private suspend fun evaluateCurrentPackage() {
        val current = ForegroundApp.currentPackage() ?: return
        val limit = store.limitFor(current)
        val window = store.windowFor(current)
        if (limit == null && window == null) return

        val now = System.currentTimeMillis()
        val reason = evaluator.evaluate(current, limit, window, now)
        if (reason != null) {
            ForegroundApp.requestBlock(current, reason)
        } else if (limit != null) {
            checkAndSendAlerts(current, limit, now)
        }
    }

    private suspend fun checkAndSendAlerts(
        packageName: String,
        limitMinutes: Int,
        now: Long,
    ) {
        if (!store.alertEnabledFor(packageName)) return
        val usedSeconds = evaluator.usedSecondsToday(packageName, now)
        val crossed = AppLimitRules.crossedAlertTiers(limitMinutes, usedSeconds)
        if (crossed.isEmpty()) return

        val today = LocalDate.now(ZoneId.systemDefault())
        val fired = store.firedTiers(packageName, today)
        val highest = crossed.last()
        if (highest !in fired) {
            alerts.sendAlert(packageName, highest, limitMinutes)
            store.markFired(packageName, today, listOf(highest))
        }
    }

    private fun buildOngoingNotification(): Notification {
        val channelId = CHANNEL_ID
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    channelId,
                    getString(R.string.usage_monitor_channel),
                    NotificationManager.IMPORTANCE_LOW,
                )
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        return NotificationCompat
            .Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.usage_monitor_channel))
            .setContentText(getString(R.string.usage_monitor_notification_detail))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "usage_monitor"
        private const val EVALUATION_INTERVAL_MS = 120_000L // 2 min

        private val syncMutex = Mutex()

        fun stop(context: Context) {
            context.stopService(Intent(context, UsageMonitorService::class.java))
        }

        /** Syncs the service state with the current rules and accessibility status. */
        fun sync(
            context: Context,
            onDone: (() -> Unit)? = null,
        ) {
            CoroutineScope(Dispatchers.Default).launch {
                syncMutex.withLock {
                    try {
                        val appContext = context.applicationContext
                        val store = AppLimitsStore.forApp(appContext)
                        val shouldRun =
                            store.hasAnyLimitOrWindow() &&
                                AccessibilityAccess.isEnabled(appContext)

                        val intent = Intent(appContext, UsageMonitorService::class.java)
                        if (shouldRun) {
                            ContextCompat.startForegroundService(appContext, intent)
                        } else {
                            appContext.stopService(intent)
                        }
                    } catch (failure: Exception) {
                        Log.w("UsageMonitorService", "Sync failed", failure)
                    } finally {
                        onDone?.invoke()
                    }
                }
            }
        }
    }
}
