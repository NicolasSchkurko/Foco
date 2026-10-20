package com.foco.app.ui.dashboard

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.foco.app.R
import com.foco.app.data.AppPackageInfo
import com.foco.app.data.ExcludedAppsStore
import com.foco.app.data.MotionScale
import com.foco.app.data.SessionHistoryStore
import com.foco.app.data.UsageRepository
import com.foco.app.data.WeekExportService
import com.foco.app.data.db.DailyAggregate
import com.foco.app.domain.DayStop
import com.foco.app.domain.dayRing
import com.foco.app.domain.dayStopFromKey
import com.foco.app.domain.key
import com.foco.app.domain.weekExportJson
import com.foco.app.model.UsageSession
import com.foco.app.model.day.DayData
import com.foco.app.ui.common.AppUsageRow
import com.foco.app.ui.common.HourChart
import com.foco.app.ui.common.ScreenHeader
import com.foco.app.ui.common.WeekContent
import com.foco.app.ui.common.readOr
import com.foco.app.ui.common.rememberCurrentDate
import com.foco.app.ui.formatDayLabel
import com.foco.app.ui.formatDuration
import com.foco.app.ui.formatTimeOfDay
import com.foco.app.ui.metrics.DayMetrics
import com.foco.app.ui.metrics.deriveDayMetrics
import kotlinx.coroutines.CancellationException
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
    onAppClick: (String, LocalDate?) -> Unit = { _, _ -> },
    onOpenSettings: () -> Unit = {},
    loadExport: (suspend () -> String)? = null,
    clock: Clock = Clock.systemDefaultZone(),
    createRepository: (Context) -> UsageRepository = { UsageRepository(it) },
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
    val ringContent = remember { mutableStateMapOf<String, RingContent>() }
    var weekTotalsDay by remember { mutableStateOf<LocalDate?>(null) }

    val snackbarExcluded = stringResource(R.string.excluded_snackbar)
    val undoLabel = stringResource(R.string.excluded_undo)
    val exportError = stringResource(R.string.week_export_error)

    fun moveTo(
        target: DayStop,
        direction: Int? = null,
        soft: Boolean = false,
    ) {
        val shortcutDay = target is DayStop.Day && target.date in today.minusDays(6)..today
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
        fun insideWindow(dayStop: DayStop): Boolean = dayStop is DayStop.Day && dayStop.date in today.minusDays(6)..today

        dashboardState.retain(ring, ::insideWindow, DayStop.Day(today))
    }

    // The live read follows the requested position: today or the week keep it, a stored day stops it.
    val weekRequested = requested == DayStop.Week
    val liveRequested = weekRequested || requestedDate == today
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, refreshIntervalMillis, todayKey, liveRequested) {
        if (!liveRequested) return@LaunchedEffect
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                val data = withContext(Dispatchers.IO) { load() }
                ringContent[todayKey] = RingContent.Day(data, data.unlockTimes.size)
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
                val unlocks = readOr(0) { store.unlocksFor(date) ?: 0 }
                RingContent.Day(DayData(sessions, emptyList()), unlocks)
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

    val swipeThreshold = with(LocalDensity.current) { 64.dp.toPx() }

    var exporting by remember { mutableStateOf(false) }

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
            val shownMetrics =
                remember(shownDay, excludedPackages) {
                    shownDay?.let {
                        deriveDayMetrics(it.data.sessions, ZoneId.systemDefault(), excludedPackages)
                    }
                }

            val title =
                when (shown) {
                    DayStop.Week -> stringResource(R.string.week_title)
                    is DayStop.Day ->
                        if (shown.date == today) {
                            stringResource(R.string.dashboard_title)
                        } else {
                            formatDayLabel(
                                shown.date,
                                today,
                                stringResource(R.string.day_today),
                                stringResource(R.string.day_yesterday),
                            )
                        }
                }
            val subtitle =
                if (shownDay != null && shownMetrics != null) {
                    stringResource(
                        R.string.dashboard_usage_line,
                        formatDuration(shownMetrics.totalMillis),
                        pluralStringResource(
                            R.plurals.dashboard_unlocks,
                            shownDay.unlocks,
                            shownDay.unlocks,
                        ),
                    )
                } else {
                    null
                }

            ScreenHeader(
                title = title,
                actions = {
                    if (shown == DayStop.Week) {
                        val exportLabel = stringResource(R.string.week_export)
                        IconButton(
                            enabled = !exporting,
                            onClick = {
                                exporting = true
                                scope.launch {
                                    try {
                                        val json = withContext(Dispatchers.IO) { exportJson() }
                                        val uri = writeWeekExport(context, json, today)
                                        context.startActivity(
                                            Intent.createChooser(weekExportShareIntent(uri), null),
                                        )
                                    } catch (cancelled: CancellationException) {
                                        throw cancelled
                                    } catch (failure: Exception) {
                                        snackbarHostState.showSnackbar(exportError)
                                    } finally {
                                        exporting = false
                                    }
                                }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = exportLabel,
                            )
                        }
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

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 20.dp)
                        .testTag("dashboard_content")
                        .pointerInput(ring, requestedKey) {
                            var totalDrag = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { totalDrag = 0f },
                                onHorizontalDrag = { change, amount ->
                                    change.consume()
                                    totalDrag += amount
                                },
                                onDragEnd = {
                                    when {
                                        totalDrag <= -swipeThreshold -> moveStep(forward = true)
                                        totalDrag >= swipeThreshold -> moveStep(forward = false)
                                    }
                                    totalDrag = 0f
                                },
                                onDragCancel = { totalDrag = 0f },
                            )
                        },
            ) {
                AnimatedContent(
                    targetState = shownKey,
                    transitionSpec = {
                        val forward = slideDirection >= 0
                        when {
                            motionScale <= 0f ->
                                EnterTransition.None togetherWith ExitTransition.None

                            softMotion -> {
                                val duration = RingMotion.durationMillis(motionScale)
                                val slide =
                                    tween<IntOffset>(
                                        durationMillis = duration,
                                        easing = RingMotion.softEasing,
                                    )
                                val fade =
                                    tween<Float>(
                                        durationMillis = duration,
                                        easing = RingMotion.softEasing,
                                    )
                                val enter =
                                    slideInHorizontally(animationSpec = slide) { width ->
                                        if (forward) {
                                            width / RingMotion.SOFT_SLIDE_DIVISOR
                                        } else {
                                            -width / RingMotion.SOFT_SLIDE_DIVISOR
                                        }
                                    } + fadeIn(animationSpec = fade)
                                val exit =
                                    slideOutHorizontally(animationSpec = slide) { width ->
                                        if (forward) {
                                            -width / RingMotion.SOFT_SLIDE_DIVISOR
                                        } else {
                                            width / RingMotion.SOFT_SLIDE_DIVISOR
                                        }
                                    } + fadeOut(animationSpec = fade)
                                enter togetherWith exit
                            }

                            else -> {
                                val enter =
                                    slideInHorizontally { width ->
                                        if (forward) width / 4 else -width / 4
                                    } + fadeIn()
                                val exit =
                                    slideOutHorizontally { width ->
                                        if (forward) -width / 4 else width / 4
                                    } + fadeOut()
                                enter togetherWith exit
                            }
                        }
                    },
                    label = "day-ring",
                    modifier = Modifier.fillMaxSize(),
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
                                    onAppClick = { packageName -> onAppClick(packageName, null) },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }

                        is DayStop.Day -> {
                            val dayContent = ringContent[targetKey] as? RingContent.Day
                            if (dayContent == null) {
                                RingLoading()
                            } else {
                                val targetMetrics =
                                    remember(dayContent, excludedPackages) {
                                        deriveDayMetrics(
                                            dayContent.data.sessions,
                                            ZoneId.systemDefault(),
                                            excludedPackages,
                                        )
                                    }
                                DayDashboardContent(
                                    metrics = targetMetrics,
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
}

@Composable
private fun DayDashboardContent(
    metrics: DayMetrics,
    date: LocalDate,
    today: LocalDate,
    onExclude: (String) -> Unit,
    onAppClick: (String, LocalDate?) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (metrics.entries.isEmpty()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text =
                        if (date == today) {
                            stringResource(R.string.dashboard_empty)
                        } else {
                            stringResource(R.string.day_empty)
                        },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
                        maxMillis = metrics.totalMillis,
                        caption =
                            stringResource(
                                R.string.row_last_used,
                                formatTimeOfDay(entry.lastUsedAt),
                            ),
                        onClick = { onAppClick(entry.packageName, date) },
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
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .testTag(RING_LOADING_TAG),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
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
