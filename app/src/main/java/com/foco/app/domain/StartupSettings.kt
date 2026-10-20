package com.foco.app.domain

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

/** Runs startup work with a bound; timeout or failure returns the fallback instead of blocking forever. */
internal suspend fun <T> withStartupTimeout(
    timeoutMillis: Long,
    fallback: () -> T,
    block: suspend () -> T,
): T =
    try {
        withTimeoutOrNull(timeoutMillis) { block() } ?: fallback()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Throwable) {
        fallback()
    }
