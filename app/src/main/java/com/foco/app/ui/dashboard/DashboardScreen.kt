package com.foco.app.ui.dashboard

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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
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
import com.foco.app.data.ExcludedAppsStore
import com.foco.app.data.MotionScale
import com.foco.app.data.SessionHistoryStore
import com.foco.app.data.UsageRepository
import com.foco.app.domain.DayStop
import com.foco.app.domain.dayRing
import com.foco.app.domain.dayStopFromKey
import com.foco.app.domain.key
import com.foco.app.domain.step
import com.foco.app.model.day.DayData
import com.foco.app.ui.common.AppUsageRow
import com.foco.app.ui.common.HourChart
import com.foco.app.ui.common.ScreenHeader
import com.foco.app.ui.common.WeekContent
import com.foco.app.ui.common.readOr
import com.foco.app.ui.formatDayLabel
import com.foco.app.ui.formatDuration
import com.foco.app.ui.formatTimeOfDay
import com.foco.app.ui.metrics.DayMetrics
import com.foco.app.ui.metrics.deriveDayMetrics
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Interval between automatic refreshes while the live day is visible. */
const val DEFAULT_REFRESH_INTERVAL_MILLIS = 30_000L

/** The home: one screen whose day is a ring position; it changes in place and keeps its history. */
@Composable
fun DashboardScreen(
    refreshIntervalMillis: Long = DEFAULT_REFRESH_INTERVAL_MILLIS,
    loadDay: (suspend () -> DayData)? = null,
    onAppClick: (String, LocalDate?) -> Unit = { _, _ -> },
    onOpenSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember { UsageRepository(context.applicationContext) }
    val store = remember { SessionHistoryStore.forApp(context.applicationContext) { day -> repository.loadDay(day) } }
    val today = remember { LocalDate.now() }
    val load by rememberUpdatedState(loadDay ?: { repository.loadToday() })

    val excludedStore = remember { ExcludedAppsStore(context.applicationContext) }
    var excludedPackages by remember { mutableStateOf(emptySet<String>()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var storedDays by remember { mutableStateOf<Set<LocalDate>>(emptySet()) }
    var stopKey by rememberSaveable { mutableStateOf(DayStop.Day(today).key()) }
    var historyKeys by rememberSaveable { mutableStateOf("") }
    var slideDirection by remember { mutableIntStateOf(1) }
    var softMotion by remember { mutableStateOf(false) }
    val motionScale = remember { MotionScale(context.applicationContext).scale() }
    val ring = remember(today, storedDays) { dayRing(today, storedDays) }
    val stop = remember(stopKey, today) { dayStopFromKey(stopKey) ?: DayStop.Day(today) }
    val dayDate = (stop as? DayStop.Day)?.date

    var day by remember { mutableStateOf<DayData?>(null) }
    var storedUnlockCount by remember { mutableStateOf(0) }

    val snackbarExcluded = stringResource(R.string.excluded_snackbar)
    val undoLabel = stringResource(R.string.excluded_undo)

    fun history(): List<String> = historyKeys.split('\n').filter { it.isNotEmpty() }

    fun setHistory(keys: List<String>) {
        historyKeys = keys.joinToString("\n")
    }

    fun moveTo(target: DayStop, direction: Int? = null, soft: Boolean = false) {
        if (target.key() == stopKey || ring.none { it.key() == target.key() }) return
        val from = ring.indexOfFirst { it.key() == stopKey }
        val to = ring.indexOfFirst { it.key() == target.key() }
        slideDirection = direction ?: if (from >= 0 && to > from) 1 else -1
        softMotion = soft
        setHistory(history() + stopKey)
        stopKey = target.key()
    }

    fun moveStep(forward: Boolean) {
        moveTo(stop.step(ring, forward), direction = if (forward) 1 else -1, soft = true)
    }

    fun goBack() {
        val visited = history()
        if (visited.isEmpty()) return
        slideDirection = -1
        softMotion = false
        stopKey = visited.last()
        setHistory(visited.dropLast(1))
    }

    BackHandler(enabled = historyKeys.isNotEmpty()) { goBack() }

    LaunchedEffect(store) {
        store.storedDaysFlow().readOr(emptySet<LocalDate>()).collect { storedDays = it }
    }

    LaunchedEffect(ring) {
        if (ring.none { it.key() == stopKey }) {
            stopKey = DayStop.Day(today).key()
            setHistory(emptyList())
        } else {
            setHistory(history().filter { key -> ring.any { it.key() == key } })
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, refreshIntervalMillis, dayDate) {
        if (dayDate == null) {
            day = null
            return@LaunchedEffect
        }
        day = null
        if (dayDate == today) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    day = withContext(Dispatchers.IO) { load() }
                    delay(refreshIntervalMillis)
                }
            }
        } else {
            val stored = withContext(Dispatchers.IO) {
                readOr(DayData(emptyList(), emptyList())) {
                    DayData(store.sessionsFor(dayDate), emptyList())
                } to readOr(0) { store.unlocksFor(dayDate) ?: 0 }
            }
            day = stored.first
            storedUnlockCount = stored.second
        }
    }

    LaunchedEffect(excludedStore) {
        excludedStore.excludedPackages.collect { excludedPackages = it }
    }

    val onExclude: (String) -> Unit = { packageName ->
        scope.launch {
            excludedStore.exclude(packageName)
            val result = snackbarHostState.showSnackbar(
                message = snackbarExcluded,
                actionLabel = undoLabel
            )
            if (result == SnackbarResult.ActionPerformed) {
                excludedStore.restore(packageName)
            }
        }
    }

    val metrics = remember(day, excludedPackages) {
        day?.let { deriveDayMetrics(it.sessions, ZoneId.systemDefault(), excludedPackages) }
    }

    val swipeThreshold = with(LocalDensity.current) { 64.dp.toPx() }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Spacer(Modifier.height(16.dp))

            val title = when (stop) {
                DayStop.Week -> stringResource(R.string.week_title)
                is DayStop.Day -> if (stop.date == today) {
                    stringResource(R.string.dashboard_title)
                } else {
                    formatDayLabel(
                        stop.date,
                        today,
                        stringResource(R.string.day_today),
                        stringResource(R.string.day_yesterday)
                    )
                }
            }
            val subtitle = if (stop is DayStop.Day) {
                metrics?.let { m ->
                    val unlocks = if (stop.date != today) {
                        storedUnlockCount
                    } else {
                        day?.unlockTimes?.size ?: 0
                    }
                    stringResource(
                        R.string.dashboard_usage_line,
                        formatDuration(m.totalMillis),
                        pluralStringResource(R.plurals.dashboard_unlocks, unlocks, unlocks)
                    )
                } ?: stringResource(R.string.loading)
            } else {
                null
            }

            ScreenHeader(
                title = title,
                actions = {
                    IconButton(onClick = { moveTo(DayStop.Week) }) {
                        Icon(
                            imageVector = Icons.Filled.DateRange,
                            contentDescription = stringResource(R.string.week_title)
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.settings_title)
                        )
                    }
                }
            )

            val previous = stringResource(R.string.day_previous)
            val next = stringResource(R.string.day_next)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { moveStep(forward = false) },
                    modifier = Modifier.semantics { contentDescription = previous }
                ) {
                    Text("‹", style = MaterialTheme.typography.titleLarge)
                }
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                IconButton(
                    onClick = { moveStep(forward = true) },
                    modifier = Modifier.semantics { contentDescription = next }
                ) {
                    Text("›", style = MaterialTheme.typography.titleLarge)
                }
            }

            Spacer(Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp)
                    .testTag("dashboard_content")
                    .pointerInput(ring, stopKey) {
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
                            onDragCancel = { totalDrag = 0f }
                        )
                    }
            ) {
                AnimatedContent(
                    targetState = stopKey,
                    transitionSpec = {
                        val forward = slideDirection >= 0
                        when {
                            motionScale <= 0f ->
                                EnterTransition.None togetherWith ExitTransition.None

                            softMotion -> {
                                val duration = RingMotion.durationMillis(motionScale)
                                val slide = tween<IntOffset>(
                                    durationMillis = duration,
                                    easing = RingMotion.softEasing
                                )
                                val fade = tween<Float>(
                                    durationMillis = duration,
                                    easing = RingMotion.softEasing
                                )
                                val enter = slideInHorizontally(animationSpec = slide) { width ->
                                    if (forward) width / RingMotion.SOFT_SLIDE_DIVISOR
                                    else -width / RingMotion.SOFT_SLIDE_DIVISOR
                                } + fadeIn(animationSpec = fade)
                                val exit = slideOutHorizontally(animationSpec = slide) { width ->
                                    if (forward) -width / RingMotion.SOFT_SLIDE_DIVISOR
                                    else width / RingMotion.SOFT_SLIDE_DIVISOR
                                } + fadeOut(animationSpec = fade)
                                enter togetherWith exit
                            }

                            else -> {
                                val enter = slideInHorizontally { width ->
                                    if (forward) width / 4 else -width / 4
                                } + fadeIn()
                                val exit = slideOutHorizontally { width ->
                                    if (forward) -width / 4 else width / 4
                                } + fadeOut()
                                enter togetherWith exit
                            }
                        }
                    },
                    label = "day-ring",
                    modifier = Modifier.fillMaxSize()
                ) { targetKey ->
                    val targetStop = dayStopFromKey(targetKey) ?: DayStop.Day(today)
                    when (targetStop) {
                        DayStop.Week -> WeekContent(
                            today = today,
                            loadDay = loadDay,
                            onDayClick = { date -> moveTo(DayStop.Day(date)) },
                            onAppClick = { packageName -> onAppClick(packageName, null) },
                            modifier = Modifier.fillMaxSize()
                        )

                        is DayStop.Day -> DayDashboardContent(
                            metrics = metrics,
                            date = targetStop.date,
                            today = today,
                            onExclude = onExclude,
                            onAppClick = onAppClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayDashboardContent(
    metrics: DayMetrics?,
    date: LocalDate,
    today: LocalDate,
    onExclude: (String) -> Unit,
    onAppClick: (String, LocalDate?) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        when {
            metrics == null -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            metrics.entries.isEmpty() -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (date == today) {
                        stringResource(R.string.dashboard_empty)
                    } else {
                        stringResource(R.string.day_empty)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            else -> {
                HourChart(metrics.hourly)
                Spacer(Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.dashboard_apps),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(4.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(metrics.entries, key = { it.packageName }) { entry ->
                        AppUsageRow(
                            packageName = entry.packageName,
                            millis = entry.totalMillis,
                            maxMillis = metrics.totalMillis,
                            caption = stringResource(
                                R.string.row_last_used,
                                formatTimeOfDay(entry.lastUsedAt)
                            ),
                            onClick = { onAppClick(entry.packageName, date) },
                            onLongClick = { onExclude(entry.packageName) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }
        }
    }
}
