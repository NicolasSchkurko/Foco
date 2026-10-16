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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.foco.app.data.UsageRepository
import com.foco.app.model.UsageSession
import com.foco.app.ui.formatDuration
import com.foco.app.ui.metrics.deriveDayMetrics
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Intervalo entre refrescos automáticos mientras el dashboard está visible. */
const val DEFAULT_REFRESH_INTERVAL_MILLIS = 30_000L

/**
 * Dashboard del día: total, actividad por hora y ranking de apps.
 *
 * Carga las sesiones del día y deriva las métricas en la capa de UI (funciones
 * puras en `ui/metrics/`). Los datos se actualizan solos: al mostrarse y cada
 * [refreshIntervalMillis] mientras la app está en primer plano.
 *
 * @param refreshIntervalMillis intervalo entre refrescos periódicos (los tests inyectan intervalos cortos).
 * @param loadSessions seam de tests: si es null, se usa el repositorio real.
 */
@Composable
fun DashboardScreen(
    refreshIntervalMillis: Long = DEFAULT_REFRESH_INTERVAL_MILLIS,
    loadSessions: (suspend () -> List<UsageSession>)? = null,
    onAppClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember { UsageRepository(context.applicationContext) }
    val load by rememberUpdatedState(loadSessions ?: { repository.loadToday() })

    var sessions by remember { mutableStateOf<List<UsageSession>?>(null) }

    // repeatOnLifecycle: el ciclo corre solo mientras la pantalla está RESUMED —
    // se relanza al volver a primer plano (refresco automático en cada vuelta)
    // y se cancela al pausar (nada de trabajo en background).
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, refreshIntervalMillis) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                sessions = withContext(Dispatchers.IO) { load() }
                delay(refreshIntervalMillis)
            }
        }
    }

    // Derivación memoizada: se recalcula solo cuando cambia la lista de sesiones.
    val metrics = remember(sessions) {
        sessions?.let { deriveDayMetrics(it, ZoneId.systemDefault()) }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            Column {
                Text("Hoy", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = metrics
                        ?.let { "Uso en pantalla: ${formatDuration(it.totalMillis)}" }
                        ?: "Cargando…",
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
                                onClick = { onAppClick(entry.packageName) }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
