package com.foco.app.ui.common

import android.util.Log
import kotlinx.coroutines.CancellationException

/**
 * One day's read that degrades instead of crashing: a failure logs and answers [fallback] (today
 * alone, or an empty day) — internal so `AppDetailReadTest` can exercise it. Cancellation propagates.
 */
internal suspend fun <T> readOr(fallback: T, read: suspend () -> T): T =
    try {
        read()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        Log.w("Foco", "Day read failed", failure)
        fallback
    }
