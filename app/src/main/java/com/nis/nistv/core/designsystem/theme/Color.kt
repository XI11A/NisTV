package com.nis.nistv.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// =============================================================================
// NisTV Revamped — Cinematic Obsidian 2.0 Design Tokens
// =============================================================================

// Surfaces
val NisBackground = Color(0xFF0B0D12)       // App canvas / deep background
val NisSurface = Color(0xFF131823)          // Primary card / bar surface
val NisSurfaceElevated = Color(0xFF1A2130)  // Raised surface / tile container / dialog
val NisSurfaceHigh = Color(0xFF232C3F)      // High-elevation surface / popup
val NisBorder = Color(0xFF2A3347)           // Standard structural border
val NisBorderSubtle = Color(0xFF1E2536)     // Subtle internal divider / card border
val NisCardBorder = NisBorder
val NisDivider = NisBorder
val NisCardHover = Color(0xFF202942)        // Surface hover / active highlight
val NisNavbarBg = Color(0xF00B0D12)         // 94% alpha dark glass
val NisNavbarBorder = NisBorder

// Brand & Accents
val NisAccentPrimary = Color(0xFF7A8FFF)          // Periwinkle primary action / focus
val NisAccentPrimaryLight = Color(0xFF9DAEFF)     // Light primary highlight / hover
val NisAccentPrimaryPressed = Color(0xFF6377F0)   // Active pressed state
val NisAccentPrimaryContainer = Color(0xFF232C5E) // Low-prominence container / selection tint
val NisAccentCyan = Color(0xFF3CCBFF)             // Secondary streaming accent / HD pill
val NisAccentViolet = Color(0xFFA978FF)           // Tertiary accent / brand gradient stop

// Semantic Status
val NisLiveRed = Color(0xFFFF4D5E)          // Vibrant live stream badge
val NisError = Color(0xFFFF5C6C)            // Destructive / error state
val NisAccentAmber = Color(0xFFF5A524)      // Star rating / warning amber
val NisWarning = NisAccentAmber
val NisLiveEmerald = Color(0xFF34D399)      // Online / success indicator
val NisSuccess = NisLiveEmerald

// Typography
val NisTextPrimary = Color(0xFFF4F6FB)      // High-contrast primary copy (WCAG AAA on bg)
val NisTextSecondary = Color(0xFFA9B2C6)    // Secondary metadata / subtitles
val NisTextMuted = Color(0xFF8B94AA)        // Tertiary / hint text (WCAG AA >= 4.5:1)

// Scrims (replacing ad-hoc alpha layers)
val NisScrimSoft = Color(0x660B0D12)        // 40% alpha background scrim
val NisScrimMedium = Color(0xB30B0D12)      // 70% alpha hero / poster scrim
val NisScrimStrong = Color(0xDE0B0D12)      // 87% alpha modal / dialog scrim
val NisPlayerScrim = Color(0xD9000000)      // 85% alpha black player control overlay

// Brand Gradients
val NisBrandGradient = listOf(NisAccentCyan, NisAccentPrimary, NisAccentViolet)
val NisBrandGradientSoft = listOf(
    Color(0x663CCBFF),
    Color(0x667A8FFF),
    Color(0x66A978FF)
)

// Backwards-Compatibility Deprecated Aliases (Will be migrated in Stage 3)
@Deprecated("Use NisAccentPrimary instead", ReplaceWith("NisAccentPrimary"))
val NisAccentGreen = NisAccentPrimary

@Deprecated("Use NisAccentPrimaryPressed instead", ReplaceWith("NisAccentPrimaryPressed"))
val NisAccentGreenPressed = NisAccentPrimaryPressed

// Legacy / Style Aliases
val NisCardBackground = NisSurface
val NtvCanvas = NisBackground
val NtvSurface = NisSurface
val NtvSurfaceElevated = NisSurfaceElevated
val NtvCardBorder = NisCardBorder
val NtvAccent = NisAccentPrimary
val NtvCyan = NisAccentCyan
val NtvGold = NisAccentAmber

// =============================================================================
// Material 3 Dark Color Scheme Mapping
// =============================================================================

val NisDarkColorScheme = darkColorScheme(
    primary = NisAccentPrimary,
    onPrimary = NisBackground,
    primaryContainer = NisAccentPrimaryContainer,
    onPrimaryContainer = NisAccentPrimaryLight,
    inversePrimary = NisAccentPrimaryPressed,
    secondary = NisAccentCyan,
    onSecondary = NisBackground,
    secondaryContainer = NisSurfaceElevated,
    onSecondaryContainer = NisAccentCyan,
    tertiary = NisAccentViolet,
    onTertiary = NisBackground,
    tertiaryContainer = NisSurfaceElevated,
    onTertiaryContainer = NisAccentViolet,
    background = NisBackground,
    onBackground = NisTextPrimary,
    surface = NisSurface,
    onSurface = NisTextPrimary,
    surfaceVariant = NisSurfaceElevated,
    onSurfaceVariant = NisTextSecondary,
    surfaceTint = NisAccentPrimary,
    inverseSurface = NisTextPrimary,
    inverseOnSurface = NisBackground,
    error = NisError,
    onError = NisBackground,
    errorContainer = NisSurfaceElevated,
    onErrorContainer = NisError,
    outline = NisBorder,
    outlineVariant = NisBorderSubtle,
    scrim = NisScrimStrong,
    surfaceContainerLowest = NisBackground,
    surfaceContainerLow = NisSurface,
    surfaceContainer = NisSurface,
    surfaceContainerHigh = NisSurfaceElevated,
    surfaceContainerHighest = NisSurfaceHigh,
    surfaceDim = NisBackground,
    surfaceBright = NisSurfaceHigh
)

// =============================================================================
// NisTV Revamped — Light Theme Tokens
// =============================================================================

// Light Surfaces & Borders
val NisLightBackground = Color(0xFFF6F7FB)
val NisLightSurface = Color(0xFFFFFFFF)
val NisLightSurfaceElevated = Color(0xFFEEF0F7)
val NisLightSurfaceHigh = Color(0xFFE2E6F0)
val NisLightBorder = Color(0xFFD5DAE6)
val NisLightBorderSubtle = Color(0xFFE4E8F2)
val NisLightCardHover = Color(0xFFE8ECF5)
val NisLightNavbarBg = Color(0xF0F6F7FB)         // 94% alpha light glass
val NisLightNavbarBorder = NisLightBorder

// Light Brand & Accents
val NisLightPrimary = Color(0xFF4C5FD5)
val NisLightPrimaryLight = Color(0xFF6C7CE8)
val NisLightPrimaryPressed = Color(0xFF3B4BBF)
val NisLightPrimaryContainer = Color(0xFFE8EBFC)
val NisLightAccentCyan = Color(0xFF007B99)
val NisLightAccentViolet = Color(0xFF7E4BD6)

// Light Semantic Status (adjusted for light surface contrast >= 3:1)
val NisLightLiveRed = Color(0xFFD92534)
val NisLightError = Color(0xFFD92D3E)
val NisLightAccentAmber = Color(0xFF9E5A00)
val NisLightWarning = NisLightAccentAmber
val NisLightLiveEmerald = Color(0xFF0E804D)
val NisLightSuccess = NisLightLiveEmerald

// Light Typography (adjusted for >= 4.5:1 WCAG AA across all light surfaces)
val NisLightTextPrimary = Color(0xFF0E1220)
val NisLightTextSecondary = Color(0xFF4A5266)
val NisLightTextMuted = Color(0xFF586277)

// Light Scrims
val NisLightScrimSoft = Color(0x330E1220)
val NisLightScrimMedium = Color(0x800E1220)
val NisLightScrimStrong = Color(0xB30E1220)

// Light Gradients
val NisLightBrandGradient = listOf(NisLightAccentCyan, NisLightPrimary, NisLightAccentViolet)
val NisLightBrandGradientSoft = listOf(
    Color(0x33007B99),
    Color(0x334C5FD5),
    Color(0x337E4BD6)
)

// =============================================================================
// NisTV Revamped — Immersive Dark Tokens (Guaranteed Dark in Both Themes)
// =============================================================================

val NisImmersiveBackground = Color(0xFF0B0D12)
val NisImmersiveSurface = Color(0xFF131823)
val NisImmersiveSurfaceElevated = Color(0xFF1A2130)
val NisImmersiveSurfaceHigh = Color(0xFF232C3F)
val NisImmersiveBorder = Color(0xFF2A3347)
val NisImmersiveBorderSubtle = Color(0xFF1E2536)
val NisImmersiveTextPrimary = Color(0xFFF4F6FB)
val NisImmersiveTextSecondary = Color(0xFFA9B2C6)
val NisImmersiveTextMuted = Color(0xFF8B94AA)
val NisImmersiveScrimSoft = Color(0x660B0D12)
val NisImmersiveScrimMedium = Color(0xB30B0D12)
val NisImmersiveScrimStrong = Color(0xDE0B0D12)
val NisImmersivePlayerScrim = Color(0xD9000000)

// =============================================================================
// Material 3 Light Color Scheme Mapping
// =============================================================================

val NisLightColorScheme = lightColorScheme(
    primary = NisLightPrimary,
    onPrimary = Color.White,
    primaryContainer = NisLightPrimaryContainer,
    onPrimaryContainer = NisLightPrimary,
    inversePrimary = NisAccentPrimary,
    secondary = NisLightAccentCyan,
    onSecondary = Color.White,
    secondaryContainer = NisLightSurfaceElevated,
    onSecondaryContainer = NisLightAccentCyan,
    tertiary = NisLightAccentViolet,
    onTertiary = Color.White,
    tertiaryContainer = NisLightSurfaceElevated,
    onTertiaryContainer = NisLightAccentViolet,
    background = NisLightBackground,
    onBackground = NisLightTextPrimary,
    surface = NisLightSurface,
    onSurface = NisLightTextPrimary,
    surfaceVariant = NisLightSurfaceElevated,
    onSurfaceVariant = NisLightTextSecondary,
    surfaceTint = NisLightPrimary,
    inverseSurface = NisSurface,
    inverseOnSurface = NisTextPrimary,
    error = NisLightError,
    onError = Color.White,
    errorContainer = Color(0xFFFEE8E8),
    onErrorContainer = NisLightError,
    outline = NisLightBorder,
    outlineVariant = NisLightBorderSubtle,
    scrim = NisScrimStrong,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = NisLightBackground,
    surfaceContainer = NisLightSurface,
    surfaceContainerHigh = NisLightSurfaceElevated,
    surfaceContainerHighest = NisLightSurfaceHigh,
    surfaceDim = NisLightSurfaceElevated,
    surfaceBright = Color.White
)

// =============================================================================
// Extended Tokens (Tokens with no direct Material 3 ColorScheme slot)
// =============================================================================

@Immutable
data class NisExtraColors(
    val isDark: Boolean = true,
    // Base Surfaces & Borders
    val background: Color = NisBackground,
    val surface: Color = NisSurface,
    val surfaceElevated: Color = NisSurfaceElevated,
    val surfaceHigh: Color = NisSurfaceHigh,
    val border: Color = NisBorder,
    val borderSubtle: Color = NisBorderSubtle,
    val cardHover: Color = NisCardHover,
    val navbarBg: Color = NisNavbarBg,
    val navbarBorder: Color = NisNavbarBorder,
    // Brand & Accents
    val primary: Color = NisAccentPrimary,
    val primaryLight: Color = NisAccentPrimaryLight,
    val primaryPressed: Color = NisAccentPrimaryPressed,
    val primaryContainer: Color = NisAccentPrimaryContainer,
    val accentCyan: Color = NisAccentCyan,
    val accentViolet: Color = NisAccentViolet,
    // Semantic Status
    val live: Color = NisLiveRed,
    val error: Color = NisError,
    val warning: Color = NisWarning,
    val success: Color = NisSuccess,
    // Typography
    val textPrimary: Color = NisTextPrimary,
    val textSecondary: Color = NisTextSecondary,
    val textMuted: Color = NisTextMuted,
    // Scrims
    val scrimSoft: Color = NisScrimSoft,
    val scrimMedium: Color = NisScrimMedium,
    val scrimStrong: Color = NisScrimStrong,
    val playerScrim: Color = NisPlayerScrim,
    // Gradients
    val brandGradient: List<Color> = NisBrandGradient,
    val brandGradientSoft: List<Color> = NisBrandGradientSoft,
    // Immersive Surfaces (always dark in both themes)
    val immersiveBackground: Color = NisImmersiveBackground,
    val immersiveSurface: Color = NisImmersiveSurface,
    val immersiveSurfaceElevated: Color = NisImmersiveSurfaceElevated,
    val immersiveBorder: Color = NisImmersiveBorder,
    val immersiveBorderSubtle: Color = NisImmersiveBorderSubtle,
    val immersiveTextPrimary: Color = NisImmersiveTextPrimary,
    val immersiveTextSecondary: Color = NisImmersiveTextSecondary,
    val immersiveTextMuted: Color = NisImmersiveTextMuted,
    val immersiveScrimMedium: Color = NisImmersiveScrimMedium,
    val immersiveScrimStrong: Color = NisImmersiveScrimStrong,
    val immersivePlayerScrim: Color = NisImmersivePlayerScrim
)

val NisDarkExtraColors = NisExtraColors(isDark = true)

val NisLightExtraColors = NisExtraColors(
    isDark = false,
    background = NisLightBackground,
    surface = NisLightSurface,
    surfaceElevated = NisLightSurfaceElevated,
    surfaceHigh = NisLightSurfaceHigh,
    border = NisLightBorder,
    borderSubtle = NisLightBorderSubtle,
    cardHover = NisLightCardHover,
    navbarBg = NisLightNavbarBg,
    navbarBorder = NisLightNavbarBorder,
    primary = NisLightPrimary,
    primaryLight = NisLightPrimaryLight,
    primaryPressed = NisLightPrimaryPressed,
    primaryContainer = NisLightPrimaryContainer,
    accentCyan = NisLightAccentCyan,
    accentViolet = NisLightAccentViolet,
    live = NisLightLiveRed,
    error = NisLightError,
    warning = NisLightWarning,
    success = NisLightSuccess,
    textPrimary = NisLightTextPrimary,
    textSecondary = NisLightTextSecondary,
    textMuted = NisLightTextMuted,
    scrimSoft = NisLightScrimSoft,
    scrimMedium = NisLightScrimMedium,
    scrimStrong = NisLightScrimStrong,
    playerScrim = NisPlayerScrim,
    brandGradient = NisLightBrandGradient,
    brandGradientSoft = NisLightBrandGradientSoft,
    immersiveBackground = NisImmersiveBackground,
    immersiveSurface = NisImmersiveSurface,
    immersiveSurfaceElevated = NisImmersiveSurfaceElevated,
    immersiveBorder = NisImmersiveBorder,
    immersiveBorderSubtle = NisImmersiveBorderSubtle,
    immersiveTextPrimary = NisImmersiveTextPrimary,
    immersiveTextSecondary = NisImmersiveTextSecondary,
    immersiveTextMuted = NisImmersiveTextMuted,
    immersiveScrimMedium = NisImmersiveScrimMedium,
    immersiveScrimStrong = NisImmersiveScrimStrong,
    immersivePlayerScrim = NisImmersivePlayerScrim
)

val LocalNisExtraColors = staticCompositionLocalOf { NisDarkExtraColors }
