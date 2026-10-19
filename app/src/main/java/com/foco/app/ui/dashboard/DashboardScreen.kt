package com.foco.app.ui.dashboard

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.foco.app.R
import com.foco.app.data.ExcludedAppsStore
import com.foco.app.data.SessionHistoryStore
import com.foco.app.data.UsageRepository
import com.foco.app.model.day.DayData
import com.foco.app.ui.common.AppUsageRow
import com.foco.app.ui.common.HourChart
import com.foco.app.ui.common.readOr
import com.foco.app.ui.formatDayLabel
import com.foco.app.ui.formatDuration
import com.foco.app.ui.formatTimeOfDay
import com.foco.app.ui.metrics.deriveDayMetrics
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Interval between automatic refreshes while the dashboard is visible. */
const val DEFAULT_REFRESH_INTERVAL_MILLIS = 30_000L

/**
 * The day's dashboard: total, hours, ranking and unlocks, derived in `ui/metrics/`, refreshed while
 * visible; [date] shows that day read-only, with no refresh. `loadDay` and the interval are seams.
 */
@Composable
fun DashboardScreen(
    refreshIntervalMillis: Long = DEFAULT_REFRESH_INTERVAL_MILLIS,
    loadDay: (suspend () -> DayData)? = null,
    onAppClick: (String) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    date: LocalDate? = null,
    onOpenWeek: () -> Unit = {}
) {
    val context = LocalContext.current
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val repository = remember { UsageRepository(context.applicationContext) }
    val store = remember { SessionHistoryStore.forApp(context.applicationContext) { day -> repository.loadDay(day) } }
    val today = remember { LocalDate.now() }
    val requestedDate = date
    val load by rememberUpdatedState(loadDay ?: { repository.loadToday() })

    val excludedStore = remember { ExcludedAppsStore(context.applicationContext) }
    var excludedPackages by remember { mutableStateOf(emptySet<String>()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var day by remember { mutableStateOf<DayData?>(null) }
    var storedUnlockCount by remember { mutableStateOf(0) }

    val snackbarExcluded = stringResource(R.string.excluded_snackbar)
    val undoLabel = stringResource(R.string.excluded_undo)

    // repeatOnLifecycle: the loop runs only while the screen is RESUMED —
    // it restarts on returning to the foreground (an automatic refresh each time)
    // and it is cancelled on pause (no work in the background).
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, refreshIntervalMillis, requestedDate) {
        when {
            requestedDate == null -> lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    day = withContext(Dispatchers.IO) { load() }
                    delay(refreshIntervalMillis)
                }
            }

            // A dated dashboard is read once: today live, any other day from the store.
            requestedDate == today -> day = withContext(Dispatchers.IO) {
                readOr(DayData(emptyList(), emptyList())) { load() }
            }

            else -> {
                val stored = withContext(Dispatchers.IO) {
                    readOr(DayData(emptyList(), emptyList())) {
                        DayData(store.sessionsFor(requestedDate), emptyList())
                    } to readOr(0) { store.unlocksFor(requestedDate) ?: 0 }
                }
                day = stored.first
                storedUnlockCount = stored.second
            }
        }
    }

    // The user's exclusions (persisted): they only affect the ranking.
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

    // Memoised derivation: it recomputes when the data or the exclusions change.
    val metrics = remember(day, excludedPackages) {
        day?.let { deriveDayMetrics(it.sessions, ZoneId.systemDefault(), excludedPackages) }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (requestedDate != null) {
                    IconButton(
                        onClick = { backDispatcher?.onBackPressed() },
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_back)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = requestedDate?.let { dated ->
                            formatDayLabel(
                                dated,
                                today,
                                stringResource(R.string.day_today),
                                stringResource(R.string.day_yesterday)
                            )
                        } ?: stringResource(R.string.dashboard_title),
                        style = MaterialTheme.typography.headlineMedium
                    )
                    val currentDay = day
                    Text(
                        text = metrics?.let { m ->
                            val unlocks = if (requestedDate != null && requestedDate != today) {
                                storedUnlockCount
                            } else {
                                currentDay?.unlockTimes?.size ?: 0
                            }
                            stringResource(
                                R.string.dashboard_usage_line,
                                formatDuration(m.totalMillis),
                                pluralStringResource(R.plurals.dashboard_unlocks, unlocks, unlocks)
                            )
                        } ?: stringResource(R.string.loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (requestedDate == null) {
                    IconButton(onClick = onOpenWeek) {
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
            }

            Spacer(Modifier.height(20.dp))

            val current = metrics
            when {
                current == null -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }

                current.entries.isEmpty() -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = requestedDate?.let { stringResource(R.string.day_empty) }
                            ?: stringResource(R.string.dashboard_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> {
                    HourChart(current.hourly)
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
                        items(current.entries, key = { it.packageName }) { entry ->
                            AppUsageRow(
                                packageName = entry.packageName,
                                millis = entry.totalMillis,
                                maxMillis = current.totalMillis,
                                caption = stringResource(
                                    R.string.row_last_used,
                                    formatTimeOfDay(entry.lastUsedAt)
                                ),
                                onClick = { onAppClick(entry.packageName) },
                                onLongClick = { onExclude(entry.packageName) }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
