package com.foco.app.ui.detail

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foco.app.data.monitor.UsageRepository
import com.foco.app.data.stores.AppLimitsStore
import com.foco.app.data.stores.SessionHistoryStore
import com.foco.app.domain.history.DaySource
import com.foco.app.domain.history.availableDays
import com.foco.app.domain.history.daySource
import com.foco.app.domain.history.recentDays
import com.foco.app.domain.history.reclampDay
import com.foco.app.domain.ring.stepCyclicIndex
import com.foco.app.model.UsageSession
import com.foco.app.ui.common.util.readOr
import com.foco.app.ui.metrics.detail.appDayDetail
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
 * ViewModel for one app's detail screen: owns day navigation window clamping, session and detail
 * summary calculation off the main thread, and app limits store observation.
 */
class AppDetailViewModel(
    val packageName: String,
    private val initialDate: LocalDate? = null,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val repositoryProvider: ((Context) -> UsageRepository)? = null,
    private val historyStoreProvider: ((Context) -> SessionHistoryStore)? = null,
    private val limitsStoreProvider: ((Context) -> AppLimitsStore)? = null,
    coroutineScope: CoroutineScope? = null,
) : ViewModel() {

    private val scope: CoroutineScope get() = internalScope ?: viewModelScope
    private val internalScope: CoroutineScope? = coroutineScope

    private val _uiState: MutableStateFlow<AppDetailUiState>
    val uiState: StateFlow<AppDetailUiState>

    private var repository: UsageRepository? = null
    private var historyStore: SessionHistoryStore? = null
    private var limitsStore: AppLimitsStore? = null
    private var storedDays: Set<LocalDate>? = null

    init {
        val today = LocalDate.now(clock)
        val initialDays = listOf(today)
        _uiState = MutableStateFlow(
            AppDetailUiState(
                packageName = packageName,
                selectedDate = initialDate ?: today,
                shownDate = initialDate ?: today,
                days = initialDays,
            )
        )
        uiState = _uiState.asStateFlow()
    }

    /** Binds the Android Context once attached from UI. */
    fun bindContext(context: Context) {
        if (repository != null) return
        val appContext = context.applicationContext
        val repo = repositoryProvider?.invoke(appContext) ?: UsageRepository.forApp(appContext)
        val store = historyStoreProvider?.invoke(appContext) ?: SessionHistoryStore.forApp(appContext) { date -> repo.loadDay(date) }
        val limits = limitsStoreProvider?.invoke(appContext) ?: AppLimitsStore.forApp(appContext)

        repository = repo
        historyStore = store
        limitsStore = limits

        scope.launch {
            limits.limitsFlow.collect { limitsMap ->
                _uiState.update { it.copy(limitMinutes = limitsMap[packageName]) }
            }
        }

        scope.launch {
            limits.windowsFlow.collect { windowsMap ->
                _uiState.update { it.copy(window = windowsMap[packageName]) }
            }
        }

        scope.launch {
            limits.alertsFlow.collect { alertsMap ->
                _uiState.update { it.copy(alertOn = alertsMap[packageName] == true) }
            }
        }

        scope.launch {
            store.storedDaysFlow().readOr(emptySet<LocalDate>()).collect { daysSet ->
                storedDays = daysSet
                updateDaysWindow(daysSet)
            }
        }

        loadCurrentDayData()
    }

    fun stepDay(delta: Int) {
        val currentDays = uiState.value.days
        if (currentDays.isEmpty()) return
        val currentIdx = uiState.value.effectiveIndex
        val targetIdx = stepCyclicIndex(currentIdx, currentDays.size, delta)
        val targetDate = currentDays.getOrNull(targetIdx) ?: return
        val slideDir = if (delta >= 0) 1 else -1

        _uiState.update {
            it.copy(
                selectedDate = targetDate,
                effectiveIndex = targetIdx,
                slideDirection = slideDir,
            )
        }
        loadCurrentDayData()
    }

    fun refreshOnResume() {
        loadCurrentDayData()
    }

    private fun updateDaysWindow(stored: Set<LocalDate>) {
        val today = LocalDate.now(clock)
        val available = availableDays(today, stored)
        val computedDays = if (initialDate != null && initialDate !in available && initialDate in recentDays(today)) {
            (available + initialDate).sortedDescending()
        } else {
            available
        }

        val clamped = reclampDay(uiState.value.selectedDate, computedDays, today)
        val newIndex = computedDays.indexOf(clamped).coerceAtLeast(0)

        _uiState.update {
            it.copy(
                days = computedDays,
                selectedDate = clamped,
                effectiveIndex = newIndex,
            )
        }
        loadCurrentDayData()
    }

    private fun loadCurrentDayData() {
        val date = uiState.value.selectedDate
        val today = LocalDate.now(clock)
        val currentStored = storedDays.orEmpty()
        val repo = repository
        val store = historyStore

        scope.launch {
            val loaded = withContext(Dispatchers.IO) {
                val loadedSessions = readOr(emptyList<UsageSession>()) {
                    when (daySource(date, today, currentStored)) {
                        DaySource.Live -> repo?.loadDay(date)?.sessions.orEmpty()
                        DaySource.Stored -> store?.sessionsFor(date).orEmpty()
                        DaySource.Absent -> emptyList()
                    }
                }
                loadedSessions to appDayDetail(loadedSessions, packageName, ZoneId.systemDefault())
            }

            _uiState.update { current ->
                val updatedDetails = current.dayDetails + (date to loaded)
                current.copy(
                    dayDetails = updatedDetails,
                    shownDate = date,
                )
            }
        }
    }
}
