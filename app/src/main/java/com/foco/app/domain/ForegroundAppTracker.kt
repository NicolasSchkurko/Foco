package com.foco.app.domain

/** The system-bound accessibility host: the current app window and the block request. */
interface ForegroundAppTracker {
    /** The package the host saw last, or `null` before the first window event. */
    fun currentPackage(): String?

    /** Asks the host to block [packageName] now for [reason]. */
    fun requestBlock(
        packageName: String,
        reason: BlockReason,
    )
}
