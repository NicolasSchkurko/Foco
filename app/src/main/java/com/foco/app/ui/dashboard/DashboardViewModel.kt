package com.foco.app.ui.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foco.app.data.db.DailyAggregate
import com.foco.app.data.export.WeekExportService
import com.foco.app.data.monitor.UsageRepository
import com.foco.app.data.stores.ExcludedAppsStore
import com.foco.app.data.stores.SessionHistoryStore
import com.foco.app.data.system.AppPackageInfo
import com.foco.app.domain.export.weekExportJson
import com.foco.app.domain.history.recentDays
import com.foco.app.domain.ring.DayRingState
import com.foco.app.domain.ring.DayStop
import com.foco.app.domain.ring.dayRing
import com.foco.app.domain.ring.key
import com.foco.app.model.UsageSession
import com.foco.app.model.day.DayData
import com.foco.app.ui.common.util.readOr
import com.foco.app.ui.metrics.day.DayMetrics
import com.foco.app.ui.metrics.day.deriveDayMetrics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

/**
 * ViewModel for the home dashboard: owns auto-refreshing live day usage, loading historical days and
 * week totals off the main thread, handling app exclusions and tracking ring navigation state.
 */
class DashboardViewModel(
    private val clock: Clock = Clock.systemDefaultZone(),
    private val repositoryProvider: ((Context) -> UsageRepository)? = null,
    private val historyStoreProvider: ((Context) -> SessionHistoryStore)? = null,
    private val excludedStoreProvider: ((Context) -> ExcludedAppsStore)? = null,
    private val loadDayOverride: (suspend () -> DayData)? = null,
    private val loadExportOverride: (suspend () -> String)? = null,
    private val refreshIntervalMillis: Long = DEFAULT_REFRESH_INTERVAL_MILLIS,
    coroutineScope: CoroutineScope? = null,
) : ViewModel() {
    private val scope: CoroutineScope get() = internalScope ?: viewModelScope
    private val internalScope: CoroutineScope? = coroutineScope

    private val dashboardState: DashboardState
    private val _uiState: MutableStateFlow<DashboardUiState>
    val uiState: StateFlow<DashboardUiState>

    private var repository: UsageRepository? = null
    private var historyStore: SessionHistoryStore? = null
    private var excludedStore: ExcludedAppsStore? = null
    private var weekTotalsDay: LocalDate? = null

    init {
        val today = LocalDate.now(clock)
        val initialStop = DayStop.Day(today)
        dashboardState = DashboardState(initialStop)

        _uiState =
            MutableStateFlow(
                DashboardUiState(
                    shown = initialStop,
                    ringState = DayRingState(initialStop),
                    today = today,
                ),
            )
        uiState = _uiState.asStateFlow()

        scope.launch {
            dashboardState.snapshot.collect { snapshot ->
                _uiState.update { current ->
                    current.copy(
                        shown = snapshot.shown,
                        ringState = snapshot.ring,
                        slideDirection = snapshot.slideDirection,
                        softMotion = snapshot.softMotion,
                    )
                }
                checkAndLoadCurrentPosition()
            }
        }
    }

    /** Binds the Android Context once attached from UI. */
    fun bindContext(context: Context) {
        if (repository != null) return
        val appContext = context.applicationContext
        val repo = repositoryProvider?.invoke(appContext) ?: UsageRepository.forApp(appContext)
        val store = historyStoreProvider?.invoke(appContext) ?: SessionHistoryStore.forApp(appContext) { day -> repo.loadDay(day) }
        val excluded = excludedStoreProvider?.invoke(appContext) ?: ExcludedAppsStore(appContext)

        repository = repo
        historyStore = store
        excludedStore = excluded

        scope.launch {
            excluded.excludedPackages.collect { set ->
                _uiState.update { it.copy(excludedPackages = set) }
            }
        }

        scope.launch {
            store.storedDaysFlow().readOr(emptySet<LocalDate>()).collect { days ->
                val today = LocalDate.now(clock)
                val ring = dayRing(today, days)
                _uiState.update { it.copy(storedDays = days) }

                val recentWindow = recentDays(today)
                dashboardState.retain(ring, { dayStop -> dayStop is DayStop.Day && dayStop.date in recentWindow }, today)
            }
        }

        startAutoRefreshLoop()
    }

    fun moveStep(forward: Boolean) {
        val today = LocalDate.now(clock)
        val ring = dayRing(today, uiState.value.storedDays)
        dashboardState.moveStep(ring, forward)
    }

    fun moveTo(
        target: DayStop,
        direction: Int? = null,
        soft: Boolean = false,
    ) {
        val today = LocalDate.now(clock)
        val ring = dayRing(today, uiState.value.storedDays)
        val recentWindow = recentDays(today)
        val isShortcut = target is DayStop.Day && target.date in recentWindow
        dashboardState.moveTo(target, ring, isShortcut, direction, soft)
    }

    fun goBack(): Boolean = dashboardState.goBack()

    fun excludeApp(packageName: String) {
        val store = excludedStore ?: return
        scope.launch {
            store.exclude(packageName)
        }
    }

    fun restoreApp(packageName: String) {
        val store = excludedStore ?: return
        scope.launch {
            store.restore(packageName)
        }
    }

    suspend fun exportWeekJson(context: Context? = null): String {
        loadExportOverride?.let { return it() }
        val appContext = context?.applicationContext ?: return ""
        val service =
            WeekExportService(
                context = appContext,
                labels = AppPackageInfo(appContext)::label,
            )
        val today = LocalDate.now(clock)
        return weekExportJson(service.export(today))
    }

    private fun startAutoRefreshLoop() {
        scope.launch {
            while (true) {
                val state = uiState.value
                val today = LocalDate.now(clock)
                val todayKey = DayStop.Day(today).key()
                val isLive = state.requested == DayStop.Week || (state.requested as? DayStop.Day)?.date == today

                if (isLive) {
                    try {
                        val content =
                            withContext(Dispatchers.IO) {
                                val data = loadDayOverride?.invoke() ?: repository?.loadDay(today) ?: DayData(emptyList(), emptyList())
                                RingContent.Day(
                                    data = data,
                                    unlocks = data.unlockTimes.size,
                                    metrics = deriveDayMetrics(data.sessions, ZoneId.systemDefault()),
                                )
                            }
                        putRingContent(todayKey, content)
                    } catch (f: Exception) {
                        android.util.Log.w("DashboardViewModel", "Periodic load failed", f)
                    }
                }
                delay(refreshIntervalMillis)
            }
        }
    }

    private fun checkAndLoadCurrentPosition() {
        val today = LocalDate.now(clock)
        val target = uiState.value.requested
        val targetKey = target.key()
        val date = (target as? DayStop.Day)?.date

        if (target == DayStop.Week) {
            if (weekTotalsDay != today || !_uiState.value.ringContent.containsKey(targetKey)) {
                scope.launch {
                    val totals =
                        withContext(Dispatchers.IO) {
                            readOr(emptyList<DailyAggregate>()) { historyStore?.week(today).orEmpty() }
                        }
                    putRingContent(targetKey, RingContent.Week(totals))
                    weekTotalsDay = today
                }
            }
        } else if (date != null && date != today) {
            if (!_uiState.value.ringContent.containsKey(targetKey)) {
                scope.launch {
                    val stored =
                        withContext(Dispatchers.IO) {
                            val sessions = readOr(emptyList<UsageSession>()) { historyStore?.sessionsFor(date).orEmpty() }
                            val unlocks = readOr<Int?>(null) { historyStore?.unlocksFor(date) }
                            RingContent.Day(
                                data = DayData(sessions, emptyList()),
                                unlocks = unlocks,
                                metrics = deriveDayMetrics(sessions, ZoneId.systemDefault()),
                            )
                        }
                    putRingContent(targetKey, stored)
                }
            }
        }

        val requestedReady =
            _uiState.value.ringContent.containsKey(targetKey) &&
                (target != DayStop.Week || _uiState.value.ringContent.containsKey(DayStop.Day(today).key()))
        if (requestedReady) {
            dashboardState.showCurrent()
        }
    }

    private fun putRingContent(
        key: String,
        content: RingContent,
    ) {
        _uiState.update { current ->
            val updatedMap = current.ringContent + (key to content)
            val today = LocalDate.now(clock)
            val ring = dayRing(today, current.storedDays)
            val keep = ring.mapTo(mutableSetOf()) { it.key() } + DayStop.Day(today).key() + current.requestedKey + current.shownKey
            val prunedMap = updatedMap.filterKeys { it in keep }
            current.copy(ringContent = prunedMap)
        }
        val targetKey = uiState.value.requestedKey
        val ready = _uiState.value.ringContent.containsKey(targetKey)
        if (ready) {
            dashboardState.showCurrent()
        }
    }
}

/** Returns day metrics filtered against excluded packages. */
fun metricsOf(
    day: RingContent.Day,
    excludedPackages: Set<String>,
): DayMetrics =
    day.metrics.copy(
        entries = day.metrics.entries.filter { it.packageName !in excludedPackages },
    )
