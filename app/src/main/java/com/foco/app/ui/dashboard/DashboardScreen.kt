package com.foco.app.ui.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.foco.app.data.ExcludedAppsStore
import com.foco.app.data.UsageRepository
import com.foco.app.model.day.DayData
import com.foco.app.ui.formatDuration
import com.foco.app.ui.metrics.deriveDayMetrics
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Intervalo entre refrescos automáticos mientras el dashboard está visible. */
const val DEFAULT_REFRESH_INTERVAL_MILLIS = 30_000L

/**
 * Dashboard del día: total, actividad por hora, ranking de apps y desbloqueos.
 *
 * Carga los datos del día (sesiones + marcas de desbloqueo) y deriva las métricas
 * en la capa de UI (funciones puras en `ui/metrics/`). Los datos se actualizan solos:
 * al mostrarse y cada [refreshIntervalMillis] mientras la app está en primer plano.
 *
 * @param refreshIntervalMillis intervalo entre refrescos periódicos (los tests inyectan intervalos cortos).
 * @param loadDay seam de tests: si es null, se usa el repositorio real.
 */
@Composable
fun DashboardScreen(
    refreshIntervalMillis: Long = DEFAULT_REFRESH_INTERVAL_MILLIS,
    loadDay: (suspend () -> DayData)? = null,
    onAppClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember { UsageRepository(context.applicationContext) }
    val load by rememberUpdatedState(loadDay ?: { repository.loadToday() })

    val excludedStore = remember { ExcludedAppsStore(context.applicationContext) }
    var excludedPackages by remember { mutableStateOf(emptySet<String>()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var day by remember { mutableStateOf<DayData?>(null) }

    // repeatOnLifecycle: el ciclo corre solo mientras la pantalla está RESUMED —
    // se relanza al volver a primer plano (refresco automático en cada vuelta)
    // y se cancela al pausar (nada de trabajo en background).
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, refreshIntervalMillis) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                day = withContext(Dispatchers.IO) { load() }
                delay(refreshIntervalMillis)
            }
        }
    }

    // Exclusiones del usuario (persistidas): solo afectan al ranking.
    LaunchedEffect(excludedStore) {
        excludedStore.excludedPackages.collect { excludedPackages = it }
    }

    val onExclude: (String) -> Unit = { packageName ->
        scope.launch {
            excludedStore.add(packageName)
            val result = snackbarHostState.showSnackbar(
                message = "Excluida de las estadísticas",
                actionLabel = "Deshacer"
            )
            if (result == SnackbarResult.ActionPerformed) {
                excludedStore.remove(packageName)
            }
        }
    }

    // Derivación memoizada: se recalcula cuando cambian los datos o las exclusiones.
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

            Column {
                Text("Hoy", style = MaterialTheme.typography.headlineMedium)
                val currentDay = day
                Text(
                    text = metrics?.let { m ->
                        val unlocks = currentDay?.unlockTimes?.size ?: 0
                        val unlockLabel = if (unlocks == 1) "desbloqueo" else "desbloqueos"
                        "Uso en pantalla: ${formatDuration(m.totalMillis)} · $unlocks $unlockLabel"
                    } ?: "Cargando…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                        text = "Sin actividad registrada todavía hoy.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> {
                    HourChart(current.hourly)
                    Spacer(Modifier.height(20.dp))
                    Text("Apps", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(current.entries, key = { it.packageName }) { entry ->
                            AppUsageRow(
                                entry = entry,
                                totalMillis = current.totalMillis,
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
