package com.foco.app.ui.dashboard

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.foco.app.R
import com.foco.app.data.monitor.UsageRepository
import com.foco.app.data.stores.SessionHistoryStore
import com.foco.app.data.system.MotionScale
import com.foco.app.domain.ring.DayStop
import com.foco.app.domain.ring.dayStopFromKey
import com.foco.app.domain.ring.key
import com.foco.app.model.day.DayData
import com.foco.app.ui.DetailTarget
import com.foco.app.ui.common.charts.WeekContent
import com.foco.app.ui.common.layout.LoadingState
import com.foco.app.ui.common.layout.ScreenHeader
import com.foco.app.ui.common.layout.StepSelectorRow
import com.foco.app.ui.common.ring.PositionRingContainer
import com.foco.app.ui.common.time.rememberAppLocale
import com.foco.app.ui.common.time.rememberDayLabel
import com.foco.app.ui.dashboard.export.WeekExportButton
import com.foco.app.ui.formatDuration
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

/** Interval between automatic refreshes while the live day is visible. */
const val DEFAULT_REFRESH_INTERVAL_MILLIS = 30_000L

/** The tag of the one loading placeholder: shown only while the home has no position yet. */
private const val RING_LOADING_TAG = "ring_loading"

/** The home: stateless screen observing [DashboardViewModel] and rendering position ring content. */
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
    viewModel: DashboardViewModel =
        remember(refreshIntervalMillis, loadDay, loadExport, clock) {
            DashboardViewModel(
                clock = clock,
                repositoryProvider = createRepository,
                historyStoreProvider = { ctx -> createHistoryStore(ctx) { day -> createRepository(ctx).loadDay(day) } },
                loadDayOverride = loadDay,
                loadExportOverride = loadExport,
                refreshIntervalMillis = refreshIntervalMillis,
            )
        },
) {
    val context = LocalContext.current
    LaunchedEffect(viewModel, context) {
        viewModel.bindContext(context)
    }

    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val today = state.today
    val shown = state.shown
    val shownKey = state.shownKey
    val slideDirection = state.slideDirection
    val softMotion = state.softMotion
    val ringContent = state.ringContent
    val excludedPackages = state.excludedPackages

    val locale = rememberAppLocale()
    val dayLabel = rememberDayLabel(today, locale)
    val motionScale = remember { MotionScale(context.applicationContext).scale() }

    val snackbarExcluded = stringResource(R.string.excluded_snackbar)
    val undoLabel = stringResource(R.string.excluded_undo)
    val exportError = stringResource(R.string.week_export_error)

    BackHandler(enabled = state.ringState.history.isNotEmpty()) {
        viewModel.goBack()
    }

    val onExclude: (String) -> Unit = { packageName ->
        scope.launch {
            viewModel.excludeApp(packageName)
            val result =
                snackbarHostState.showSnackbar(
                    message = snackbarExcluded,
                    actionLabel = undoLabel,
                )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.restoreApp(packageName)
            }
        }
    }

    val exportJson: suspend () -> String = {
        viewModel.exportWeekJson(context)
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
                    IconButton(onClick = { viewModel.moveTo(DayStop.Week) }) {
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
            StepSelectorRow(
                text = subtitle.orEmpty(),
                onPrevious = { viewModel.moveStep(forward = false) },
                onNext = { viewModel.moveStep(forward = true) },
                previousContentDescription = previous,
                nextContentDescription = next,
                useArrowIcons = true,
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            Spacer(Modifier.height(8.dp))

            PositionRingContainer(
                targetState = shownKey,
                slideDirection = slideDirection,
                softMotion = softMotion,
                motionScale = motionScale,
                onNext = { viewModel.moveStep(forward = true) },
                onPrevious = { viewModel.moveStep(forward = false) },
                label = "day-ring",
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 20.dp)
                        .testTag("dashboard_content"),
            ) { targetKey ->
                val todayKey = DayStop.Day(today).key()
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
                                onDayClick = { date -> viewModel.moveTo(DayStop.Day(date)) },
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
