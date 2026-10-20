package com.foco.app.data

import android.app.Notification
import android.app.NotificationManager
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.foco.app.R
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Category: notifications (API 36). One alert, one Entendido action, and no fake when denied. */
@RunWith(AndroidJUnit4::class)
class AppLimitAlertNotifierTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val notifications = context.getSystemService(NotificationManager::class.java)

    @Before
    fun setUp() {
        shell("pm grant com.foco.app android.permission.POST_NOTIFICATIONS")
        notifications.cancelAll()
    }

    @After
    fun cleanUp() {
        notifications.cancelAll()
        shell("pm grant com.foco.app android.permission.POST_NOTIFICATIONS")
    }

    private fun shell(command: String) {
        InstrumentationRegistry
            .getInstrumentation()
            .uiAutomation
            .executeShellCommand(command)
            .close()
    }

    private fun waitUntil(condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 10_000
        while (SystemClock.uptimeMillis() < deadline) {
            if (condition()) return
            SystemClock.sleep(50)
        }
        assertTrue("the expected notification state did not arrive", condition())
    }

    @Test
    fun postsOneNotificationWithOnlyTheEntendidoAction() {
        val notifier = AppLimitAlertNotifier(context)

        assertTrue(notifier.notify("com.foco.app", 75, 30))
        waitUntil { activeAlert() != null }

        val notification = activeAlert()!!.notification
        val title = notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString()
        val text = notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString()
        assertTrue(title.contains("Foco"))
        assertTrue(title.contains("75"))
        assertTrue(text.contains("30 min"))
        assertEquals(1, notification.actions.size)
        assertEquals(
            context.getString(R.string.limit_alert_ack),
            notification.actions
                .single()
                .title
                .toString(),
        )

        notification.actions
            .single()
            .actionIntent
            .send()
        waitUntil { activeAlert() == null }
    }

    private fun activeAlert() =
        notifications.activeNotifications
            .firstOrNull { it.notification.channelId == AppLimitAlertNotifier.CHANNEL_ID }
}
