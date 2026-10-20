package com.foco.app.ui

import android.app.KeyguardManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.os.PowerManager
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.platform.app.InstrumentationRegistry
import com.foco.app.ui.metrics.MIN_VISIBLE_SESSION_MILLIS

/** The app whose real usage the dashboard tests need; also the system Settings package. */
private const val SETTINGS_PACKAGE = "com.android.settings"

private const val LOOKBACK_MILLIS = 60_000L

/** Waits for the device to be awake and unlocked after the shell setup commands. */
internal fun waitForDeviceAwakeAndUnlocked(rule: ComposeTestRule) {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val power = context.getSystemService(PowerManager::class.java)
    val keyguard = context.getSystemService(KeyguardManager::class.java)
    rule.waitUntil(timeoutMillis = 5_000) {
        power.isInteractive && !keyguard.isKeyguardLocked
    }
}

/** Waits for the screen state a `KEYCODE_SLEEP`/`KEYCODE_WAKEUP` should produce. */
internal fun waitForInteractive(rule: ComposeTestRule, interactive: Boolean) {
    val power = InstrumentationRegistry.getInstrumentation().targetContext
        .getSystemService(PowerManager::class.java)
    rule.waitUntil(timeoutMillis = 5_000) { power.isInteractive == interactive }
}

/** Waits for the keyguard to report unlocked after `MENU` or `dismiss-keyguard`. */
internal fun waitForKeyguardUnlocked(rule: ComposeTestRule) {
    val keyguard = InstrumentationRegistry.getInstrumentation().targetContext
        .getSystemService(KeyguardManager::class.java)
    rule.waitUntil(timeoutMillis = 5_000) { !keyguard.isKeyguardLocked }
}

/**
 * Waits until Settings has been resumed long enough to count as a visible session for the
 * dashboard, then returns; this replaces the old fixed 2 s usage-generation sleep.
 */
internal fun waitForSettingsForeground(rule: ComposeTestRule) {
    var resumedAt = 0L
    rule.waitUntil(timeoutMillis = 10_000) {
        val last = lastSettingsEvent()
        if (last?.first == UsageEvents.Event.ACTIVITY_RESUMED) {
            resumedAt = last.second
            true
        } else {
            false
        }
    }
    rule.waitUntil(timeoutMillis = 5_000) {
        System.currentTimeMillis() - resumedAt >= MIN_VISIBLE_SESSION_MILLIS
    }
}

/** Waits until Settings has left the foreground so its usage session is closed. */
internal fun waitForSettingsBackground(rule: ComposeTestRule) {
    rule.waitUntil(timeoutMillis = 5_000) {
        lastSettingsEvent()?.first in setOf(
            UsageEvents.Event.ACTIVITY_PAUSED,
            UsageEvents.Event.ACTIVITY_STOPPED
        )
    }
}

/** The last usage event type and timestamp for Settings in the recent window, if any. */
private fun lastSettingsEvent(): Pair<Int, Long>? {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val usageStats = context.getSystemService(UsageStatsManager::class.java)
    val now = System.currentTimeMillis()
    val events = usageStats.queryEvents(now - LOOKBACK_MILLIS, now)
    var last: Pair<Int, Long>? = null
    val event = UsageEvents.Event()
    while (events.hasNextEvent()) {
        events.getNextEvent(event)
        if (event.packageName == SETTINGS_PACKAGE) last = event.eventType to event.timeStamp
    }
    return last
}
