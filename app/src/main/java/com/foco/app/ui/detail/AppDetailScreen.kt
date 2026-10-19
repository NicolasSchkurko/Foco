package com.foco.app.ui.detail

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.foco.app.R
import com.foco.app.data.SessionHistoryStore
import com.foco.app.data.UsageRepository
import com.foco.app.domain.DaySource
import com.foco.app.domain.availableDays
import com.foco.app.domain.daySource
import com.foco.app.model.UsageSession
import com.foco.app.ui.common.AppIcon
import com.foco.app.ui.common.HourChart
import com.foco.app.ui.common.appLabel
import com.foco.app.ui.common.readOr
import com.foco.app.ui.formatDayLabel
import com.foco.app.ui.formatDuration
import com.foco.app.ui.formatTimeOfDay
import com.foco.app.ui.metrics.appDayDetail
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext

/**
 * One app's detail: hourly breakdown, the day's summary and the session list, with a ‹ day ›
 * selector that walks back from today to the oldest day the store has; [date] opens on that day.
 */
@Composable
fun AppDetailScreen(packageName: String, date: LocalDate? = null) {
    val context = LocalContext.current
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val repository = remember { UsageRepository(context.applicationContext) }
    val store = remember { SessionHistoryStore.forApp(context.applicationContext) { date -> repository.loadDay(date) } }
    val today = remember { LocalDate.now() }
    var storedDays by remember { mutableStateOf<Set<LocalDate>?>(null) }
    var selectedIndex by remember { mutableIntStateOf(0) } // 0 = hoy; crece hacia atrás
    var sessions by remember { mutableStateOf<List<UsageSession>?>(null) }

    // Which days the store has grows while the screen is open (the startup backfill writes totals),
    // so the window follows the store's stream instead of being re-read on resume. Until the first
    // emission — and if the stream fails — it is today alone, which keeps the screen usable.
    LaunchedEffect(store) {
        store.storedDaysFlow().readOr(emptySet<LocalDate>()).collect { storedDays = it }
    }

    val days = remember(today, storedDays, date) {
        val available = availableDays(today, storedDays.orEmpty())
        // Any bar the week offers opens its day, even one the store has nothing for.
        if (date != null && date !in available && date in today.minusDays(6)..today) {
            (available + date).sortedDescending()
        } else {
            available
        }
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
                requested = null
            }
        }
    }

    val selectedDate = days[selectedIndex]
    LaunchedEffect(packageName, selectedDate, storedDays) {
        sessions = null
        sessions = withContext(Dispatchers.IO) {
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

    val detail = remember(sessions, packageName) {
        sessions?.let { appDayDetail(it, packageName, ZoneId.systemDefault()) }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Spacer(Modifier.height(16.dp))

            // Header: the arrow at the leading edge (4 dp inset) and the app identity centred.
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = { backDispatcher?.onBackPressed() },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.content_back)
                    )
                }
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIcon(packageName = packageName)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = appLabel(packageName),
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1
                        )
                        Text(
                            text = detail?.let { stringResource(R.string.detail_day_usage, formatDuration(it.totalMillis)) }
                                ?: stringResource(R.string.loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ‹ day › selector: ‹ goes back in time, › forward; disabled at the ends.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = { selectedIndex++ },
                    enabled = selectedIndex < days.lastIndex
                ) {
                    Text("‹", style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    text = formatDayLabel(
                        selectedDate,
                        today,
                        stringResource(R.string.day_today),
                        stringResource(R.string.day_yesterday)
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.width(150.dp),
                    textAlign = TextAlign.Center
                )
                IconButton(
                    onClick = { selectedIndex-- },
                    enabled = selectedIndex > 0
                ) {
                    Text("›", style = MaterialTheme.typography.titleLarge)
                }
            }

            Spacer(Modifier.height(8.dp))

            val current = detail
            when {
                sessions == null -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }

                current == null -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.detail_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .weight(1f)
                ) {
                    HourChart(current.hourly)
                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.detail_summary),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    val summary = listOf(
                        stringResource(R.string.detail_total) to formatDuration(current.totalMillis),
                        stringResource(R.string.detail_sessions) to current.sessionCount.toString(),
                        stringResource(R.string.detail_first) to formatTimeOfDay(current.firstStart),
                        stringResource(R.string.detail_last) to formatTimeOfDay(current.lastEnd),
                        stringResource(R.string.detail_average) to formatDuration(current.averageMillis),
                        stringResource(R.string.detail_longest) to formatDuration(current.longestMillis)
                    )
                    summary.forEach { (label, value) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(text = value, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.detail_sessions),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(current.sessions) { session ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${formatTimeOfDay(session.start)} – ${formatTimeOfDay(session.end)}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = formatDuration(session.end - session.start),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
