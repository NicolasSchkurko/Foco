package com.foco.app.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Restarts the limit monitor after a reboot. Resource: the system boot broadcast; runs once at
 * boot and only starts the foreground service when a limit and accessibility are present.
 */
class UsageMonitorBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            UsageMonitorService.sync(context)
        }
    }
}
