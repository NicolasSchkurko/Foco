package com.foco.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.foco.app.R
import com.foco.app.domain.AppLimitRules
import com.foco.app.domain.AppLimitWindowRules
import com.foco.app.domain.AppWindow
import com.foco.app.ui.common.AppIcon
import java.util.Locale

/**
 * One row per launchable app with its daily-limit picker, recurring window and alert toggle.
 * [apps] are package/label pairs; a package missing from [limits]/[windows] has none configured.
 */
fun LazyListScope.appLimitsSection(
    apps: List<Pair<String, String>>,
    limits: Map<String, Int>,
    windows: Map<String, AppWindow>,
    alerts: Map<String, Boolean>,
    notificationsEnabled: Boolean,
    onLimitChange: (String, Int?) -> Unit,
    onWindowChange: (String, Int, Int) -> Unit,
    onWindowClear: (String) -> Unit,
    onAlertChange: (String, Boolean) -> Unit,
) {
    item {
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.settings_limits_section),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.settings_limits_explanation),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
    }

    items(apps, key = { it.first }) { (packageName, label) ->
        AppLimitRow(
            packageName = packageName,
            label = label,
            minutes = limits[packageName],
            window = windows[packageName],
            alertEnabled = alerts[packageName] == true,
            notificationsEnabled = notificationsEnabled,
            onPick = { minutes -> onLimitChange(packageName, minutes) },
            onWindowChange = { start, end -> onWindowChange(packageName, start, end) },
            onWindowClear = { onWindowClear(packageName) },
            onAlertChange = { enabled -> onAlertChange(packageName, enabled) },
        )
    }
}

@Composable
private fun AppLimitRow(
    packageName: String,
    label: String,
    minutes: Int?,
    window: AppWindow?,
    alertEnabled: Boolean,
    notificationsEnabled: Boolean,
    onPick: (Int?) -> Unit,
    onWindowChange: (Int, Int) -> Unit,
    onWindowClear: () -> Unit,
    onAlertChange: (Boolean) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val none = stringResource(R.string.settings_limit_none)
    val blockNow = stringResource(R.string.settings_limit_block_now)
    val alertsLabel = stringResource(R.string.settings_limit_alerts_toggle, label)
    val options =
        remember(none, blockNow) {
            listOf<Pair<String, Int?>>(
                none to null,
                blockNow to AppLimitRules.BLOCK_IMMEDIATELY_MINUTES,
            ) + AppLimitRules.options().drop(1).map { limitLabel(it, none, blockNow) to it }
        }

    Box {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier =
                    Modifier
                        .weight(1f)
                        .clickable { expanded = true },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIcon(packageName)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                    )
                    Text(
                        text = limitLabel(minutes, none, blockNow),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    WindowControls(
                        window = window,
                        onWindowChange = onWindowChange,
                        onWindowClear = onWindowClear,
                    )
                    if (alertEnabled && !notificationsEnabled) {
                        Text(
                            text = stringResource(R.string.settings_limit_alerts_no_permission),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
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
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (optionLabel, option) ->
                DropdownMenuItem(
                    text = { Text(optionLabel) },
                    onClick = {
                        onPick(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

/**
 * The recurring-window control: `No window`, or a start/end pair on the 15-minute grid. Picking an
 * equal pair is ignored, so the previous window stays.
 */
@Composable
private fun WindowControls(
    window: AppWindow?,
    onWindowChange: (Int, Int) -> Unit,
    onWindowClear: () -> Unit,
) {
    val none = stringResource(R.string.settings_window_none)
    val start = window?.startMinute ?: DEFAULT_START_MINUTE
    val end = window?.endMinute ?: DEFAULT_END_MINUTE
    val summary =
        if (window == null) {
            none
        } else {
            stringResource(R.string.settings_window_range, clockLabel(start), clockLabel(end))
        }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.settings_window_label),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = summary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        TimePicker(
            value = start,
            label = stringResource(R.string.settings_window_from),
        ) { picked ->
            if (picked != end) onWindowChange(picked, end)
        }
        Text(
            text = "–",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TimePicker(
            value = end,
            label = stringResource(R.string.settings_window_to),
        ) { picked ->
            if (start != picked) onWindowChange(start, picked)
        }
        if (window != null) {
            TextButton(onClick = onWindowClear) {
                Text(stringResource(R.string.settings_window_clear))
            }
        }
    }
}

@Composable
private fun TimePicker(
    value: Int,
    label: String,
    onPick: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(
            onClick = { expanded = true },
            modifier = Modifier.semantics { contentDescription = label },
        ) {
            Text(clockLabel(value), style = MaterialTheme.typography.bodyMedium)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AppLimitWindowRules.options().forEach { minute ->
                DropdownMenuItem(
                    text = { Text(clockLabel(minute)) },
                    onClick = {
                        onPick(minute)
                        expanded = false
                    },
                )
            }
        }
    }
}

private fun clockLabel(minute: Int): String = String.format(Locale.ROOT, "%02d:%02d", minute / 60, minute % 60)

private const val DEFAULT_START_MINUTE = 9 * 60
private const val DEFAULT_END_MINUTE = 23 * 60

/** `No limit`, `Block immediately`, `15 min`, `1 h`, `1 h 15 min` or `24 h`. */
private fun limitLabel(
    minutes: Int?,
    none: String,
    blockNow: String,
): String {
    if (minutes == null) return none
    if (minutes == AppLimitRules.BLOCK_IMMEDIATELY_MINUTES) return blockNow
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours > 0 && rest > 0 -> "$hours h $rest min"
        hours > 0 -> "$hours h"
        else -> "$minutes min"
    }
}
