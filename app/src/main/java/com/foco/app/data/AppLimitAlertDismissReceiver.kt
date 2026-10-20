package com.foco.app.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

/** The Entendido action: removes the alert notification and changes nothing else. */
class AppLimitAlertDismissReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != ACTION_DISMISS) return
        val id = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        if (id >= 0) NotificationManagerCompat.from(context).cancel(id)
    }

    companion object {
        const val ACTION_DISMISS = "com.foco.app.action.DISMISS_LIMIT_ALERT"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }
}
