package com.foco.app.ui.settings

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.foco.app.R
import com.foco.app.data.AppIdentity
import com.foco.app.data.AppLanguage
import com.foco.app.data.ExcludedAppsStore
import com.foco.app.data.SettingsStore
import com.foco.app.model.AppearanceOption
import com.foco.app.model.LanguageOption
import com.foco.app.ui.common.AppIcon
import com.foco.app.ui.common.ScreenHeader
import com.foco.app.ui.common.appLabel
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Settings: the About block, language and appearance as one row with a dropdown each, and the
 * management of the apps excluded from the ranking.
 */
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val store = remember { SettingsStore(context.applicationContext) }
    val excludedStore = remember { ExcludedAppsStore(context.applicationContext) }
    val identity = remember { AppIdentity(context.applicationContext) }
    val scope = rememberCoroutineScope()

    val language by store.language.collectAsState(initial = LanguageOption.AUTO)
    val appearance by store.appearance.collectAsState(initial = AppearanceOption.AUTO)
    val excluded by excludedStore.excludedPackages.collectAsState(initial = emptySet())

    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    Scaffold { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            Spacer(Modifier.height(16.dp))

            ScreenHeader(
                title = stringResource(R.string.settings_title),
                onBack = { backDispatcher?.onBackPressed() },
            )

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .weight(1f),
            ) {
                item {
                    AboutBlock(
                        version = identity.versionName(),
                        onOpenRepository = { context.startActivity(identity.repositoryIntent()) },
                    )
                    Spacer(Modifier.height(16.dp))
                }

                item {
                    DropdownRow(
                        title = stringResource(R.string.settings_language_section),
                        value = languageLabel(language),
                        options =
                            LanguageOption.entries.map { option ->
                                languageLabel(option) to {
                                    // The pick re-creates the activity: the write must not be cancelled with it.
                                    scope.launch {
                                        withContext(NonCancellable) { store.setLanguage(option) }
                                        AppLanguage.applyToSystem(option)
                                    }
                                }
                            },
                    )
                }

                item {
                    Spacer(Modifier.height(16.dp))
                    DropdownRow(
                        title = stringResource(R.string.settings_appearance_section),
                        value = appearanceLabel(appearance),
                        options =
                            AppearanceOption.entries.map { option ->
                                appearanceLabel(option) to {
                                    scope.launch {
                                        withContext(NonCancellable) { store.setAppearance(option) }
                                    }
                                }
                            },
                    )
                }

                item {
                    Spacer(Modifier.height(16.dp))
                    SectionTitle(stringResource(R.string.settings_excluded_section))
                    if (excluded.isEmpty()) {
                        Text(
                            text = stringResource(R.string.settings_excluded_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                items(excluded.sorted()) { packageName ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIcon(packageName = packageName)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = appLabel(packageName),
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
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

/** The About block: credit, installed version and the way to the repository. */
@Composable
private fun AboutBlock(
    version: String?,
    onOpenRepository: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.settings_about_credit),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text =
                stringResource(
                    R.string.settings_about_version,
                    version ?: stringResource(R.string.settings_about_version_unknown),
                ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = onOpenRepository) {
            Text(stringResource(R.string.settings_about_repository))
        }
    }
}

/** One setting: its title, the active label and a chevron that opens the anchored dropdown. */
@Composable
private fun DropdownRow(
    title: String,
    value: String,
    options: List<Pair<String, () -> Unit>>,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true }
                    .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (label, onSelect) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onSelect()
                        expanded = false
                    },
                )
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
private fun languageLabel(option: LanguageOption): String =
    when (option) {
        LanguageOption.AUTO -> stringResource(R.string.settings_option_auto)
        LanguageOption.ES -> stringResource(R.string.settings_language_es)
        LanguageOption.EN -> stringResource(R.string.settings_language_en)
    }

@Composable
private fun appearanceLabel(option: AppearanceOption): String =
    when (option) {
        AppearanceOption.AUTO -> stringResource(R.string.settings_option_auto)
        AppearanceOption.LIGHT -> stringResource(R.string.settings_option_light)
        AppearanceOption.DARK -> stringResource(R.string.settings_option_dark)
    }
