package com.foco.app.ui.dashboard

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.foco.app.R
import com.foco.app.data.db.DailyAggregate
import com.foco.app.data.export.WeekExportService
import com.foco.app.data.monitor.UsageRepository
import com.foco.app.data.stores.ExcludedAppsStore
import com.foco.app.data.stores.SessionHistoryStore
import com.foco.app.data.system.AppPackageInfo
import com.foco.app.data.system.MotionScale
import com.foco.app.domain.export.weekExportJson
import com.foco.app.domain.history.recentDays
import com.foco.app.domain.ring.DayStop
import com.foco.app.domain.ring.dayRing
import com.foco.app.domain.ring.dayStopFromKey
import com.foco.app.domain.ring.key
import com.foco.app.model.UsageSession
import com.foco.app.model.day.DayData
import com.foco.app.ui.DetailTarget
import com.foco.app.ui.common.apps.AppUsageRow
import com.foco.app.ui.common.charts.HourChart
import com.foco.app.ui.common.charts.WeekContent
import com.foco.app.ui.common.layout.EmptyState
import com.foco.app.ui.common.layout.LoadingState
import com.foco.app.ui.common.layout.ScreenHeader
import com.foco.app.ui.common.ring.PositionRingContainer
import com.foco.app.ui.common.time.rememberAppLocale
import com.foco.app.ui.common.time.rememberCurrentDate
import com.foco.app.ui.common.time.rememberDayLabel
import com.foco.app.ui.common.util.readOr
import com.foco.app.ui.dashboard.export.WeekExportButton
import com.foco.app.ui.formatDuration
import com.foco.app.ui.formatTimeOfDay
import com.foco.app.ui.metrics.day.DayMetrics
import com.foco.app.ui.metrics.day.deriveDayMetrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

/** Interval between automatic refreshes while the live day is visible. */
const val DEFAULT_REFRESH_INTERVAL_MILLIS = 30_000L

/** The tag of the one loading placeholder: shown only while the home has no position yet. */
private const val RING_LOADING_TAG = "ring_loading"

/** The home: one screen whose day is a ring position; it changes in place and keeps its history. */
@Composable
fun DashboardScreen(
    refreshIntervalMillis: Long = DEFAULT_REFRESH_INTERVAL_MILLIS,
    loadDay: (suspend () -> DayData)? = null,
    onAppClick: (DetailTarget) -> Unit = { },
    onOpenSettings: () -> Unit = {},
    loadExport: (suspend () -> String)? = null,
    clock: Clock = Clock.systemDefaultZone(),
    createRepository: (Context) -> UsageRepository = { UsageRepository.forApp(it) },
    createHistoryStore: (Context, (LocalDate) -> DayData) -> SessionHistoryStore =
        { appContext, readDay -> SessionHistoryStore.forApp(appContext, readDay) },
) {
    val context = LocalContext.current
    val repository =
        remember(context.applicationContext) {
            createRepository(context.applicationContext)
        }
    val store =
        remember(context.applicationContext, repository) {
            createHistoryStore(context.applicationContext) { day -> repository.loadDay(day) }
        }
    val today = rememberCurrentDate(clock)
    val load by rememberUpdatedState(loadDay ?: { repository.loadDay(today) })

    val excludedStore = remember { ExcludedAppsStore(context.applicationContext) }
    var excludedPackages by remember { mutableStateOf(emptySet<String>()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // The seven-day window of the home, read once: the shortcut days live in it.
    val recentWindow = remember(today) { recentDays(today) }
    // The locale and the day labels are read once for the screen, not once per label.
    val locale = rememberAppLocale()
    val dayLabel = rememberDayLabel(today, locale)

    var storedDays by remember { mutableStateOf<Set<LocalDate>>(emptySet()) }
    val motionScale = remember { MotionScale(context.applicationContext).scale() }
    val ring = remember(today, storedDays) { dayRing(today, storedDays) }
    // Two positions: the arrows, bars, history and back move the requested one; the header and the
    // content follow the shown one, which advances only once that content is in memory.
    val dashboardState =
        rememberSaveable(saver = dashboardStateSaver(DayStop.Day(today))) {
            DashboardState(DayStop.Day(today))
        }
    val state by dashboardState.snapshot.collectAsState()
    val requested = state.ring.current
    val requestedKey = requested.key()
    val requestedDate = (requested as? DayStop.Day)?.date
    val shown = state.shown
    val shownKey = shown.key()
    val slideDirection = state.slideDirection
    val softMotion = state.softMotion
    val todayKey = remember(today) { DayStop.Day(today).key() }
    val weekKey = DayStop.Week.key()
    val ringContent = rememberSaveable(saver = ringContentSaver) { mutableStateMapOf<String, RingContent>() }
    var weekTotalsDay by remember { mutableStateOf<LocalDate?>(null) }

    val snackbarExcluded = stringResource(R.string.excluded_snackbar)
    val undoLabel = stringResource(R.string.excluded_undo)
    val exportError = stringResource(R.string.week_export_error)

    fun moveTo(
        target: DayStop,
        direction: Int? = null,
        soft: Boolean = false,
    ) {
        val shortcutDay = target is DayStop.Day && target.date in recentWindow
        dashboardState.moveTo(target, ring, shortcutDay, direction, soft)
    }

    fun moveStep(forward: Boolean) {
        dashboardState.moveStep(ring, forward)
    }

    fun goBack() {
        dashboardState.goBack()
    }

    BackHandler(enabled = state.ring.history.isNotEmpty()) { goBack() }

    LaunchedEffect(store) {
        store.storedDaysFlow().readOr(emptySet<LocalDate>()).collect { storedDays = it }
    }

    LaunchedEffect(ring, requestedKey, today) {
        fun insideWindow(dayStop: DayStop): Boolean = dayStop is DayStop.Day && dayStop.date in recentWindow

        dashboardState.retain(ring, ::insideWindow, today)
    }

    // The live read follows the requested position: today or the week keep it, a stored day stops it.
    val weekRequested = requested == DayStop.Week
    val liveRequested = weekRequested || requestedDate == today
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, refreshIntervalMillis, todayKey, liveRequested) {
        if (!liveRequested) return@LaunchedEffect
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                try {
                    // The read and the day's metrics both happen off the main thread.
                    val content =
                        withContext(Dispatchers.IO) {
                            val data = load()
                            RingContent.Day(
                                data = data,
                                unlocks = data.unlockTimes.size,
                                metrics = deriveDayMetrics(data.sessions, ZoneId.systemDefault()),
                            )
                        }
                    ringContent[todayKey] = content
                } catch (failure: Exception) {
                    android.util.Log.w("DashboardScreen", "Periodic load failed, keeping loop", failure)
                }
                delay(refreshIntervalMillis)
            }
        }
    }

    LaunchedEffect(store, requestedKey, today) {
        val target = dayStopFromKey(requestedKey) ?: return@LaunchedEffect
        val date = (target as? DayStop.Day)?.date ?: return@LaunchedEffect
        if (date == today) return@LaunchedEffect
        if (ringContent.containsKey(target.key())) return@LaunchedEffect
        val stored =
            withContext(Dispatchers.IO) {
                val sessions = readOr(emptyList<UsageSession>()) { store.sessionsFor(date) }
                val unlocks = readOr<Int?>(null) { store.unlocksFor(date) }
                RingContent.Day(
                    data = DayData(sessions, emptyList()),
                    unlocks = unlocks,
                    metrics = deriveDayMetrics(sessions, ZoneId.systemDefault()),
                )
            }
        ringContent[target.key()] = stored
    }

    LaunchedEffect(store, today, weekRequested) {
        if (!weekRequested) return@LaunchedEffect
        if (weekTotalsDay == today && ringContent[weekKey] is RingContent.Week) return@LaunchedEffect
        val totals =
            withContext(Dispatchers.IO) {
                readOr(emptyList<DailyAggregate>()) { store.week(today) }
            }
        ringContent[weekKey] = RingContent.Week(totals)
        weekTotalsDay = today
    }

    // Bounded by the ring; the two positions on screen keep their entry for the exit side.
    LaunchedEffect(ring, today) {
        val keep = ring.mapTo(mutableSetOf<String>()) { it.key() } + todayKey + requestedKey + shownKey
        ringContent.keys
            .toList()
            .filter { it !in keep }
            .forEach { ringContent.remove(it) }
    }

    val requestedReady =
        ringContent.containsKey(requestedKey) &&
            (!weekRequested || ringContent[todayKey] is RingContent.Day)
    LaunchedEffect(requestedKey, requestedReady) {
        if (requestedReady) dashboardState.showCurrent()
    }

    LaunchedEffect(excludedStore) {
        excludedStore.excludedPackages.collect { excludedPackages = it }
    }

    val onExclude: (String) -> Unit = { packageName ->
        scope.launch {
            excludedStore.exclude(packageName)
            val result =
                snackbarHostState.showSnackbar(
                    message = snackbarExcluded,
                    actionLabel = undoLabel,
                )
            if (result == SnackbarResult.ActionPerformed) {
                excludedStore.restore(packageName)
            }
        }
    }

    // The export seam: the tests drive the success and failure paths without the platform.
    val exportJson: suspend () -> String =
        loadExport ?: {
            val service =
                WeekExportService(
                    context = context.applicationContext,
                    labels = AppPackageInfo(context.applicationContext)::label,
                )
            weekExportJson(service.export(today))
        }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            Spacer(Modifier.height(16.dp))

            val shownDay = ringContent[shownKey] as? RingContent.Day
            val shownMetrics = shownDay?.let { day -> metricsOf(day, excludedPackages) }

            val title =
                when (shown) {
                    DayStop.Week -> stringResource(R.string.week_title)
                    is DayStop.Day ->
                        if (shown.date == today) {
                            stringResource(R.string.dashboard_title)
                        } else {
                            dayLabel(shown.date)
                        }
                }
            val subtitle =
                if (shownDay != null && shownMetrics != null) {
                    val unlocksPart =
                        shownDay.unlocks?.let { count ->
                            pluralStringResource(R.plurals.dashboard_unlocks, count, count)
                        }
                    if (unlocksPart != null) {
                        stringResource(
                            R.string.dashboard_usage_line,
                            formatDuration(shownMetrics.totalMillis),
                            unlocksPart,
                        )
                    } else {
                        stringResource(
                            R.string.dashboard_usage_line_no_unlocks,
                            formatDuration(shownMetrics.totalMillis),
                        )
                    }
                } else {
                    null
                }

            ScreenHeader(
                title = title,
                actions = {
                    if (shown == DayStop.Week) {
                        WeekExportButton(
                            exportJson = exportJson,
                            weekEnd = today,
                            onFailed = { snackbarHostState.showSnackbar(exportError) },
                        )
                    }
                    IconButton(onClick = { moveTo(DayStop.Week) }) {
                        Icon(
                            imageVector = Icons.Filled.DateRange,
                            contentDescription = stringResource(R.string.week_title),
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.settings_title),
                        )
                    }
                },
            )

            val previous = stringResource(R.string.day_previous)
            val next = stringResource(R.string.day_next)
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { moveStep(forward = false) },
                    modifier = Modifier.semantics { contentDescription = previous },
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = null,
                    )
                }
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                IconButton(
                    onClick = { moveStep(forward = true) },
                    modifier = Modifier.semantics { contentDescription = next },
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            PositionRingContainer(
                targetState = shownKey,
                slideDirection = slideDirection,
                softMotion = softMotion,
                motionScale = motionScale,
                onNext = { moveStep(forward = true) },
                onPrevious = { moveStep(forward = false) },
                label = "day-ring",
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 20.dp)
                        .testTag("dashboard_content"),
            ) { targetKey ->
                when (val target = dayStopFromKey(targetKey) ?: DayStop.Day(today)) {
                    DayStop.Week -> {
                        val week = ringContent[targetKey] as? RingContent.Week
                        val live = ringContent[todayKey] as? RingContent.Day
                        if (week == null || live == null) {
                            RingLoading()
                        } else {
                            WeekContent(
                                today = today,
                                totals = week.totals,
                                todayData = live.data,
                                excludedPackages = excludedPackages,
                                onDayClick = { date -> moveTo(DayStop.Day(date)) },
                                onAppClick = { packageName -> onAppClick(DetailTarget(packageName)) },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }

                    is DayStop.Day -> {
                        val dayContent = ringContent[targetKey] as? RingContent.Day
                        if (dayContent == null) {
                            RingLoading()
                        } else {
                            DayDashboardContent(
                                metrics = metricsOf(dayContent, excludedPackages),
                                date = target.date,
                                today = today,
                                onExclude = onExclude,
                                onAppClick = onAppClick,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayDashboardContent(
    metrics: DayMetrics,
    date: LocalDate,
    today: LocalDate,
    onExclude: (String) -> Unit,
    onAppClick: (DetailTarget) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (metrics.entries.isEmpty()) {
            EmptyState(
                text =
                    if (date == today) {
                        stringResource(R.string.dashboard_empty)
                    } else {
                        stringResource(R.string.day_empty)
                    },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
            )
        } else {
            HourChart(metrics.hourly)
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.dashboard_apps),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(4.dp))
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
            ) {
                items(metrics.entries, key = { it.packageName }) { entry ->
                    AppUsageRow(
                        packageName = entry.packageName,
                        millis = entry.totalMillis,
                        barReferenceMillis = metrics.totalMillis,
                        caption =
                            stringResource(
                                R.string.row_last_used,
                                formatTimeOfDay(entry.lastUsedAt),
                            ),
                        onClick = { onAppClick(DetailTarget(entry.packageName, date)) },
                        onLongClick = { onExclude(entry.packageName) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
            }
        }
    }
}

/** The only loading placeholder: no position on screen has content yet (cold start). */
@Composable
private fun RingLoading() {
    LoadingState(
        modifier =
            Modifier
                .fillMaxSize()
                .testTag(RING_LOADING_TAG),
    )
}

/** The day's metrics without the excluded apps: the exclusion only removes rows. */
@Composable
private fun metricsOf(
    day: RingContent.Day,
    excludedPackages: Set<String>,
): DayMetrics =
    remember(day, excludedPackages) {
        day.metrics.copy(entries = day.metrics.entries.filter { it.packageName !in excludedPackages })
    }

private fun dashboardStateSaver(fallback: DayStop): Saver<DashboardState, Any> =
    listSaver<DashboardState, String>(
        save = { state ->
            val snapshot = state.value
            listOf(
                snapshot.ring.current.key(),
                snapshot.shown.key(),
                snapshot.slideDirection.toString(),
                snapshot.softMotion.toString(),
            ) + snapshot.ring.history.map { it.key() }
        },
        restore = { values ->
            val current = dayStopFromKey(values.getOrNull(0).orEmpty()) ?: fallback
            val shown = dayStopFromKey(values.getOrNull(1).orEmpty()) ?: current
            DashboardState.restore(
                current = current,
                shown = shown,
                history = values.drop(4).mapNotNull(::dayStopFromKey),
                slideDirection = values.getOrNull(2)?.toIntOrNull() ?: 1,
                softMotion = values.getOrNull(3)?.toBooleanStrictOrNull() ?: false,
            )
        },
    )
