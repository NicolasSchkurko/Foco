package com.foco.app.ui.week

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.foco.app.R
import com.foco.app.data.ExcludedAppsStore
import com.foco.app.data.SessionHistoryStore
import com.foco.app.data.UsageRepository
import com.foco.app.data.db.DailyAggregate
import com.foco.app.model.day.DayData
import com.foco.app.ui.common.AppUsageRow
import com.foco.app.ui.common.readOr
import com.foco.app.ui.formatDayLabel
import com.foco.app.ui.formatDuration
import com.foco.app.ui.formatWeekday
import com.foco.app.ui.metrics.WeekDayBar
import com.foco.app.ui.metrics.deriveWeekMetrics
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The last seven days: a bar per day (today live, the stored days from the store, a day with
 * nothing read as absent) plus the apps ranked by their total over the window. `loadDay` is a seam.
 */
@Composable
fun WeekScreen(
    loadDay: (suspend () -> DayData)? = null,
    onDayClick: (LocalDate) -> Unit = {},
    onAppClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val repository = remember { UsageRepository(context.applicationContext) }
    val store = remember { SessionHistoryStore.forApp(context.applicationContext) { date -> repository.loadDay(date) } }
    val today = remember { LocalDate.now() }
    val load by rememberUpdatedState(loadDay ?: { repository.loadToday() })

    val excludedStore = remember { ExcludedAppsStore(context.applicationContext) }
    var excludedPackages by remember { mutableStateOf(emptySet<String>()) }
    var storedTotals by remember { mutableStateOf<List<DailyAggregate>?>(null) }
    var todayData by remember { mutableStateOf<DayData?>(null) }

    // The six stored days do not change while the screen is open, so the store is read once.
    LaunchedEffect(store) {
        storedTotals = withContext(Dispatchers.IO) {
            readOr(emptyList<DailyAggregate>()) { store.week(today) }
        }
    }

    // Only the live day changes: one read per resume, no loop and no refresh interval here.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            todayData = withContext(Dispatchers.IO) {
                readOr(DayData(emptyList(), emptyList())) { load() }
            }
        }
    }

    LaunchedEffect(excludedStore) {
        excludedStore.excludedPackages.collect { excludedPackages = it }
    }

    val metrics = remember(storedTotals, todayData, excludedPackages) {
        deriveWeekMetrics(
            storedTotals = storedTotals.orEmpty(),
            todayDate = today,
            today = todayData ?: DayData(emptyList(), emptyList()),
            excludedPackages = excludedPackages
        )
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { backDispatcher?.onBackPressed() },
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.content_back)
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.week_title),
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            Spacer(Modifier.height(24.dp))

            if (storedTotals == null || todayData == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                WeekBars(bars = metrics.bars, today = today, onDayClick = onDayClick)
                Spacer(Modifier.height(24.dp))

                if (metrics.ranking.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.week_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.dashboard_apps),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    val maxAppMillis = metrics.ranking.maxOf { it.totalMillis }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(metrics.ranking, key = { it.packageName }) { app ->
                            AppUsageRow(
                                packageName = app.packageName,
                                millis = app.totalMillis,
                                maxMillis = maxAppMillis,
                                caption = pluralStringResource(
                                    R.plurals.week_days_used,
                                    app.daysUsed,
                                    app.daysUsed
                                ),
                                onClick = { onAppClick(app.packageName) }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

/** The seven bars in one row, oldest first; the tallest one fills the track. */
@Composable
private fun WeekBars(bars: List<WeekDayBar>, today: LocalDate, onDayClick: (LocalDate) -> Unit) {
    val maxMillis = bars
        .filter { it.state != WeekDayBar.State.Absent }
        .maxOfOrNull { it.totalMillis }
        ?: 0L

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        bars.forEach { bar ->
            WeekBar(
                bar = bar,
                maxMillis = maxMillis,
                today = today,
                onClick = { onDayClick(bar.date) }
            )
        }
    }
}

/**
 * One day: its track with the bar bottom-aligned (a short marker when the day is absent, so the
 * gap never reads as a zero) and its weekday and day of month below. One tappable, readable node.
 */
@Composable
private fun WeekBar(bar: WeekDayBar, maxMillis: Long, today: LocalDate, onClick: () -> Unit) {
    val dayLabel = formatDayLabel(
        bar.date,
        today,
        stringResource(R.string.day_today),
        stringResource(R.string.day_yesterday)
    )
    val description = if (bar.state == WeekDayBar.State.Absent) {
        stringResource(R.string.week_bar_absent, dayLabel)
    } else {
        stringResource(R.string.week_bar, dayLabel, formatDuration(bar.totalMillis))
    }
    val absent = bar.state == WeekDayBar.State.Absent
    val fraction = if (maxMillis > 0) bar.totalMillis.toFloat() / maxMillis else 0f

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(120.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (absent) 4.dp else maxOf(4.dp, 120.dp * fraction))
                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                    .background(
                        if (absent) MaterialTheme.colorScheme.surfaceVariant
                        else MaterialTheme.colorScheme.primary
                    )
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = formatWeekday(bar.date),
            style = MaterialTheme.typography.labelSmall
        )
        Text(
            text = bar.date.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
