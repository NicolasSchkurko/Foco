package com.foco.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.foco.app.R
import com.foco.app.ui.onboarding.OnboardingScreen
import com.foco.app.ui.theme.FocoTheme
import kotlin.math.pow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Category: contrast (API 36). The activity paints the themed surface behind the content without
 * giving it a content colour, which is what made the onboarding text black on black.
 */
@RunWith(AndroidJUnit4::class)
class OnboardingContrastTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val strings = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun darkAppearanceKeepsTheTextLegible() = assertLegible(dark = true)

    @Test
    fun lightAppearanceKeepsTheTextLegible() = assertLegible(dark = false)

    private fun assertLegible(dark: Boolean) {
        composeRule.setContent {
            FocoTheme(darkTheme = dark) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                    OnboardingScreen()
                }
            }
        }

        val appearance = if (dark) "dark" else "light"
        val background = composeRule.onRoot().captureToImage().toPixelMap()[0, 0]
        val title = contrast(composeRule.onNodeWithText("Foco").captureToImage(), background)
        val explanation = contrast(
            composeRule.onNodeWithText(strings.getString(R.string.onboarding_explanation))
                .captureToImage(),
            background
        )

        assertTrue("title contrast $title on $appearance", title >= 3.0)
        assertTrue("explanation contrast $explanation on $appearance", explanation >= 4.5)
    }

    /** The worst ratio between the darkest and the lightest pixel, each composited over [background]. */
    private fun contrast(image: ImageBitmap, background: Color): Double {
        val pixels = image.toPixelMap()
        var min = 1.0
        var max = 0.0
        for (x in 0 until pixels.width) {
            for (y in 0 until pixels.height) {
                val luminance = luminance(over(pixels[x, y], background))
                if (luminance < min) min = luminance
                if (luminance > max) max = luminance
            }
        }
        return (max + 0.05) / (min + 0.05)
    }

    private fun over(foreground: Color, background: Color): Color {
        val alpha = foreground.alpha.toDouble()
        return Color(
            red = (foreground.red * alpha + background.red * (1 - alpha)).toFloat(),
            green = (foreground.green * alpha + background.green * (1 - alpha)).toFloat(),
            blue = (foreground.blue * alpha + background.blue * (1 - alpha)).toFloat(),
            alpha = 1f
        )
    }

    private fun luminance(color: Color): Double {
        fun channel(value: Float): Double {
            val c = value.toDouble()
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) +
            0.0722 * channel(color.blue)
    }
}
