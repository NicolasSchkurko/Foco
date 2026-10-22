package com.foco.app

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import com.foco.app.data.AppLimitsStore
import com.foco.app.data.AppPackageInfo
import com.foco.app.data.UsageMonitorEvaluator
import com.foco.app.data.UsageMonitorService
import com.foco.app.data.UsageRepository
import com.foco.app.domain.AppLimitWindowRules
import com.foco.app.domain.AppWindow
import com.foco.app.domain.BlockReason
import com.foco.app.domain.ForegroundApp
import com.foco.app.domain.ForegroundAppState
import com.foco.app.domain.ForegroundAppTracker
import com.foco.app.ui.limits.AppLimitBlockScreen
import com.foco.app.ui.limits.BlockOverlayHost
import com.foco.app.ui.theme.FocoTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZoneId

/**
 * The app's accessibility entry point: reads the foreground package, hosts the three-second block
 * overlay and answers the monitor. Resource: the accessibility binding; lives while enabled.
 */
class UsageMonitorAccessibilityService :
    AccessibilityService(),
    ForegroundAppTracker {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val blockHost by lazy { BlockOverlayHost(this) }
    private var boundaryJob: Job? = null
    private var recheckJob: Job? = null
    private var blockedPackage: String? = null

    private val store by lazy { AppLimitsStore.forApp(this) }
    private val evaluator by lazy { UsageMonitorEvaluator.forApp(this) }
    private val usage by lazy { UsageRepository.forApp(this) }

    private val inputMethodPackages: Set<String> by lazy {
        getSystemService(InputMethodManager::class.java)
            ?.enabledInputMethodList
            ?.map { it.packageName }
            ?.toSet()
            .orEmpty()
    }

    private val foreground by lazy {
        ForegroundAppState(packageName, IGNORED_PACKAGES, inputMethodPackages)
    }

    override fun onServiceConnected() {
        ForegroundApp.register(this)
        UsageMonitorService.sync(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val isViewWindow = event.className?.toString() == VIEW_GROUP
        val next = foreground.onWindowEvent(event.packageName?.toString(), isViewWindow)
        if (next != null) evaluate(next)
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        removeOverlay()
        boundaryJob?.cancel()
        recheckJob?.cancel()
        foreground.clear()
        ForegroundApp.unregister(this)
        UsageMonitorService.stop(this)
        scope.cancel()
        super.onDestroy()
    }

    override fun currentPackage(): String? = foreground.current

    override fun requestBlock(
        packageName: String,
        reason: BlockReason,
    ) {
        if (!foreground.accepts(packageName)) return
        evaluate(packageName, reason)
    }

    private fun evaluate(
        packageName: String,
        forcedReason: BlockReason? = null,
    ) {
        scope.launch {
            if (blockHost.isShowing) return@launch
            try {
                val reason = withContext(Dispatchers.IO) { decide(packageName, forcedReason) }
                if (reason == null) return@launch
                boundaryJob?.cancel()
                showBlock(packageName, reason)
            } catch (t: Throwable) {
                Log.e(TAG, "evaluate failed for $packageName", t)
            }
        }
    }

    /**
     * The block reason for a package, read off the main thread; null when there is nothing to
     * enforce. The usage is read only when a limit can fire.
     */
    private suspend fun decide(
        packageName: String,
        forcedReason: BlockReason?,
    ): BlockReason? {
        val now = System.currentTimeMillis()
        val window = store.windowFor(packageName)
        val limit = store.limitFor(packageName)
        val reason = forcedReason ?: evaluator.evaluate(packageName, limit, window, now)
        if (reason == null) armBoundary(packageName, window, now)
        return reason
    }

    /** Arms the one-shot check for the next window boundary; the 2-minute tick is the fallback. */
    private fun armBoundary(
        packageName: String,
        window: AppWindow?,
        now: Long,
    ) {
        boundaryJob?.cancel()
        if (window == null) return
        val waitMillis = AppLimitWindowRules.nextBoundaryMillis(now, window, ZoneId.systemDefault()) - now
        if (waitMillis <= 0) return
        boundaryJob =
            scope.launch {
                delay(waitMillis)
                if (foreground.current == packageName) evaluate(packageName)
            }
    }

    private fun showBlock(
        packageName: String,
        reason: BlockReason,
    ) {
        blockedPackage = packageName
        val label = AppPackageInfo(this).label(packageName)
        blockHost.show {
            FocoTheme {
                AppLimitBlockScreen(
                    appName = label,
                    reason = reason,
                    onFinished = { finishBlock() },
                )
            }
        }
    }

    private fun finishBlock() {
        val finishedPackage = blockedPackage
        finishedPackage?.let { foreground.blockFinished(it) }
        removeOverlay()
        performGlobalAction(GLOBAL_ACTION_HOME)
        if (finishedPackage != null) {
            recheckJob?.cancel()
            recheckJob =
                scope.launch {
                    delay(RECHECK_FIRST_DELAY_MS)
                    recheckForeground(finishedPackage)
                    delay(RECHECK_SECOND_DELAY_MS)
                    recheckForeground(finishedPackage)
                }
        }
    }

    /** One-shot re-check after our own HOME: a limit screen already showing wins, else re-evaluate. */
    private suspend fun recheckForeground(packageName: String) {
        if (blockHost.isShowing) return
        if (withContext(Dispatchers.IO) { isStillInFront(packageName) }) evaluate(packageName)
    }

    /**
     * Whether the blocked package is still in front and still has something to enforce. The usage
     * is read only when a limit or a window can fire.
     */
    private suspend fun isStillInFront(packageName: String): Boolean {
        if (store.limitFor(packageName) == null && store.windowFor(packageName) == null) return false
        return usage.isForegroundNow(packageName)
    }

    private fun removeOverlay() {
        blockHost.dismiss()
    }

    private companion object {
        const val TAG = "FocoBlock"
        const val SYSTEM_UI = "com.android.systemui"
        const val VIEW_GROUP = "android.view.ViewGroup"

        /** First re-check ~+2 s after HOME; the second delay adds ~+4 s → the ~+6 s re-check. */
        const val RECHECK_FIRST_DELAY_MS = 2_000L
        const val RECHECK_SECOND_DELAY_MS = 4_000L
        val IGNORED_PACKAGES = setOf(SYSTEM_UI)
    }
}
