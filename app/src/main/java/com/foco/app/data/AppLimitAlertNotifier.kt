package com.foco.app.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.foco.app.R

/**
 * Posts the threshold warning as a system notification with one Entendido action. Resource: the
 * notification channel and the system notification shade; nothing is stored here.
 */
class AppLimitAlertNotifier(
    private val context: Context,
) {
    private val notifications = NotificationManagerCompat.from(context)

    /** False when Foco notifications or the alerts channel are off: the alert is skipped, never faked. */
    fun canNotify(): Boolean {
        if (!notifications.areNotificationsEnabled()) return false
        val channel =
            context
                .getSystemService(NotificationManager::class.java)
                ?.getNotificationChannel(CHANNEL_ID)
        return channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    /**
     * Shows the alert for [packageName] at [tier] % of [limitMinutes]. Returns true only when the
     * notification was posted, so the caller marks the tier fired only on a real alert.
     */
    fun notify(
        packageName: String,
        tier: Int,
        limitMinutes: Int,
    ): Boolean {
        if (!canNotify()) return false
        createChannel()

        val label = AppPackageInfo(context).label(packageName)
        val id = notificationId(packageName)
        val dismiss =
            PendingIntent.getBroadcast(
                context,
                id,
                Intent(context, AppLimitAlertDismissReceiver::class.java).apply {
                    action = AppLimitAlertDismissReceiver.ACTION_DISMISS
                    putExtra(AppLimitAlertDismissReceiver.EXTRA_NOTIFICATION_ID, id)
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        val notification =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(context.getString(R.string.limit_alert_title, label, tier))
                .setContentText(
                    context.getString(
                        R.string.limit_alert_message,
                        label,
                        formatLimit(limitMinutes),
                    ),
                ).setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .addAction(0, context.getString(R.string.limit_alert_ack), dismiss)
                .build()

        notifications.notify(id, notification)
        return true
    }

    private fun createChannel() {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.limit_alert_channel),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
    }

    private fun formatLimit(minutes: Int): String {
        val hours = minutes / 60
        val rest = minutes % 60
        return when {
            hours > 0 && rest > 0 -> "$hours h $rest min"
            hours > 0 -> "$hours h"
            else -> "$minutes min"
        }
    }

    companion object {
        internal const val CHANNEL_ID = "limit_alerts"

        /** Stable per package and outside the monitor's id 1. */
    }
}

/** Stable per-package notification id: the full package hash, so alerts never collide. */
fun notificationId(packageName: String): Int = packageName.hashCode() and 0x7FFFFFFF
