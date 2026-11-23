package com.nis.nistv.core.designsystem.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.nis.nistv.core.preferences.AppThemeConfig

// =============================================================================
// NisTV Revamped — Cinematic Obsidian 2.0 Theme Accessor & Application Wrapper
// =============================================================================

/**
 * Top-level theme accessor providing ergonomic access to custom NisTV tokens.
 * Usage:
 *   NisTheme.colors.accentCyan
 *   NisTheme.colors.scrimMedium
 *   NisTheme.textExtras.hero
 *   NisTheme.spacing.touchTarget
 */
object NisTheme {
    val colors: NisExtraColors
        @Composable
        @ReadOnlyComposable
        get() = LocalNisExtraColors.current

    val textExtras: NisTextExtras
        @Composable
        @ReadOnlyComposable
        get() = LocalNisTextExtras.current

    val spacing: NisSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalSpacing.current
}

@Composable
fun NisTvTheme(
    appTheme: AppThemeConfig = AppThemeConfig.DARK,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (appTheme) {
        AppThemeConfig.SYSTEM -> isSystemDark
        AppThemeConfig.DARK -> true
        AppThemeConfig.LIGHT -> false
    }

    val colorScheme = if (isDark) NisDarkColorScheme else NisLightColorScheme
    val extraColors = if (isDark) NisDarkExtraColors else NisLightExtraColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isDark
            insetsController.isAppearanceLightNavigationBars = !isDark
        }
    }

    CompositionLocalProvider(
        LocalNisExtraColors provides extraColors,
        LocalNisTextExtras provides NisTextExtras(),
        LocalSpacing provides NisSpacing()
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NisTypography,
            shapes = NisShapes,
            content = content
        )
    }
}
