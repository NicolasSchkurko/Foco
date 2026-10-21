package com.foco.app

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import com.foco.app.data.AppLimitsStore
import com.foco.app.data.AppPackageInfo
import com.foco.app.data.UsageMonitorEvaluator
import com.foco.app.data.UsageMonitorService
import com.foco.app.data.windowBlocks
import com.foco.app.domain.AppLimitRules
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
    private var blockedPackage: String? = null

    private val store by lazy { AppLimitsStore.forApp(this) }
    private val evaluator by lazy { UsageMonitorEvaluator(this) }

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
                val now = System.currentTimeMillis()
                val window = store.windowFor(packageName)
                val limit = store.limitFor(packageName)
                val used = evaluator.usedSecondsToday(packageName, now, window)
                val reason =
                    forcedReason ?: when {
                        windowBlocks(now, window) -> BlockReason.OUTSIDE_WINDOW
                        AppLimitRules.isAtOrOverLimit(limit, used) -> BlockReason.LIMIT
                        else -> null
                    }
                if (reason == null) {
                    armBoundary(packageName, window, now)
                    return@launch
                }
                boundaryJob?.cancel()
                showBlock(packageName, reason)
            } catch (t: Throwable) {
                Log.e(TAG, "evaluate failed for $packageName", t)
            }
        }
    }

    /** Arms the one-shot check for the next window boundary; the 2-minute tick is the fallback. */
    private fun armBoundary(
        packageName: String,
        window: AppWindow?,
        now: Long,
    ) {
        boundaryJob?.cancel()
        if (window == null) return
        val waitMillis = AppLimitWindowRules.nextBoundaryMillis(now, window) - now
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
        blockedPackage?.let { foreground.blockFinished(it) }
        removeOverlay()
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    private fun removeOverlay() {
        blockHost.dismiss()
    }

    private companion object {
        const val TAG = "FocoBlock"
        const val SYSTEM_UI = "com.android.systemui"
        const val VIEW_GROUP = "android.view.ViewGroup"
        val IGNORED_PACKAGES = setOf(SYSTEM_UI)
    }
}
