package com.foco.app.ui.detail

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.foco.app.R
import com.foco.app.data.AppLimitsStore
import com.foco.app.data.SessionHistoryStore
import com.foco.app.data.UsageRepository
import com.foco.app.domain.DaySource
import com.foco.app.domain.availableDays
import com.foco.app.domain.daySource
import com.foco.app.domain.recentDays
import com.foco.app.domain.reclampDay
import com.foco.app.domain.stepCyclicIndex
import com.foco.app.model.UsageSession
import com.foco.app.ui.common.AppIcon
import com.foco.app.ui.common.EmptyState
import com.foco.app.ui.common.HourChart
import com.foco.app.ui.common.LoadingState
import com.foco.app.ui.common.ScreenHeader
import com.foco.app.ui.common.appLabel
import com.foco.app.ui.common.readOr
import com.foco.app.ui.common.rememberAppLocale
import com.foco.app.ui.common.rememberCurrentDate
import com.foco.app.ui.common.rememberDayLabel
import com.foco.app.ui.common.swipeThroughPositions
import com.foco.app.ui.formatDuration
import com.foco.app.ui.formatMinuteOfDay24
import com.foco.app.ui.formatTimeOfDay
import com.foco.app.ui.metrics.AppDayDetail
import com.foco.app.ui.metrics.appDayDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

/** Tag of the day label; the tests read it to see which day is shown. */
const val DAY_LABEL_TAG = "detail_day_label"

/** Tag of the detail's content; the tests address it without depending on the language. */
const val DETAIL_CONTENT_TAG = "detail_content"

/** Tag of the limits preview block; the tests address it without depending on the language. */
const val LIMITS_PREVIEW_TAG = "detail_limits_preview"

/**
 * One app's detail: hourly breakdown, the day's summary and the session list, with a ‹ day ›
 * selector that walks back from today to the oldest day the store has; [date] opens on that day.
 */
@Composable
fun AppDetailScreen(
    packageName: String,
    date: LocalDate? = null,
    clock: Clock = Clock.systemDefaultZone(),
    onConfigure: () -> Unit = {},
) {
    val context = LocalContext.current
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val repository = remember { UsageRepository.forApp(context.applicationContext) }
    val limitsStore = remember { AppLimitsStore.forApp(context.applicationContext) }
    val store = remember { SessionHistoryStore.forApp(context.applicationContext) { date -> repository.loadDay(date) } }
    val today = rememberCurrentDate(clock)
    val limits by limitsStore.limitsFlow.collectAsState(initial = emptyMap())
    val windows by limitsStore.windowsFlow.collectAsState(initial = emptyMap())
    val alerts by limitsStore.alertsFlow.collectAsState(initial = emptyMap())
    var storedDays by remember { mutableStateOf<Set<LocalDate>?>(null) }
    var selectedIndex by remember { mutableIntStateOf(0) } // 0 = today; it grows towards the past
    // Survives recreation (rotation / Settings round-trip): a date key, not a raw index, so a
    // shrunken window resolves through reclampDay instead of crashing.
    var savedSelectedDate by rememberSaveable { mutableStateOf<String?>(null) }
    // The window the selection was made in, so a rebuilt window can keep the same day.
    var selectedWindow by remember { mutableStateOf<List<LocalDate>>(emptyList()) }
    var sessions by remember { mutableStateOf<List<UsageSession>?>(null) }
    // The day's summary, derived off the main thread when the day's sessions are read.
    var detail by remember { mutableStateOf<AppDayDetail?>(null) }
    // Resume trigger: bumps on every return to the foreground so the load effect re-runs
    // (first show is handled by the effect itself; later resumes re-read the visible day).
    var refreshKey by remember { mutableStateOf(0) }
    var hasResumedOnce by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (hasResumedOnce) refreshKey++
        hasResumedOnce = true
    }

    // Which days the store has grows while the screen is open (the startup backfill writes totals),
    // so the window follows the store's stream instead of being re-read on resume. Until the first
    // emission — and if the stream fails — it is today alone, which keeps the screen usable.
    LaunchedEffect(store) {
        store.storedDaysFlow().readOr(emptySet<LocalDate>()).collect { storedDays = it }
    }

    val days =
        remember(today, storedDays, date) {
            val available = availableDays(today, storedDays.orEmpty())
            // Any bar the week offers opens its day, even one the store has nothing for.
            if (date != null && date !in available && date in recentDays(today)) {
                (available + date).sortedDescending()
            } else {
                available
            }
        }

    // The day the route asked for, applied once the window can hold it: the stored days arrive
    // asynchronously, and applying the request to the first (today-alone) window would land on
    // the wrong day once the list grows. Afterwards the user's taps on the selector win.
    var requested by remember { mutableStateOf(date) }

    // The window is rebuilt when today rolls over or the store changes, so the selection is
    // reclamped: the same DAY when it is still offered, the nearest valid one otherwise. The
    // resolution is by date, never by the old index, which may point at another day.
    val windowChanged = selectedWindow != days

    fun resolveSelection(): Int {
        val pending = requested?.let { day -> days.indexOf(day).takeIf { it >= 0 } }
        if (pending != null) return pending
        val remembered = savedSelectedDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        if (remembered != null) {
            val clamped = reclampDay(remembered, days, today)
            return days.indexOf(clamped).takeIf { it >= 0 } ?: 0
        }
        return selectedIndex.coerceIn(0, days.lastIndex.coerceAtLeast(0))
    }
    val effectiveIndex =
        if (windowChanged) {
            resolveSelection()
        } else {
            selectedIndex.coerceIn(0, days.lastIndex.coerceAtLeast(0))
        }

    // One day forward (older) or backward (newer) inside the window; shared by the arrows and
    // the horizontal swipe, which walks the days the way the home walks its ring.
    fun stepDay(delta: Int) {
        if (days.isEmpty()) return
        // The walk cycles: past the oldest day it starts again on today, and on today a swipe
        // back continues with the oldest one — the same ring the home walks.
        val target = stepCyclicIndex(effectiveIndex, days.size, delta)
        selectedIndex = target
        days.getOrNull(target)?.toString()?.let { savedSelectedDate = it }
    }
    // The day walk is called straight from the swipe's lambdas (like the home's ring): a
    // reference to this local function would keep the first composition's window forever,
    // because Kotlin compares two local-function references by owner and name, not by the
    // values they captured.
    SideEffect {
        if (windowChanged) {
            val clamped = resolveSelection()
            if (selectedIndex != clamped) selectedIndex = clamped
            selectedWindow = days
        }
        days.getOrNull(effectiveIndex)?.toString()?.let { savedSelectedDate = it }
    }

    LaunchedEffect(days, storedDays) {
        if (storedDays != null) {
            val index = days.indexOf(requested)
            if (index >= 0) {
                selectedIndex = index
                savedSelectedDate = requested?.toString()
                requested = null
            }
        }
    }

    // Survives recreation: when the store has loaded, resolve the saved date key against the
    // current window (reclamp keeps it inside a shrunken window) and restore the selection.
    LaunchedEffect(days, storedDays, savedSelectedDate) {
        val saved = savedSelectedDate?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() } ?: return@LaunchedEffect
        if (storedDays == null) return@LaunchedEffect
        val idx = days.indexOf(reclampDay(saved, days, today)).takeIf { it >= 0 } ?: 0
        if (selectedIndex != idx) selectedIndex = idx
    }

    val selectedDate = days.getOrNull(effectiveIndex) ?: today
    LaunchedEffect(packageName, selectedDate, storedDays, today, refreshKey) {
        sessions = null
        detail = null
        val loaded =
            withContext(Dispatchers.IO) {
                // Today is never consolidated, so the live read answers for it and the store for the rest.
                // A failed read answers an empty day, which is the empty state the screen already has.
                val loadedSessions =
                    readOr(emptyList<UsageSession>()) {
                        when (daySource(selectedDate, today, storedDays.orEmpty())) {
                            DaySource.Live -> repository.loadDay(selectedDate).sessions
                            DaySource.Stored -> store.sessionsFor(selectedDate)
                            DaySource.Absent -> emptyList()
                        }
                    }
                // The day's summary is derived here, off the main thread: it walks every session.
                loadedSessions to appDayDetail(loadedSessions, packageName, ZoneId.systemDefault())
            }
        sessions = loaded.first
        detail = loaded.second
    }

    val openLimitsLabel = stringResource(R.string.detail_limits_configure)
    // The locale and the day labels are read once for the screen, not once per label.
    val locale = rememberAppLocale()
    val dayLabel = rememberDayLabel(today, locale)
    val previousDay = stringResource(R.string.day_previous)
    val nextDay = stringResource(R.string.day_next)

    Scaffold { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            Spacer(Modifier.height(16.dp))

            ScreenHeader(
                title = appLabel(packageName),
                subtitle =
                    detail?.let {
                        stringResource(R.string.detail_day_usage, formatDuration(it.totalMillis))
                    } ?: stringResource(R.string.loading),
                onBack = { backDispatcher?.onBackPressed() },
                titleLeading = { AppIcon(packageName = packageName) },
                actions = {
                    IconButton(onClick = onConfigure) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = stringResource(R.string.detail_limits_configure),
                        )
                    }
                },
            )

            Spacer(Modifier.height(12.dp))

            // ‹ day › selector: ‹ goes back in time, › forward; disabled at the ends.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                IconButton(
                    onClick = { stepDay(1) },
                    enabled = effectiveIndex < days.lastIndex,
                    modifier = Modifier.semantics { contentDescription = previousDay },
                ) {
                    Text("‹", style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    text = dayLabel(selectedDate),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.width(150.dp).testTag(DAY_LABEL_TAG),
                    textAlign = TextAlign.Center,
                )
                IconButton(
                    onClick = { stepDay(-1) },
                    enabled = effectiveIndex > 0,
                    modifier = Modifier.semantics { contentDescription = nextDay },
                ) {
                    Text("›", style = MaterialTheme.typography.titleLarge)
                }
            }

            Spacer(Modifier.height(8.dp))

            val current = detail
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag(DETAIL_CONTENT_TAG)
                        .swipeThroughPositions(
                            onNext = { stepDay(1) },
                            onPrevious = { stepDay(-1) },
                        ),
            ) {
                when {
                    sessions == null ->
                        LoadingState(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                        )

                    current == null ->
                        EmptyState(
                            text = stringResource(R.string.detail_empty),
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                        )

                    else ->
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .weight(1f),
                        ) {
                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable(
                                            onClickLabel = openLimitsLabel,
                                            role = Role.Button,
                                            onClick = onConfigure,
                                        ).padding(vertical = 4.dp)
                                        .testTag(LIMITS_PREVIEW_TAG),
                            ) {
                                // Preview: limit / window / alert for this app
                                Text(
                                    text = stringResource(R.string.detail_limits_section),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Spacer(Modifier.height(4.dp))
                                val minutes = limits[packageName]
                                val window = windows[packageName]
                                val alertOn = alerts[packageName] == true
                                val preview =
                                    listOf(
                                        stringResource(R.string.detail_limits_limit_label) to previewLimitLabel(minutes),
                                        stringResource(R.string.settings_window_label) to
                                            (
                                                if (window ==
                                                    null
                                                ) {
                                                    stringResource(R.string.settings_window_none)
                                                } else {
                                                    stringResource(
                                                        R.string.settings_window_range,
                                                        formatMinuteOfDay24(window.startMinute),
                                                        formatMinuteOfDay24(window.endMinute),
                                                    )
                                                }
                                            ),
                                        stringResource(R.string.detail_limits_alerts_label) to
                                            (
                                                if (alertOn) {
                                                    stringResource(
                                                        R.string.detail_limits_alerts_on,
                                                    )
                                                } else {
                                                    stringResource(R.string.detail_limits_alerts_off)
                                                }
                                            ),
                                    )
                                preview.forEach { (label, value) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Text(text = value, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                            Spacer(Modifier.height(16.dp))

                            HourChart(current.hourly)
                            Spacer(Modifier.height(16.dp))

                            Text(
                                text = stringResource(R.string.detail_summary),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Spacer(Modifier.height(4.dp))
                            val summary =
                                listOf(
                                    stringResource(R.string.detail_total) to formatDuration(current.totalMillis),
                                    stringResource(R.string.detail_sessions) to current.sessionCount.toString(),
                                    stringResource(R.string.detail_first) to formatTimeOfDay(current.firstStart),
                                    stringResource(R.string.detail_last) to formatTimeOfDay(current.lastEnd),
                                    stringResource(R.string.detail_average) to formatDuration(current.averageMillis),
                                    stringResource(R.string.detail_longest) to formatDuration(current.longestMillis),
                                )
                            summary.forEach { (label, value) ->
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(text = value, style = MaterialTheme.typography.bodyMedium)
                                }
                            }

                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.detail_sessions),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Spacer(Modifier.height(4.dp))
                            LazyColumn(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                            ) {
                                items(current.sessions) { session ->
                                    Row(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            text = "${formatTimeOfDay(session.start)} – ${formatTimeOfDay(session.end)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                        Text(
                                            text = formatDuration(session.end - session.start),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                                }
                            }
                        }
                }
            }
        }
    }
}

/** The limit as the preview shows it: the same words the editor shows. */
@Composable
private fun previewLimitLabel(minutes: Int?): String =
    when (minutes) {
        null -> stringResource(R.string.settings_limits_value_none)
        0 -> stringResource(R.string.settings_limits_value_block_now)
        else -> stringResource(R.string.settings_limits_value_minutes, minutes)
    }
