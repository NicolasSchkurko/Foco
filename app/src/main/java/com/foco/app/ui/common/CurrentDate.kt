package com.foco.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlinx.coroutines.delay

/**
 * The current local date, refreshed on resume and at the next midnight; [clock] is the test seam.
 */
@Composable
fun rememberCurrentDate(clock: Clock = Clock.systemDefaultZone()): LocalDate {
    var today by remember { mutableStateOf(LocalDate.now(clock)) }
    val latestClock by rememberUpdatedState(clock)
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) today = LocalDate.now(latestClock)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(latestClock.zone) {
        while (true) {
            val now = ZonedDateTime.now(latestClock)
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
            delay(Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1L))
            today = LocalDate.now(latestClock)
        }
    }

    return today
}
