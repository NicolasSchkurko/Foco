package com.foco.app.domain

/**
 * The accessibility host's foreground track. Foco's own interface clears it; System UI and input
 * methods keep the app in use. A block briefly ignores late events from the app it sent home.
 */
class ForegroundAppState(
    private val selfPackage: String,
    private val ignoredPackages: Set<String> = emptySet(),
    private val inputMethodPackages: Set<String> = emptySet(),
    private val now: () -> Long = System::currentTimeMillis,
) {
    @Volatile
    var current: String? = null
        private set

    private var blockedPackage: String? = null
    private var blockedAt = 0L

    /** Records the app a block just sent home; its late window events are ignored briefly. */
    fun blockFinished(packageName: String) {
        blockedPackage = packageName
        blockedAt = now()
        if (current == packageName) current = null
    }

    /**
     * Applies a window-state event; returns the app to evaluate, or null when nothing changed.
     * [isViewWindow] marks a view-group window leaving the foreground, which a block ignores briefly.
     */
    fun onWindowEvent(
        packageName: String?,
        isViewWindow: Boolean = false,
    ): String? {
        if (packageName == null) return null
        if (packageName == selfPackage) {
            current = null
            return null
        }
        if (packageName in ignoredPackages || packageName in inputMethodPackages) return null
        val at = now()
        if (packageName == blockedPackage && isViewWindow && at - blockedAt < BLOCK_COOLDOWN_MS) {
            return null
        }
        if (blockedPackage != null && at - blockedAt >= BLOCK_COOLDOWN_MS) blockedPackage = null
        current = packageName
        return packageName
    }

    /** Whether a monitor request still targets the app in front. */
    fun accepts(packageName: String): Boolean = packageName == current

    fun clear() {
        current = null
    }

    private companion object {
        const val BLOCK_COOLDOWN_MS = 1_000L
    }
}
