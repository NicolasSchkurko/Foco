package com.foco.app.data

import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.foco.app.MainActivity
import com.foco.app.ui.setAccessibilityServiceEnabled
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Limit monitor (API 36): follows the limits and the accessibility binding; starts with a limit,
 *  stops with the last, and its ongoing notification is the observable "alive" signal. */
@RunWith(AndroidJUnit4::class)
class UsageMonitorServiceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val store = AppLimitsStore.forApp(context)
    private val notifications = context.getSystemService(NotificationManager::class.java)

    @Before
    fun setUp() {
        shell("appops set com.foco.app android:get_usage_stats allow")
        shell("pm grant com.foco.app android.permission.POST_NOTIFICATIONS")
        setAccessibilityServiceEnabled(true)
        runBlocking { store.clearAll() }
    }

    @After
    fun cleanUp() {
        UsageMonitorService.stop(context)
        setAccessibilityServiceEnabled(false)
        runBlocking { store.clearAll() }
    }

    private fun shell(command: String) {
        InstrumentationRegistry
            .getInstrumentation()
            .uiAutomation
            .executeShellCommand(command)
            .close()
    }

    private fun isRunning(): Boolean =
        notifications.activeNotifications.any { notification ->
            notification.notification.channelId == CHANNEL_ID &&
                (notification.notification.flags and Notification.FLAG_ONGOING_EVENT) != 0
        }

    /** The bounded poll: it stops as soon as the state arrives, so the wait is not a fixed sleep. */
    private fun waitUntil(condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 15_000
        while (SystemClock.uptimeMillis() < deadline) {
            if (condition()) return
            SystemClock.sleep(50)
        }
        assertTrue("the expected service state did not arrive", condition())
    }

    @Test
    fun startsWithALimitAndStopsWithTheLastOne() {
        ActivityScenario.launch(MainActivity::class.java).use {
            runBlocking { store.setLimit("com.example.app", 30) }
            UsageMonitorService.sync(context)
            waitUntil { isRunning() }

            runBlocking { store.clearAll() }
            UsageMonitorService.sync(context)
            waitUntil { !isRunning() }
            assertFalse(isRunning())
        }
    }

    @Test
    fun showsItsOngoingNotification() {
        ActivityScenario.launch(MainActivity::class.java).use {
            runBlocking { store.setLimit("com.example.app", 30) }
            UsageMonitorService.sync(context)
            waitUntil { isRunning() }

            assertTrue(
                notifications.activeNotifications.any {
                    it.notification.channelId == CHANNEL_ID
                },
            )
        }
    }

    @Test
    fun doesNotRunWithoutAccessibility() {
        setAccessibilityServiceEnabled(false)

        ActivityScenario.launch(MainActivity::class.java).use {
            runBlocking { store.setLimit("com.example.app", 30) }
            UsageMonitorService.sync(context)
            waitUntil { !isRunning() }
            assertFalse(isRunning())
        }
    }

    @Test
    fun theBootReceiverRestartsTheMonitor() {
        ActivityScenario.launch(MainActivity::class.java).use {
            runBlocking { store.setLimit("com.example.app", 30) }
            UsageMonitorBootReceiver().onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))
            waitUntil { isRunning() }
            assertTrue(isRunning())
        }
    }

    private companion object {
        const val CHANNEL_ID = "usage_monitor"
    }
}
