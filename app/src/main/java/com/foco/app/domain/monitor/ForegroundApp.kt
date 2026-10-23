package com.foco.app.domain.monitor

import com.foco.app.domain.limits.BlockReason

/**
 * The monitor's handle on the foreground app: the accessibility host registers itself here, so
 * the data layer never reaches up into the UI (or the entry layer) to ask.
 */
object ForegroundApp {
    @Volatile
    private var tracker: ForegroundAppTracker? = null

    fun register(tracker: ForegroundAppTracker) {
        this.tracker = tracker
    }

    fun unregister(tracker: ForegroundAppTracker) {
        if (this.tracker === tracker) this.tracker = null
    }

    fun currentPackage(): String? = tracker?.currentPackage()

    fun requestBlock(
        packageName: String,
        reason: BlockReason,
    ) {
        tracker?.requestBlock(packageName, reason)
    }
}
