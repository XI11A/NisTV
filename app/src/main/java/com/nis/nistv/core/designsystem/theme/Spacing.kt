package com.nis.nistv.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// =============================================================================
// NisTV Revamped — Cinematic Obsidian 2.0 Spacing & Dimension Tokens
// =============================================================================

@Immutable
data class NisSpacing(
    // 8-point harmonic spacing scale with 4dp micro-step
    val scale4: Dp = 4.dp,
    val scale8: Dp = 8.dp,
    val scale12: Dp = 12.dp,
    val scale16: Dp = 16.dp,
    val scale20: Dp = 20.dp,
    val scale24: Dp = 24.dp,
    val scale32: Dp = 32.dp,
    val scale40: Dp = 40.dp,
    val scale48: Dp = 48.dp,

    // Semantic spacing aliases (maintains backwards-compatibility with existing usages)
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 12.dp,
    val standard: Dp = 16.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp,
    val section: Dp = 40.dp,

    // Sizing & touch targets
    val touchTarget: Dp = 48.dp,
    val iconSmall: Dp = 20.dp,
    val iconMedium: Dp = 24.dp,
    val iconLarge: Dp = 32.dp,
    val maxContentWidth: Dp = 840.dp,

    // Adaptive screen paddings
    val screenPaddingPhone: Dp = 16.dp,
    val screenPaddingTablet: Dp = 24.dp
) {
    /**
     * Dynamically resolves screen padding based on device window width configuration:
     * 16dp for phones (<600dp), 24dp for tablets and foldables (>=600dp).
     */
    val screenPadding: Dp
        @Composable
        get() = if (LocalConfiguration.current.screenWidthDp >= 600) screenPaddingTablet else screenPaddingPhone
}

val LocalSpacing = staticCompositionLocalOf { NisSpacing() }

// Non-composable direct access tokens
object NisDimens {
    val Scale4 = 4.dp
    val Scale8 = 8.dp
    val Scale12 = 12.dp
    val Scale16 = 16.dp
    val Scale20 = 20.dp
    val Scale24 = 24.dp
    val Scale32 = 32.dp
    val Scale40 = 40.dp
    val Scale48 = 48.dp

    val TouchTarget = 48.dp
    val IconSmall = 20.dp
    val IconMedium = 24.dp
    val IconLarge = 32.dp
    val MaxContentWidth = 840.dp
    val ScreenPaddingPhone = 16.dp
    val ScreenPaddingTablet = 24.dp
    const val PosterAspectRatio: Float = 2f / 3f
}
