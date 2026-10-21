package com.foco.app.ui.detail

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.foco.app.R
import com.foco.app.data.AppLimitsStore
import com.foco.app.data.SessionHistoryStore
import com.foco.app.data.UsageRepository
import com.foco.app.domain.DaySource
import com.foco.app.domain.availableDays
import com.foco.app.domain.reclampDayIndex
import com.foco.app.domain.daySource
import com.foco.app.model.UsageSession
import com.foco.app.ui.common.AppIcon
import com.foco.app.ui.common.HourChart
import com.foco.app.ui.common.ScreenHeader
import com.foco.app.ui.common.appLabel
import com.foco.app.ui.common.readOr
import com.foco.app.ui.common.rememberCurrentDate
import com.foco.app.ui.formatDayLabel
import com.foco.app.ui.formatDuration
import com.foco.app.ui.formatTimeOfDay
import com.foco.app.ui.metrics.appDayDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

private const val SWIPE_CLOSE_THRESHOLD_PX = 500f

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
    val repository = remember { UsageRepository(context.applicationContext) }
    val limitsStore = remember { AppLimitsStore.forApp(context.applicationContext) }
    val store = remember { SessionHistoryStore.forApp(context.applicationContext) { date -> repository.loadDay(date) } }
    val today = rememberCurrentDate(clock)
    val limits by limitsStore.limitsFlow.collectAsState(initial = emptyMap())
    val windows by limitsStore.windowsFlow.collectAsState(initial = emptyMap())
    val alerts by limitsStore.alertsFlow.collectAsState(initial = emptyMap())
    var storedDays by remember { mutableStateOf<Set<LocalDate>?>(null) }
    var selectedIndex by remember { mutableIntStateOf(0) } // 0 = hoy; crece hacia atrás
    // Survives recreation (rotation / Settings round-trip): a date key, not a raw index, so a
    // shrunken window resolves through reclampDayIndex instead of crashing.
    var savedSelectedDate by rememberSaveable { mutableStateOf<String?>(null) }
    // The window the selection was made in, so a rebuilt window can keep the same day.
    var selectedWindow by remember { mutableStateOf<List<LocalDate>>(emptyList()) }
    var sessions by remember { mutableStateOf<List<UsageSession>?>(null) }
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
            if (date != null && date !in available && date in today.minusDays(6)..today) {
                (available + date).sortedDescending()
            } else {
                available
            }
        }

    // The window is rebuilt when today rolls over or the store changes, so the selection is
    // reclamped: the same day when it is offered, the nearest valid one otherwise.
    val effectiveIndex = reclampDayIndex(selectedIndex, selectedWindow, days, today)
    SideEffect {
        if (selectedIndex != effectiveIndex) selectedIndex = effectiveIndex
        selectedWindow = days
        days.getOrNull(effectiveIndex)?.toString()?.let { savedSelectedDate = it }
    }

    // The requested day is applied once, and only when the window can hold it: the stored days
    // arrive asynchronously, and applying the request to the first (today-alone) window would land
    // on the wrong day once the list grows. Afterwards the user's taps on the selector win.
    var requested by remember { mutableStateOf(date) }
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
        val idx = days.indexOf(saved).takeIf { it >= 0 }
            ?: days.minByOrNull { kotlin.math.abs(ChronoUnit.DAYS.between(it, saved)) }?.let { days.indexOf(it) }
            ?: 0
        if (selectedIndex != idx) selectedIndex = idx
    }

    val selectedDate = days.getOrNull(effectiveIndex) ?: today
    LaunchedEffect(packageName, selectedDate, storedDays, today, refreshKey) {
        sessions = null
        sessions =
            withContext(Dispatchers.IO) {
                // Today is never consolidated, so the live read answers for it and the store for the rest.
                // A failed read answers an empty day, which is the empty state the screen already has.
                readOr(emptyList<UsageSession>()) {
                    when (daySource(selectedDate, today, storedDays.orEmpty())) {
                        DaySource.Live -> repository.loadDay(selectedDate).sessions
                        DaySource.Stored -> store.sessionsFor(selectedDate)
                        DaySource.Absent -> emptyList()
                    }
                }
            }
    }

    val detail =
        remember(sessions, packageName) {
            sessions?.let { appDayDetail(it, packageName, ZoneId.systemDefault()) }
        }

    Scaffold { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .pointerInput(Unit) {
                        // Intercepted in the Initial pass so a horizontal swipe reaches the screen
                        // even when the touch starts on the scrollable list.
                        awaitPointerEventScope {
                            var startX = 0f
                            var startY = 0f
                            var tracking = false
                            var horizontal = false
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val change = event.changes.firstOrNull() ?: continue
                                when (event.type) {
                                    PointerEventType.Press -> {
                                        tracking = true
                                        horizontal = false
                                        startX = change.position.x
                                        startY = change.position.y
                                    }
                                    PointerEventType.Move -> {
                                        if (!tracking) continue
                                        val dx = change.position.x - startX
                                        val dy = change.position.y - startY
                                        if (!horizontal) {
                                            if (kotlin.math.abs(dx) > kotlin.math.abs(dy) && kotlin.math.abs(dx) > 24f) {
                                                horizontal = true
                                            } else if (kotlin.math.abs(dy) > kotlin.math.abs(dx) && kotlin.math.abs(dy) > 24f) {
                                                // Vertical scroll: the list keeps the gesture.
                                                tracking = false
                                            }
                                        }
                                        if (horizontal) {
                                            change.consume()
                                            if (dx > size.width * 0.3f) {
                                                backDispatcher?.onBackPressed()
                                                tracking = false
                                                horizontal = false
                                            }
                                        }
                                    }
                                    PointerEventType.Release, PointerEventType.Exit -> {
                                        tracking = false
                                        horizontal = false
                                    }
                                }
                            }
                        }
                    },
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
                    onClick = {
                        val ni = (effectiveIndex + 1).coerceAtMost(days.lastIndex)
                        selectedIndex = ni
                        days.getOrNull(ni)?.toString()?.let { savedSelectedDate = it }
                    },
                    enabled = effectiveIndex < days.lastIndex,
                ) {
                    Text("‹", style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    text =
                        formatDayLabel(
                            selectedDate,
                            today,
                            stringResource(R.string.day_today),
                            stringResource(R.string.day_yesterday),
                        ),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.width(150.dp),
                    textAlign = TextAlign.Center,
                )
                IconButton(
                    onClick = {
                        val ni = (effectiveIndex - 1).coerceAtLeast(0)
                        selectedIndex = ni
                        days.getOrNull(ni)?.toString()?.let { savedSelectedDate = it }
                    },
                    enabled = effectiveIndex > 0,
                ) {
                    Text("›", style = MaterialTheme.typography.titleLarge)
                }
            }

            Spacer(Modifier.height(8.dp))

            val current = detail
            when {
                sessions == null ->
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }

                current == null ->
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.detail_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                else ->
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .weight(1f),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onConfigure() }
                                .padding(vertical = 4.dp)
                                .testTag(LIMITS_PREVIEW_TAG),
                        ) {
                        // Preview: limit / window / alert for this app
                        Text(
                            text = stringResource(R.string.detail_limits_section),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(4.dp))
                        val none = stringResource(R.string.settings_limit_none)
                        val blockNow = stringResource(R.string.settings_limit_block_now)
                        val minutes = limits[packageName]
                        val window = windows[packageName]
                        val alertOn = alerts[packageName] == true
                        val preview =
                            listOf(
                                stringResource(R.string.detail_limits_limit_label) to previewLimitLabel(minutes, none, blockNow),
                                stringResource(R.string.settings_window_label) to
                                    (
                                        if (window ==
                                            null
                                        ) {
                                            stringResource(R.string.settings_window_none)
                                        } else {
                                            stringResource(
                                                R.string.settings_window_range,
                                                previewClockLabel(window.startMinute),
                                                previewClockLabel(window.endMinute),
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

private fun previewLimitLabel(
    minutes: Int?,
    none: String,
    blockNow: String,
): String {
    if (minutes == null) return none
    if (minutes == 0) return blockNow
    val h = minutes / 60
    val rest = minutes % 60
    return when {
        h > 0 && rest > 0 -> "$h h $rest min"
        h > 0 -> "$h h"
        else -> "$minutes min"
    }
}

private fun previewClockLabel(minute: Int): String = String.format(java.util.Locale.ROOT, "%02d:%02d", minute / 60, minute % 60)
