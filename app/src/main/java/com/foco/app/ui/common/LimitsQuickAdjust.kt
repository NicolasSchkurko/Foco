package com.foco.app.ui.common

import android.text.format.DateFormat
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.foco.app.R
import com.foco.app.data.AppLimitsStore
import com.foco.app.domain.AppLimitRules
import com.foco.app.domain.AppLimitWindowRules
import com.foco.app.domain.AppWindow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

/**
 * The daily limit of one app: `-15 min` / `+15 min` buttons around a centered value (long-press
 * accelerates) and the recurring use window with an interval picker. It reports the picks; the
 * caller persists them and syncs the monitor.
 */
@Composable
fun LimitsQuickAdjust(
    store: AppLimitsStore,
    packageName: String,
    onLimitChange: (String, Int?) -> Unit,
    onWindowChange: (String, Int, Int) -> Unit,
    onWindowClear: (String) -> Unit,
) {
    val context = LocalContext.current
    var limit by remember { mutableStateOf<Int?>(null) }
    var window by remember { mutableStateOf<AppWindow?>(null) }
    var showPicker by remember { mutableStateOf(false) }
    var interacted by remember { mutableStateOf(false) }
    LaunchedEffect(packageName) {
        // Only seed from the store when the user has not started editing; otherwise a late load
        // would clobber an in-flight adjustment.
        if (!interacted) {
            limit = store.limitFor(packageName)
            window = store.windowFor(packageName)
        }
    }
    val scope = rememberCoroutineScope()
    val is24 = DateFormat.is24HourFormat(context)

    @Composable
    fun valueLabel(value: Int?): String =
        when (value) {
            null -> stringResource(R.string.settings_limits_value_none)
            0 -> stringResource(R.string.settings_limits_value_block_now)
            else -> stringResource(R.string.settings_limits_value_minutes, value)
        }

    fun applyDelta(delta: Int) {
        val current = limit ?: 0
        if (delta < 0) {
            // Lower limit: never clear "block now" (0) and never fire a spurious clearLimit on "no limit".
            if (current == 0) return
            val newVal =
                ((current - AppLimitRules.STEP_MINUTES).coerceAtLeast(0) / AppLimitRules.STEP_MINUTES * AppLimitRules.STEP_MINUTES)
            val newLimit = if (newVal == 0) null else newVal
            limit = newLimit
            interacted = true
            onLimitChange(packageName, newLimit)
        } else {
            val newVal =
                ((current + AppLimitRules.STEP_MINUTES) / AppLimitRules.STEP_MINUTES * AppLimitRules.STEP_MINUTES)
                    .coerceAtMost(AppLimitRules.MAX_MINUTES)
            val newLimit = if (newVal == 0) null else newVal
            limit = newLimit
            interacted = true
            onLimitChange(packageName, newLimit)
        }
    }

    var repeatJob by remember { mutableStateOf<Job?>(null) }
    fun startRepeat(delta: Int) {
        repeatJob?.cancel()
        repeatJob = scope.launch {
            while (true) {
                delay(150)
                applyDelta(delta)
            }
        }
    }

    Column(modifier = Modifier.padding(12.dp)) {
        Text(stringResource(R.string.settings_limits_quick_adjust), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = { applyDelta(-AppLimitRules.STEP_MINUTES) },
                modifier =
                    Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    try {
                                        awaitRelease()
                                    } finally {
                                        repeatJob?.cancel()
                                        repeatJob = null
                                    }
                                },
                                onLongPress = { startRepeat(-AppLimitRules.STEP_MINUTES) },
                            )
                        },
            ) { Text(stringResource(R.string.settings_limits_button_minus)) }
            Text(
                text = valueLabel(limit),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(
                onClick = { applyDelta(AppLimitRules.STEP_MINUTES) },
                modifier =
                    Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    try {
                                        awaitRelease()
                                    } finally {
                                        repeatJob?.cancel()
                                        repeatJob = null
                                    }
                                },
                                onLongPress = { startRepeat(AppLimitRules.STEP_MINUTES) },
                            )
                        },
            ) { Text(stringResource(R.string.settings_limits_button_plus)) }
        }
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.settings_limits_picker_label), style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        val w = window
        val rangeText =
            if (w == null) {
                stringResource(R.string.settings_window_none)
            } else {
                stringResource(
                    R.string.settings_window_range,
                    formatMinuteOfDay(context, w.startMinute, is24),
                    formatMinuteOfDay(context, w.endMinute, is24),
                )
            }
        Text(rangeText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = { showPicker = true },
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
            ) { Text(stringResource(R.string.settings_limits_picker_label)) }
            Spacer(Modifier.width(8.dp))
            if (window != null) {
                TextButton(
                    onClick = {
                        window = null
                        onWindowClear(packageName)
                    },
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                ) { Text(stringResource(R.string.settings_window_clear)) }
            }
        }
    }

    if (showPicker) {
        var pendingStart by remember { mutableStateOf(window?.startMinute ?: (9 * 60)) }
        var pendingEnd by remember { mutableStateOf(window?.endMinute ?: (23 * 60)) }
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text(stringResource(R.string.settings_limits_picker_title)) },
            text = {
                Column {
                    TimeRangePicker(
                        startMinute = pendingStart,
                        endMinute = pendingEnd,
                        is24 = is24,
                        context = context,
                        onChange = { s, e ->
                            pendingStart = s
                            pendingEnd = e
                        },
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (pendingStart != pendingEnd) {
                            window = AppWindow(pendingStart, pendingEnd)
                            onWindowChange(packageName, pendingStart, pendingEnd)
                        }
                        showPicker = false
                    },
                    enabled = pendingStart != pendingEnd,
                ) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text(stringResource(android.R.string.cancel)) }
            },
        )
    }
}

/** The alert toggle of one app: off by default and disabled until a positive limit exists. */
@Composable
fun LimitAlertsToggle(
    label: String,
    minutes: Int?,
    alertEnabled: Boolean,
    notificationsEnabled: Boolean,
    onAlertChange: (Boolean) -> Unit,
) {
    val alertsLabel = stringResource(R.string.settings_limit_alerts_toggle, label)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.detail_limits_alerts_label),
                style = MaterialTheme.typography.titleMedium,
            )
            if (alertEnabled && !notificationsEnabled) {
                Text(
                    text = stringResource(R.string.settings_limit_alerts_no_permission),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        Switch(
            checked = alertEnabled,
            onCheckedChange = onAlertChange,
            enabled = minutes != null && minutes > 0,
            modifier =
                Modifier
                    .padding(start = 8.dp)
                    .semantics { contentDescription = alertsLabel },
        )
    }
}

@Composable
private fun TimeRangePicker(
    startMinute: Int,
    endMinute: Int,
    is24: Boolean,
    context: android.content.Context,
    onChange: (Int, Int) -> Unit,
) {
    var start by remember { mutableStateOf(startMinute) }
    var end by remember { mutableStateOf(endMinute) }
    LaunchedEffect(startMinute, endMinute) {
        start = startMinute
        end = endMinute
    }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TimeDropdown(
                value = start,
                label = stringResource(R.string.settings_window_from),
                is24 = is24,
                context = context,
            ) { picked ->
                if (picked != end) {
                    start = picked
                    onChange(start, end)
                }
            }
            Text(" – ", modifier = Modifier.padding(horizontal = 4.dp))
            TimeDropdown(
                value = end,
                label = stringResource(R.string.settings_window_to),
                is24 = is24,
                context = context,
            ) { picked ->
                if (start != picked) {
                    end = picked
                    onChange(start, end)
                }
            }
        }
    }
}

@Composable
private fun TimeDropdown(
    value: Int,
    label: String,
    is24: Boolean,
    context: android.content.Context,
    onPick: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier =
                Modifier
                    .semantics { contentDescription = label }
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp),
        ) {
            Text(formatMinuteOfDay(context, value, is24))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AppLimitWindowRules.options().filter { it % 30 == 0 }.forEach { minute ->
                DropdownMenuItem(
                    text = { Text(formatMinuteOfDay(context, minute, is24)) },
                    onClick = {
                        onPick(minute)
                        expanded = false
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
    }
}

private fun formatMinuteOfDay(
    context: android.content.Context,
    minute: Int,
    is24: Boolean,
): String =
    if (is24) {
        String.format(Locale.ROOT, "%02d:%02d", minute / 60, minute % 60)
    } else {
        val cal =
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, minute / 60)
                set(Calendar.MINUTE, minute % 60)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        DateFormat.getTimeFormat(context).format(cal.time)
    }
