package com.foco.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.foco.app.UsageAccess

/**
 * Se muestra cuando la app todavía no tiene el permiso de "Acceso de uso".
 * Le explica al usuario qué activar y lo lleva directo a la pantalla correcta de Ajustes.
 */
@Composable
fun OnboardingScreen() {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Foco",
            style = MaterialTheme.typography.displaySmall
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Para medir cuánto usás cada app, Foco necesita acceso a las " +
                "estadísticas de uso. Android solo permite activarlo a mano, desde Ajustes.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))

        Button(onClick = { context.startActivity(UsageAccess.settingsIntent()) }) {
            Text("Abrir Ajustes")
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Buscá «Foco» en la lista y activá el permiso. Al volver acá, " +
                "la pantalla se actualiza sola.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
