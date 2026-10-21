package com.foco.app.ui.detail

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.foco.app.R
import com.foco.app.data.AppLimitAlertNotifier
import com.foco.app.data.AppLimitsStore
import com.foco.app.data.UsageMonitorService
import com.foco.app.ui.common.AppIcon
import com.foco.app.ui.common.LimitAlertsToggle
import com.foco.app.ui.common.LimitsQuickAdjust
import com.foco.app.ui.common.ScreenHeader
import com.foco.app.ui.common.appLabel
import kotlinx.coroutines.launch

/**
 * The configuration screen for one app: its limit, window and alerts. It writes through
 * `AppLimitsStore` and syncs the monitor; without [onBack] it uses the back dispatcher.
 */
@Composable
fun AppAppSettingsScreen(
    packageName: String,
    onBack: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val limitsStore = remember { AppLimitsStore.forApp(context.applicationContext) }
    val scope = rememberCoroutineScope()

    val limits by limitsStore.limitsFlow.collectAsState(initial = emptyMap())
    val windows by limitsStore.windowsFlow.collectAsState(initial = emptyMap())
    val alerts by limitsStore.alertsFlow.collectAsState(initial = emptyMap())

    val label = appLabel(packageName)

    var notificationsEnabled by remember {
        mutableStateOf(AppLimitAlertNotifier(context).canNotify())
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        notificationsEnabled = AppLimitAlertNotifier(context).canNotify()
    }
    val notificationPermission =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) {
            notificationsEnabled = AppLimitAlertNotifier(context).canNotify()
        }

    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val minutes = limits[packageName]
    val window = windows[packageName]
    val alertOn = alerts[packageName] == true

    fun syncMonitor() {
        UsageMonitorService.sync(context.applicationContext)
    }

    Scaffold { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            Spacer(Modifier.height(16.dp))

            ScreenHeader(
                title = label,
                subtitle = stringResource(R.string.detail_limits_section),
                onBack = { if (onBack != null) onBack() else backDispatcher?.onBackPressed() },
                titleLeading = { AppIcon(packageName = packageName) },
            )

            Spacer(Modifier.height(8.dp))

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_limits_explanation),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(8.dp))

                LimitsQuickAdjust(
                    store = limitsStore,
                    packageName = packageName,
                    onLimitChange = { pkg, picked ->
                        scope.launch {
                            if (picked == null) {
                                limitsStore.clearLimit(pkg)
                            } else {
                                limitsStore.setLimit(pkg, picked)
                            }
                            syncMonitor()
                        }
                    },
                    onWindowChange = { pkg, start, end ->
                        scope.launch {
                            limitsStore.setWindow(pkg, start, end)
                            syncMonitor()
                        }
                    },
                    onWindowClear = { pkg ->
                        scope.launch {
                            limitsStore.clearWindow(pkg)
                            syncMonitor()
                        }
                    },
                )

                LimitAlertsToggle(
                    label = label,
                    minutes = minutes,
                    alertEnabled = alertOn,
                    notificationsEnabled = notificationsEnabled,
                    onAlertChange = { enabled ->
                        if (enabled) requestNotificationPermission()
                        scope.launch {
                            limitsStore.setAlertEnabled(packageName, enabled)
                            syncMonitor()
                        }
                    },
                )

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
