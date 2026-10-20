package com.foco.app

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.os.Build
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import com.foco.app.data.AppLimitsStore
import com.foco.app.data.AppPackageInfo
import com.foco.app.data.UsageMonitorEvaluator
import com.foco.app.data.UsageMonitorService
import com.foco.app.domain.AppLimitRules
import com.foco.app.domain.ForegroundApp
import com.foco.app.domain.ForegroundAppTracker
import com.foco.app.ui.limits.AppLimitBlockScreen
import com.foco.app.ui.theme.FocoTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * The app's accessibility entry point: reads the foreground package, hosts the three-second block
 * overlay and answers the monitor. Resource: the accessibility binding; lives while enabled.
 */
class UsageMonitorAccessibilityService :
    AccessibilityService(),
    ForegroundAppTracker {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var overlay: ComposeView? = null

    @Volatile
    private var foregroundPackage: String? = null

    private val store by lazy { AppLimitsStore.forApp(this) }
    private val evaluator by lazy { UsageMonitorEvaluator(this) }

    private val inputMethodPackages: Set<String> by lazy {
        getSystemService(InputMethodManager::class.java)
            ?.enabledInputMethodList
            ?.map { it.packageName }
            ?.toSet()
            .orEmpty()
    }

    override fun onServiceConnected() {
        ForegroundApp.register(this)
        UsageMonitorService.sync(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        if (packageName == this.packageName || packageName in IGNORED_PACKAGES) return
        if (packageName in inputMethodPackages) return

        foregroundPackage = packageName
        evaluate(packageName)
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        removeOverlay()
        foregroundPackage = null
        ForegroundApp.unregister(this)
        UsageMonitorService.stop(this)
        scope.cancel()
        super.onDestroy()
    }

    override fun currentPackage(): String? = foregroundPackage

    override fun requestBlock(packageName: String) {
        evaluate(packageName)
    }

    private fun evaluate(packageName: String) {
        scope.launch {
            if (overlay != null) return@launch
            val limit = store.limitFor(packageName) ?: return@launch
            val used = evaluator.usedSecondsToday(packageName)
            if (!AppLimitRules.isAtOrOverLimit(limit, used)) return@launch
            showBlock(packageName)
        }
    }

    private fun showBlock(packageName: String) {
        val label = AppPackageInfo(this).label(packageName)
        val lifecycleOwner =
            object : LifecycleOwner {
                val registry = LifecycleRegistry(this)
                override val lifecycle: Lifecycle get() = registry
            }

        val view =
            ComposeView(this).apply {
                setViewTreeLifecycleOwner(lifecycleOwner)
                setContent {
                    FocoTheme {
                        AppLimitBlockScreen(appName = label, onFinished = { finishBlock() })
                    }
                }
            }

        val params =
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT,
            )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            params.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        getSystemService(WindowManager::class.java).addView(view, params)
        overlay = view
        lifecycleOwner.registry.currentState = Lifecycle.State.RESUMED
    }

    private fun finishBlock() {
        removeOverlay()
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    private fun removeOverlay() {
        val view = overlay ?: return
        runCatching { getSystemService(WindowManager::class.java).removeView(view) }
        runCatching { view.disposeComposition() }
        overlay = null
    }

    private companion object {
        const val SYSTEM_UI = "com.android.systemui"
        val IGNORED_PACKAGES = setOf(SYSTEM_UI)
    }
}
