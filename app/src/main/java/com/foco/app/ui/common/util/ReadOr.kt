package com.foco.app.ui.common.util

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

/**
 * One day's read that degrades instead of crashing: a failure logs and answers [fallback] (today
 * alone, or an empty day) — internal so `AppDetailReadTest` can exercise it. Cancellation propagates.
 */
internal suspend fun <T> readOr(
    fallback: T,
    read: suspend () -> T,
): T =
    try {
        read()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        runCatching { Log.w("Foco", "Day read failed", failure) }
        fallback
    }

/**
 * The stream half of [readOr]: a read that fails mid-stream logs and answers [fallback], so the
 * screen keeps a usable window; a cancelled collection ends without a value.
 */
internal fun <T> Flow<T>.readOr(fallback: T): Flow<T> =
    catch { failure ->
        if (failure is CancellationException) throw failure
        runCatching { Log.w("Foco", "Day read failed", failure) }
        emit(fallback)
    }
