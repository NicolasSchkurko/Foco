package com.foco.app.domain

/**
 * The accessibility host's foreground track. Foco's own interface clears it, so the monitor never
 * blocks the app the user just left; System UI and input methods keep the app in use.
 */
class ForegroundAppState(
    private val selfPackage: String,
    private val ignoredPackages: Set<String> = emptySet(),
    private val inputMethodPackages: Set<String> = emptySet(),
) {
    @Volatile
    var current: String? = null
        private set

    /** Applies a window-state event; returns the app to evaluate, or null when nothing changed. */
    fun onWindowEvent(packageName: String?): String? {
        if (packageName == null) return null
        if (packageName == selfPackage) {
            current = null
            return null
        }
        if (packageName in ignoredPackages || packageName in inputMethodPackages) return null
        current = packageName
        return packageName
    }

    /** Whether a monitor request still targets the app in front. */
    fun accepts(packageName: String): Boolean = packageName == current

    fun clear() {
        current = null
    }
}
