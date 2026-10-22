package com.foco.app.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Restarts the limit monitor after a reboot. Resource: the system boot broadcast; runs once at
 * boot and holds the process until the foreground-service start settles.
 */
class UsageMonitorBootReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent?,
    ) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        // The process must outlive the broadcast: the start runs on the monitor's own scope.
        val pending = goAsync()
        UsageMonitorService.sync(context.applicationContext) { pending.finish() }
    }
}
