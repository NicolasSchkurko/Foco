package com.foco.app.ui.settings

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.foco.app.R
import com.foco.app.data.AppLanguage
import com.foco.app.data.ExcludedAppsStore
import com.foco.app.data.SettingsStore
import com.foco.app.model.AppearanceOption
import com.foco.app.model.LanguageOption
import com.foco.app.ui.common.AppIcon
import com.foco.app.ui.common.appLabel
import kotlinx.coroutines.launch

/**
 * Ajustes: idioma (Automático/Español/Inglés), apariencia (Automático/Claro/Oscuro)
 * y gestión de las apps excluidas del ranking.
 */
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val store = remember { SettingsStore(context.applicationContext) }
    val excludedStore = remember { ExcludedAppsStore(context.applicationContext) }
    val scope = rememberCoroutineScope()

    val language by store.language.collectAsState(initial = LanguageOption.AUTO)
    val appearance by store.appearance.collectAsState(initial = AppearanceOption.AUTO)
    val excluded by excludedStore.excludedPackages.collectAsState(initial = emptySet())

    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Spacer(Modifier.height(16.dp))

            // Header: la flecha al borde inicial (inset 4 dp) y el título centrado en la ventana.
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
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .weight(1f)
            ) {
                item {
                    SectionTitle(stringResource(R.string.settings_language_section))
                    LanguageOption.entries.forEach { option ->
                        OptionRow(
                            label = languageLabel(option),
                            selected = language == option,
                            onSelect = {
                                scope.launch {
                                    store.setLanguage(option)
                                    AppLanguage.applyToSystem(option)
                                }
                            }
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(16.dp))
                    SectionTitle(stringResource(R.string.settings_appearance_section))
                    AppearanceOption.entries.forEach { option ->
                        OptionRow(
                            label = appearanceLabel(option),
                            selected = appearance == option,
                            onSelect = {
                                scope.launch { store.setAppearance(option) }
                            }
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(16.dp))
                    SectionTitle(stringResource(R.string.settings_excluded_section))
                    if (excluded.isEmpty()) {
                        Text(
                            text = stringResource(R.string.settings_excluded_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(excluded.sorted()) { packageName ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIcon(packageName = packageName)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = appLabel(packageName),
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { scope.launch { excludedStore.restore(packageName) } }) {
                            Text(stringResource(R.string.settings_restore))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun OptionRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun languageLabel(option: LanguageOption): String = when (option) {
    LanguageOption.AUTO -> stringResource(R.string.settings_option_auto)
    LanguageOption.ES -> stringResource(R.string.settings_language_es)
    LanguageOption.EN -> stringResource(R.string.settings_language_en)
}

@Composable
private fun appearanceLabel(option: AppearanceOption): String = when (option) {
    AppearanceOption.AUTO -> stringResource(R.string.settings_option_auto)
    AppearanceOption.LIGHT -> stringResource(R.string.settings_option_light)
    AppearanceOption.DARK -> stringResource(R.string.settings_option_dark)
}
