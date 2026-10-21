package com.foco.app.ui.settings

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.foco.app.data.AppIdentity
import com.foco.app.data.AppLanguage
import com.foco.app.data.ExcludedAppsStore
import com.foco.app.data.SettingsStore
import com.foco.app.model.AppearanceOption
import com.foco.app.model.LanguageOption
import com.foco.app.ui.common.AppIcon
import com.foco.app.ui.common.ScreenHeader
import com.foco.app.ui.common.appLabel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Context
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import com.foco.app.data.AppLimitsStore
import com.foco.app.data.UsageMonitorService
import com.foco.app.domain.AppLimitRules
import com.foco.app.domain.AppLimitWindowRules
import com.foco.app.domain.AppWindow
import com.foco.app.ui.appLocale
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

/**
 * Settings: the About block, language and appearance as one row with a dropdown each, and the
 * management of the apps excluded from the ranking.
 */
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val store = remember { SettingsStore(context.applicationContext) }
    val excludedStore = remember { ExcludedAppsStore(context.applicationContext) }
    val limitsStore = remember { AppLimitsStore.forApp(context.applicationContext) }
    val identity = remember { AppIdentity(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var limitJob by remember { mutableStateOf<Job?>(null) }

    val language by store.language.collectAsState(initial = LanguageOption.AUTO)
    val appearance by store.appearance.collectAsState(initial = AppearanceOption.AUTO)
    val excluded by excludedStore.excludedPackages.collectAsState(initial = emptySet())

    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    Scaffold { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            Spacer(Modifier.height(16.dp))

            ScreenHeader(
                title = stringResource(R.string.settings_title),
                onBack = { backDispatcher?.onBackPressed() },
            )

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .weight(1f),
            ) {
                item {
                    AboutBlock(
                        version = identity.versionName(),
                        onOpenRepository = { context.startActivity(identity.repositoryIntent()) },
                    )
                    Spacer(Modifier.height(16.dp))
                }

                item {
                    DropdownRow(
                        title = stringResource(R.string.settings_language_section),
                        value = languageLabel(language),
                        options =
                            LanguageOption.entries.map { option ->
                                languageLabel(option) to {
                                    // The pick re-creates the activity: the write must not be cancelled with it.
                                    scope.launch {
                                        withContext(NonCancellable) { store.setLanguage(option) }
                                        AppLanguage.applyToSystem(option)
                                    }
                                }
                            },
                    )
                }

                item {
                    Spacer(Modifier.height(16.dp))
                    DropdownRow(
                        title = stringResource(R.string.settings_appearance_section),
                        value = appearanceLabel(appearance),
                        options =
                            AppearanceOption.entries.map { option ->
                                appearanceLabel(option) to {
                                    scope.launch {
                                        withContext(NonCancellable) { store.setAppearance(option) }
                                    }
                                }
                            },
                    )
                }

                item {
                    Spacer(Modifier.height(16.dp))
                    SectionTitle(stringResource(R.string.settings_excluded_section))
                    if (excluded.isEmpty()) {
                        Text(
                            text = stringResource(R.string.settings_excluded_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                items(excluded.sorted()) { packageName ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIcon(packageName = packageName)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = appLabel(packageName),
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { scope.launch { excludedStore.restore(packageName) } }) {
                            Text(stringResource(R.string.settings_restore))
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(24.dp))
                    SectionTitle(stringResource(R.string.detail_limits_section))
                    val limitsStore = remember { AppLimitsStore.forApp(context.applicationContext) }
                    LimitsQuickAdjust(
                        store = limitsStore,
                        packageName = context.applicationContext.packageName,
                        onLimitChange = { pkg, minutes ->
                            limitJob?.cancel()
                            limitJob = scope.launch {
                                if (minutes == null || minutes == 0) {
                                    limitsStore.clearLimit(pkg)
                                } else {
                                    limitsStore.setLimit(pkg, minutes)
                                }
                                UsageMonitorService.sync(context.applicationContext)
                            }
                        },
                        onWindowChange = { pkg, start, end ->
                            scope.launch {
                                limitsStore.setWindow(pkg, start, end)
                                UsageMonitorService.sync(context.applicationContext)
                            }
                        },
                        onWindowClear = { pkg ->
                            scope.launch {
                                limitsStore.clearWindow(pkg)
                                UsageMonitorService.sync(context.applicationContext)
                            }
                        },
                    )
                }
            }
        }
    }
}

/** The About block: credit, installed version and the way to the repository. */
@Composable
private fun AboutBlock(
    version: String?,
    onOpenRepository: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.settings_about_credit),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text =
                stringResource(
                    R.string.settings_about_version,
                    version ?: stringResource(R.string.settings_about_version_unknown),
                ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = onOpenRepository) {
            Text(stringResource(R.string.settings_about_repository))
        }
    }
}

/** One setting: its title, the active label and a chevron that opens the anchored dropdown. */
@Composable
private fun DropdownRow(
    title: String,
    value: String,
    options: List<Pair<String, () -> Unit>>,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true }
                    .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (label, onSelect) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onSelect()
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun languageLabel(option: LanguageOption): String =
    when (option) {
        LanguageOption.AUTO -> stringResource(R.string.settings_option_auto)
        LanguageOption.ES -> stringResource(R.string.settings_language_es)
        LanguageOption.EN -> stringResource(R.string.settings_language_en)
    }

@Composable
private fun appearanceLabel(option: AppearanceOption): String =
    when (option) {
        AppearanceOption.AUTO -> stringResource(R.string.settings_option_auto)
        AppearanceOption.LIGHT -> stringResource(R.string.settings_option_light)
        AppearanceOption.DARK -> stringResource(R.string.settings_option_dark)
    }

@Composable
private fun LimitsQuickAdjust(
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
        String.format(java.util.Locale.ROOT, "%02d:%02d", minute / 60, minute % 60)
    } else {
        val cal =
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, minute / 60)
                set(Calendar.MINUTE, minute % 60)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        android.text.format.DateFormat.getTimeFormat(context).format(cal.time)
    }
