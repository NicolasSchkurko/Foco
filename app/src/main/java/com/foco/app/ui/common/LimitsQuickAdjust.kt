package com.foco.app.ui.common

import android.content.Context
import android.text.format.DateFormat
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.foco.app.R
import com.foco.app.domain.AppLimitRules
import com.foco.app.domain.AppLimitWindowRules
import com.foco.app.domain.AppWindow
import com.foco.app.ui.formatMinuteOfDay24
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * The daily limit of one app: `-15 min` / `+15 min` around a centered value (long-press
 * accelerates) plus the window picker. It shows the values it is given and reports the picks.
 */
@Composable
fun LimitsQuickAdjust(
    limit: Int?,
    window: AppWindow?,
    onLimitChange: (Int?) -> Unit,
    onWindowChange: (Int, Int) -> Unit,
    onWindowClear: () -> Unit,
) {
    val context = LocalContext.current
    // The values on screen: the caller's, until the user picks another one. The pick stays until
    // the caller echoes it back, so a slow write never shows the old value again.
    var draftLimit by remember(limit) { mutableStateOf(limit) }
    var draftWindow by remember(window) { mutableStateOf(window) }
    var showPicker by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val is24 = DateFormat.is24HourFormat(context)
    val limitLabel = stringResource(R.string.detail_limits_limit_label)
    // The press each button is holding: outside the snapshot, so a step does not recompose.
    val minusPress = remember { StepPress() }
    val plusPress = remember { StepPress() }

    @Composable
    fun valueLabel(value: Int?): String =
        when (value) {
            null -> stringResource(R.string.settings_limits_value_none)
            0 -> stringResource(R.string.settings_limits_value_block_now)
            else -> stringResource(R.string.settings_limits_value_minutes, value)
        }

    fun applyDelta(delta: Int) {
        val current = draftLimit ?: 0
        if (delta < 0) {
            // Lower limit: 0 ("block now") is the floor of the grid, so the step never clears it.
            if (current == 0) return
            val newValue =
                (
                    (current - AppLimitRules.STEP_MINUTES).coerceAtLeast(0) /
                        AppLimitRules.STEP_MINUTES * AppLimitRules.STEP_MINUTES
                )
            draftLimit = newValue
            onLimitChange(newValue)
        } else {
            val newValue =
                ((current + AppLimitRules.STEP_MINUTES) / AppLimitRules.STEP_MINUTES * AppLimitRules.STEP_MINUTES)
                    .coerceAtMost(AppLimitRules.MAX_MINUTES)
            draftLimit = newValue
            onLimitChange(newValue)
        }
    }

    // The step the gesture and the click apply: the current one, so a step never restarts them.
    val currentApplyDelta by rememberUpdatedState<(Int) -> Unit>(::applyDelta)

    // The step the click applies: a tap steps once, a held press already repeated the step.
    fun stepFromClick(
        press: StepPress,
        delta: Int,
    ) {
        if (press.gestureSteps > press.accountedSteps) {
            press.accountedSteps = press.gestureSteps
        } else {
            currentApplyDelta(delta)
        }
    }

    fun startRepeat(
        press: StepPress,
        delta: Int,
    ) {
        press.repeatJob?.cancel()
        press.repeatJob =
            scope.launch {
                var wait = 400L
                while (true) {
                    delay(wait)
                    currentApplyDelta(delta)
                    press.gestureSteps++
                    // The longer it is held, the faster it repeats.
                    wait = (wait - 50L).coerceAtLeast(100L)
                }
            }
    }

    // A press held past the long-press threshold repeats the step while it is held; a tap is
    // the button's own click. Written by hand: the button's clickable would consume the gesture.
    suspend fun PointerInputScope.holdGesture(
        press: StepPress,
        delta: Int,
    ) {
        awaitPointerEventScope {
            while (true) {
                val down = awaitPointerEvent(PointerEventPass.Initial)
                if (!down.changes.any { it.changedToDown() }) continue
                val longPress =
                    scope.launch {
                        delay(viewConfiguration.longPressTimeoutMillis)
                        startRepeat(press, delta)
                    }
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.changes.all { it.changedToUp() }) break
                }
                longPress.cancel()
                press.stopRepeat()
            }
        }
    }

    val limitOn = draftLimit != null
    Column {
        // The switch is the binary decision (a limit or none); the stepper only sets its size.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.detail_limits_limit_label), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.settings_limits_enable_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = limitOn,
                onCheckedChange = { on ->
                    if (on) {
                        val value = draftLimit ?: AppLimitRules.STEP_MINUTES
                        draftLimit = value
                        onLimitChange(value)
                    } else {
                        draftLimit = null
                        onLimitChange(null)
                    }
                },
                modifier = Modifier.semantics { contentDescription = limitLabel },
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = { stepFromClick(minusPress, -AppLimitRules.STEP_MINUTES) },
                enabled = limitOn && (draftLimit ?: 0) > 0,
                modifier =
                    Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .pointerInput(Unit) { holdGesture(minusPress, -AppLimitRules.STEP_MINUTES) },
            ) { Text(stringResource(R.string.settings_limits_button_minus)) }
            val currentValue = valueLabel(draftLimit)
            Text(
                text = currentValue,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .weight(1f)
                        .semantics {
                            stateDescription = currentValue
                            liveRegion = LiveRegionMode.Polite
                        },
            )
            OutlinedButton(
                onClick = { stepFromClick(plusPress, AppLimitRules.STEP_MINUTES) },
                enabled = limitOn,
                modifier =
                    Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .pointerInput(Unit) { holdGesture(plusPress, AppLimitRules.STEP_MINUTES) },
            ) { Text(stringResource(R.string.settings_limits_button_plus)) }
        }
        Spacer(Modifier.height(12.dp))
        val w = draftWindow
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
        // The same title-and-value shape as the two rows above, so the three line up.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_limits_picker_label),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = rangeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (draftWindow != null) {
                TextButton(
                    onClick = {
                        draftWindow = null
                        onWindowClear()
                    },
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                ) { Text(stringResource(R.string.settings_window_clear)) }
            }
            OutlinedButton(
                onClick = { showPicker = true },
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
            ) { Text(stringResource(R.string.settings_limits_picker_open)) }
        }
    }

    if (showPicker) {
        var pendingStart by remember { mutableStateOf(draftWindow?.startMinute ?: (9 * 60)) }
        var pendingEnd by remember { mutableStateOf(draftWindow?.endMinute ?: (23 * 60)) }
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
                            draftWindow = AppWindow(pendingStart, pendingEnd)
                            onWindowChange(pendingStart, pendingEnd)
                        }
                        showPicker = false
                    },
                    enabled = pendingStart != pendingEnd,
                ) { Text(stringResource(R.string.settings_limits_picker_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.settings_limits_picker_cancel)) }
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
                .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.detail_limits_alerts_label),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.settings_limits_alerts_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (alertEnabled && !notificationsEnabled) {
                Text(
                    text = stringResource(R.string.settings_limit_alerts_no_permission),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Switch(
            checked = alertEnabled,
            onCheckedChange = onAlertChange,
            // Any limit counts, including "block now".
            enabled = minutes != null,
            modifier = Modifier.semantics { contentDescription = alertsLabel },
        )
    }
}

@Composable
private fun TimeRangePicker(
    startMinute: Int,
    endMinute: Int,
    is24: Boolean,
    context: Context,
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
    context: Context,
    onPick: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier =
                Modifier
                    .semantics { contentDescription = "$label: ${formatMinuteOfDay(context, value, is24)}" }
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp),
        ) {
            Text(formatMinuteOfDay(context, value, is24))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AppLimitWindowRules.options().forEach { minute ->
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
    context: Context,
    minute: Int,
    is24: Boolean,
): String =
    if (is24) {
        formatMinuteOfDay24(minute)
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
