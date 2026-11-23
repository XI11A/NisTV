package com.nis.nistv

import androidx.compose.ui.graphics.Color
import com.nis.nistv.core.designsystem.theme.NisAccentCyan
import com.nis.nistv.core.designsystem.theme.NisAccentPrimary
import com.nis.nistv.core.designsystem.theme.NisBackground
import com.nis.nistv.core.designsystem.theme.NisDarkColorScheme
import com.nis.nistv.core.designsystem.theme.NisLiveRed
import com.nis.nistv.core.designsystem.theme.NisSurface
import com.nis.nistv.core.designsystem.theme.NisSurfaceElevated
import com.nis.nistv.core.designsystem.theme.NisSurfaceHigh
import com.nis.nistv.core.designsystem.theme.NisTextMuted
import com.nis.nistv.core.designsystem.theme.NisTextPrimary
import com.nis.nistv.core.designsystem.theme.NisTextSecondary
import com.nis.nistv.core.designsystem.theme.NisLightAccentAmber
import com.nis.nistv.core.designsystem.theme.NisLightAccentCyan
import com.nis.nistv.core.designsystem.theme.NisLightBackground
import com.nis.nistv.core.designsystem.theme.NisLightColorScheme
import com.nis.nistv.core.designsystem.theme.NisLightLiveEmerald
import com.nis.nistv.core.designsystem.theme.NisLightLiveRed
import com.nis.nistv.core.designsystem.theme.NisLightPrimary
import com.nis.nistv.core.designsystem.theme.NisLightSurface
import com.nis.nistv.core.designsystem.theme.NisLightSurfaceElevated
import com.nis.nistv.core.designsystem.theme.NisLightSurfaceHigh
import com.nis.nistv.core.designsystem.theme.NisLightTextMuted
import com.nis.nistv.core.designsystem.theme.NisLightTextPrimary
import com.nis.nistv.core.designsystem.theme.NisLightTextSecondary
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class ContrastTest {

    private fun calculateLuminance(color: Color): Double {
        fun channel(c: Float): Double {
            val d = c.toDouble()
            return if (d <= 0.04045) d / 12.92 else ((d + 0.055) / 1.055).pow(2.4)
        }
        val r = channel(color.red)
        val g = channel(color.green)
        val b = channel(color.blue)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun calculateContrastRatio(c1: Color, c2: Color): Double {
        val l1 = calculateLuminance(c1)
        val l2 = calculateLuminance(c2)
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    @Test
    fun testTextOnSurfacesMeetsWcagAA() {
        val surfaces = listOf(
            "Background" to NisBackground,
            "Surface" to NisSurface,
            "SurfaceElevated" to NisSurfaceElevated,
            "SurfaceHigh" to NisSurfaceHigh
        )

        val texts = listOf(
            "TextPrimary" to NisTextPrimary,
            "TextSecondary" to NisTextSecondary,
            "TextMuted" to NisTextMuted
        )

        for ((sName, surface) in surfaces) {
            for ((tName, text) in texts) {
                val ratio = calculateContrastRatio(text, surface)
                assertTrue(
                    "Expected $tName on $sName to meet WCAG AA text threshold (>= 4.5:1), but was ${String.format("%.2f", ratio)}:1",
                    ratio >= 4.5
                )
            }
        }
    }

    @Test
    fun testOnPrimaryOnPrimaryMeetsWcagAA() {
        val ratio = calculateContrastRatio(NisDarkColorScheme.onPrimary, NisDarkColorScheme.primary)
        assertTrue(
            "Expected onPrimary on primary to meet WCAG AA (>= 4.5:1), but was ${String.format("%.2f", ratio)}:1",
            ratio >= 4.5
        )
    }

    @Test
    fun testBrandAccentsOnSurfacesMeetUiThreshold() {
        val surfaces = listOf(
            "Background" to NisBackground,
            "Surface" to NisSurface,
            "SurfaceElevated" to NisSurfaceElevated,
            "SurfaceHigh" to NisSurfaceHigh
        )

        val accents = listOf(
            "Primary" to NisAccentPrimary,
            "Cyan" to NisAccentCyan,
            "LiveRed" to NisLiveRed
        )

        for ((sName, surface) in surfaces) {
            for ((aName, accent) in accents) {
                val ratio = calculateContrastRatio(accent, surface)
                assertTrue(
                    "Expected $aName on $sName to meet UI/graphical object threshold (>= 3.0:1), but was ${String.format("%.2f", ratio)}:1",
                    ratio >= 3.0
                )
            }
        }
    }

    @Test
    fun testLightTextOnSurfacesMeetsWcagAA() {
        val surfaces = listOf(
            "LightBackground" to NisLightBackground,
            "LightSurface" to NisLightSurface,
            "LightSurfaceElevated" to NisLightSurfaceElevated,
            "LightSurfaceHigh" to NisLightSurfaceHigh
        )

        val texts = listOf(
            "LightTextPrimary" to NisLightTextPrimary,
            "LightTextSecondary" to NisLightTextSecondary,
            "LightTextMuted" to NisLightTextMuted
        )

        for ((sName, surface) in surfaces) {
            for ((tName, text) in texts) {
                val ratio = calculateContrastRatio(text, surface)
                assertTrue(
                    "Expected $tName on $sName to meet WCAG AA text threshold (>= 4.5:1), but was ${String.format("%.2f", ratio)}:1",
                    ratio >= 4.5
                )
            }
        }
    }

    @Test
    fun testLightOnPrimaryOnPrimaryMeetsWcagAA() {
        val ratio = calculateContrastRatio(NisLightColorScheme.onPrimary, NisLightColorScheme.primary)
        assertTrue(
            "Expected onPrimary on primary in light scheme to meet WCAG AA (>= 4.5:1), but was ${String.format("%.2f", ratio)}:1",
            ratio >= 4.5
        )
    }

    @Test
    fun testLightBrandAccentsOnSurfacesMeetUiThreshold() {
        val surfaces = listOf(
            "LightBackground" to NisLightBackground,
            "LightSurface" to NisLightSurface,
            "LightSurfaceElevated" to NisLightSurfaceElevated,
            "LightSurfaceHigh" to NisLightSurfaceHigh
        )

        val accents = listOf(
            "LightPrimary" to NisLightPrimary,
            "LightCyan" to NisLightAccentCyan,
            "LightLiveRed" to NisLightLiveRed,
            "LightWarning" to NisLightAccentAmber,
            "LightSuccess" to NisLightLiveEmerald
        )

        for ((sName, surface) in surfaces) {
            for ((aName, accent) in accents) {
                val ratio = calculateContrastRatio(accent, surface)
                assertTrue(
                    "Expected $aName on $sName to meet UI threshold (>= 3.0:1), but was ${String.format("%.2f", ratio)}:1",
                    ratio >= 3.0
                )
            }
        }
    }
}
