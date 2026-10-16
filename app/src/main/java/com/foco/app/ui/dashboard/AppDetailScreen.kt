package com.foco.app.ui.dashboard

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.foco.app.data.UsageRepository
import com.foco.app.data.availableDays
import com.foco.app.model.UsageSession
import com.foco.app.ui.formatDayLabel
import com.foco.app.ui.formatDuration
import com.foco.app.ui.formatTimeOfDay
import com.foco.app.ui.metrics.appDayDetail
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Detalle de una app: desglose por hora, resumen del día y lista de sesiones,
 * con selector ‹ día › limitado a la ventana que el sistema retiene.
 */
@Composable
fun AppDetailScreen(packageName: String) {
    val context = LocalContext.current
    val repository = remember { UsageRepository(context.applicationContext) }
    val today = remember { LocalDate.now() }
    val days = remember(today) { availableDays(today) }
    var selectedIndex by remember { mutableIntStateOf(0) } // 0 = hoy; crece hacia atrás
    var sessions by remember { mutableStateOf<List<UsageSession>?>(null) }

    val selectedDate = days[selectedIndex]
    LaunchedEffect(packageName, selectedDate) {
        sessions = null
        sessions = withContext(Dispatchers.IO) { repository.loadDay(selectedDate) }
    }

    val detail = remember(sessions, packageName) {
        sessions?.let { appDayDetail(it, packageName, ZoneId.systemDefault()) }
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
                AppIcon(packageName = packageName)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = appLabel(packageName),
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1
                    )
                    Text(
                        text = detail?.let { "Uso del día: ${formatDuration(it.totalMillis)}" }
                            ?: "Cargando…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Selector ‹ día ›: ‹ retrocede en el tiempo, › avanza; deshabilitadas en los bordes.
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
                    text = formatDayLabel(selectedDate, today),
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
                        text = "Sin actividad de esta app ese día.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> {
                    HourChart(current.hourly)
                    Spacer(Modifier.height(16.dp))

                    Text("Resumen", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    val summary = listOf(
                        "Total" to formatDuration(current.totalMillis),
                        "Sesiones" to current.sessionCount.toString(),
                        "Primera" to formatTimeOfDay(current.firstStart),
                        "Última" to formatTimeOfDay(current.lastEnd),
                        "Promedio" to formatDuration(current.averageMillis),
                        "Más larga" to formatDuration(current.longestMillis)
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
                    Text("Sesiones", style = MaterialTheme.typography.titleMedium)
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
