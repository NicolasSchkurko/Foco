package com.foco.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.foco.app.R
import com.foco.app.data.AppLimitsStore
import com.foco.app.ui.common.LimitsQuickAdjust
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * The quick adjust of one app: the `-15 min` / `+15 min` buttons and the time-range picker, with
 * the picks the caller persists through `AppLimitsStore`.
 */
class LimitsQuickAdjustTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val store = AppLimitsStore.forApp(targetContext)
    private val packageName = "com.example.quick"

    /** The strings of the activity: the per-app locale wins over the device one. */
    private fun appString(resId: Int): String = compose.activity.resources.getString(resId)

    @Before
    fun clean() {
        runBlocking { store.clearLimit(packageName) }
    }

    @Test
    fun thePlusButtonAddsOneStepAndReportsIt() {
        var limit: Int? = null
        compose.setContent {
            MaterialTheme {
                LimitsQuickAdjust(
                    store = store,
                    packageName = packageName,
                    onLimitChange = { _, minutes -> limit = minutes },
                    onWindowChange = { _, _, _ -> },
                    onWindowClear = { },
                )
            }
        }

        compose.onNodeWithText(appString(R.string.settings_limits_button_plus)).performClick()

        assertEquals(15, limit)
    }

    @Test
    fun theMinusButtonOnNoLimitDoesNothing() {
        var called = false
        compose.setContent {
            MaterialTheme {
                LimitsQuickAdjust(
                    store = store,
                    packageName = packageName,
                    onLimitChange = { _, _ -> called = true },
                    onWindowChange = { _, _, _ -> },
                    onWindowClear = { },
                )
            }
        }

        compose.onNodeWithText(appString(R.string.settings_limits_button_minus)).performClick()

        assertFalse("no limit must not be cleared", called)
    }

    @Test
    fun thePickerOpensWithTheSystemTimeFormat() {
        compose.setContent {
            MaterialTheme {
                LimitsQuickAdjust(
                    store = store,
                    packageName = packageName,
                    onLimitChange = { _, _ -> },
                    onWindowChange = { _, _, _ -> },
                    onWindowClear = { },
                )
            }
        }

        val pickerLabel = appString(R.string.settings_limits_picker_label)
        compose.onAllNodesWithText(pickerLabel).filterToOne(hasClickAction()).performClick()
        compose.onNodeWithText(appString(R.string.settings_limits_picker_title)).assertIsDisplayed()

        val expected =
            if (android.text.format.DateFormat.is24HourFormat(targetContext)) {
                "09:00"
            } else {
                val cal = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 9)
                    set(java.util.Calendar.MINUTE, 0)
                }
                android.text.format.DateFormat.getTimeFormat(targetContext).format(cal.time)
            }
        compose.onNodeWithText(expected).assertIsDisplayed()
    }
}
