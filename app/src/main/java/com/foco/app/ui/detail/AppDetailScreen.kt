package com.foco.app.ui.detail

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.foco.app.R
import com.foco.app.data.system.MotionScale
import com.foco.app.ui.common.apps.AppIcon
import com.foco.app.ui.common.apps.appLabel
import com.foco.app.ui.common.layout.EmptyState
import com.foco.app.ui.common.layout.LoadingState
import com.foco.app.ui.common.layout.ScreenHeader
import com.foco.app.ui.common.layout.StepSelectorRow
import com.foco.app.ui.common.ring.PositionRingContainer
import com.foco.app.ui.common.time.rememberAppLocale
import com.foco.app.ui.common.time.rememberCurrentDate
import com.foco.app.ui.common.time.rememberDayLabel
import com.foco.app.ui.formatDuration
import java.time.Clock
import java.time.LocalDate

/** Tag of the day label; the tests read it to see which day is shown. */
const val DAY_LABEL_TAG = "detail_day_label"

/** Tag of the detail's content; the tests address it without depending on the language. */
const val DETAIL_CONTENT_TAG = "detail_content"

/** Tag of the limits preview block; the tests address it without depending on the language. */
const val LIMITS_PREVIEW_TAG = "detail_limits_preview"

/**
 * One app's detail: hourly breakdown, the day's summary and the session list, with a ‹ day ›
 * selector that walks back from today to the oldest day the store has; [date] opens on that day.
 */
@Composable
fun AppDetailScreen(
    packageName: String,
    date: LocalDate? = null,
    clock: Clock = Clock.systemDefaultZone(),
    onConfigure: () -> Unit = {},
    viewModel: AppDetailViewModel =
        remember(packageName, date, clock) {
            AppDetailViewModel(
                packageName = packageName,
                initialDate = date,
                clock = clock,
            )
        },
) {
    val context = LocalContext.current
    LaunchedEffect(viewModel, context) {
        viewModel.bindContext(context)
    }

    var hasResumedOnce by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (hasResumedOnce) viewModel.refreshOnResume()
        hasResumedOnce = true
    }

    val uiState by viewModel.uiState.collectAsState()
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    val shownDate = uiState.shownDate
    val slideDirection = uiState.slideDirection
    val pair = uiState.currentPair
    val detail = pair?.second
    val limitMinutes = uiState.limitMinutes
    val window = uiState.window
    val alertOn = uiState.alertOn

    val motionScale = remember { MotionScale(context.applicationContext).scale() }
    val today = rememberCurrentDate(clock)
    val locale = rememberAppLocale()
    val dayLabel = rememberDayLabel(today, locale)
    val previousDay = stringResource(R.string.day_previous)
    val nextDay = stringResource(R.string.day_next)

    val selectorText =
        if (detail != null) {
            "${dayLabel(shownDate)} • ${formatDuration(detail.totalMillis)}"
        } else {
            dayLabel(shownDate)
        }

    Scaffold { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            Spacer(Modifier.height(16.dp))

            ScreenHeader(
                title = appLabel(packageName),
                onBack = { backDispatcher?.onBackPressed() },
                titleLeading = { AppIcon(packageName = packageName) },
                actions = {
                    IconButton(onClick = onConfigure) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = stringResource(R.string.detail_limits_configure),
                        )
                    }
                },
            )

            StepSelectorRow(
                text = selectorText,
                onPrevious = { viewModel.stepDay(1) },
                onNext = { viewModel.stepDay(-1) },
                previousContentDescription = previousDay,
                nextContentDescription = nextDay,
                textModifier = Modifier.testTag(DAY_LABEL_TAG),
                useArrowIcons = true,
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            Spacer(Modifier.height(8.dp))

            PositionRingContainer(
                targetState = shownDate.toString(),
                slideDirection = slideDirection,
                softMotion = true,
                motionScale = motionScale,
                onNext = { viewModel.stepDay(1) },
                onPrevious = { viewModel.stepDay(-1) },
                label = "detail-day-ring",
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 20.dp)
                        .testTag(DETAIL_CONTENT_TAG),
            ) { dateKey ->
                val date = runCatching { LocalDate.parse(dateKey) }.getOrNull()
                val dayPair = date?.let { uiState.dayDetails[it] }
                val daySessions = dayPair?.first
                val dayDetail = dayPair?.second

                when {
                    daySessions == null ->
                        LoadingState(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                        )

                    dayDetail == null ->
                        EmptyState(
                            text = stringResource(R.string.detail_empty),
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                        )

                    else ->
                        AppDetailContent(
                            detail = dayDetail,
                            limitMinutes = limitMinutes,
                            window = window,
                            alertOn = alertOn,
                            onConfigure = onConfigure,
                            modifier = Modifier.weight(1f),
                        )
                }
            }
        }
    }
}
