package com.foco.app.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import java.util.Locale

/**
 * The limits editor for one app: the daily-limit picker, the recurring use window and the alert
 * toggle. It reports picks and writes nothing; the caller persists them and syncs the monitor.
 */
@Composable
fun AppLimitEditor(
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
    Column(modifier = Modifier.fillMaxWidth()) {
        LimitControl(minutes = minutes, onPick = onPick)
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        WindowControl(
            window = window,
            onWindowChange = onWindowChange,
            onWindowClear = onWindowClear,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        AlertControl(
            label = label,
            minutes = minutes,
            alertEnabled = alertEnabled,
            notificationsEnabled = notificationsEnabled,
            onAlertChange = onAlertChange,
        )
    }
}

/** The daily limit: the current step and the dropdown on the zero/15-minute grid. */
@Composable
private fun LimitControl(
    minutes: Int?,
    onPick: (Int?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val none = stringResource(R.string.settings_limit_none)
    val blockNow = stringResource(R.string.settings_limit_block_now)
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
                    .clickable { expanded = true }
                    .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.detail_limits_limit_label),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = limitLabel(minutes, none, blockNow),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
 * The recurring window: `No window`, or a start/end pair on the 15-minute grid. Picking an equal
 * pair is ignored, so the previous window stays.
 */
@Composable
private fun WindowControl(
    window: AppWindow?,
    onWindowChange: (Int, Int) -> Unit,
    onWindowClear: () -> Unit,
) {
    val none = stringResource(R.string.settings_window_none)
    val start = window?.startMinute ?: DEFAULT_START_MINUTE
    val end = window?.endMinute ?: DEFAULT_END_MINUTE
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_window_label),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text =
                if (window == null) {
                    none
                } else {
                    stringResource(R.string.settings_window_range, clockLabel(start), clockLabel(end))
                },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
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
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            TimePicker(
                value = end,
                label = stringResource(R.string.settings_window_to),
            ) { picked ->
                if (start != picked) onWindowChange(start, picked)
            }
            Spacer(Modifier.weight(1f))
            if (window != null) {
                TextButton(onClick = onWindowClear) {
                    Text(stringResource(R.string.settings_window_clear))
                }
            }
        }
    }
}

/** The alert toggle: off by default and disabled until a positive limit exists. */
@Composable
private fun AlertControl(
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
private fun TimePicker(
    value: Int,
    label: String,
    onPick: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.width(88.dp)) {
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
