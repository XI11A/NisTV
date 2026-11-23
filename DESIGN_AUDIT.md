# NisTV Revamped — Comprehensive Visual Design & UI System Audit

> **Target Codebase:** NisTV Revamped (Android / Jetpack Compose)  
> **Repository Path:** `/data/data/com.termux/files/home/NisTV-Revamped`  
> **App Version at Audit:** 1.13.0 (VersionCode: 11300)  
> **Target SDK:** 35 | **Min SDK:** 24 | **Compose Compiler:** Kotlin 2.0.21 Compose Multiplatform / AndroidX Compose  
> **Generated:** November 2026  
> **Audit Status:** Verified directly against repository source code. All code quotes, token values, dimensions, and file references are verbatim.

---

## Table of Contents
1. [Overview: Visual Identity & Design Language](#1-overview-visual-identity--design-language)
2. [Theme Setup & Architecture](#2-theme-setup--architecture)
3. [Colour Tokens, Palettes & Hardcoded Colours](#3-colour-tokens-palettes--hardcoded-colours)
4. [Typography & Type Scale](#4-typography--type-scale)
5. [Shapes, Spacing, Elevation & Borders](#5-shapes-spacing-elevation--borders)
6. [Iconography & Launcher Assets](#6-iconography--launcher-assets)
7. [Component Library Catalogue](#7-component-library-catalogue)
8. [Screen-by-Screen Inventory & Wireframes](#8-screen-by-screen-inventory--wireframes)
9. [Navigation UI & Shared Element Transitions](#9-navigation-ui--shared-element-transitions)
10. [Motion, Micro-Interactions & Animations](#10-motion-micro-interactions--animations)
11. [Imagery, Posters & Media Scrims](#11-imagery-posters--media-scrims)
12. [Player UI & Overlay Systems](#12-player-ui--overlay-systems)
13. [Accessibility, Responsiveness & Contrast](#13-accessibility-responsiveness--contrast)
14. [Brand Assets & Multi-Platform Alignment](#14-brand-assets--multi-platform-alignment)
15. [Design Debt & Visual Inconsistencies](#15-design-debt--visual-inconsistencies)
16. [Redesign Constraints & Safe Edit Plan](#16-redesign-constraints--safe-edit-plan)
17. [Preview & Screenshot Readiness](#17-preview--screenshot-readiness)

---

## 1. Overview: Visual Identity & Design Language

NisTV Revamped exhibits an ultra-dark, high-contrast **"Cinematic Obsidian"** aesthetic designed for living room streaming and late-night mobile consumption. Its dominant canvas colour is an absolute deep obsidian charcoal (`#0B0D12` / `#07090D`), paired with elevated dark-navy card surfaces (`#141821` and `#1C2230`) and crisp 1dp outline borders (`#262D3D`).

The visual signature is driven by a tri-accent palette: electric periwinkle blue (`#6C8CFF`, mislabeled historically in code as `NisAccentGreen`), cyber cyan (`#38BDF8` / `#00F2FE`), and a broadcast live ruby red (`#FF4D5E`) complemented by rating amber (`#F59E0B`). Density is moderately high and mobile-optimised, relying on virtualized horizontal carousels (115dp poster cards, 280dp 16:9 backdrop cards) and adaptive grids. 

The styling is strictly **flat-bordered with localized luminous glow and gradient scrims**; standard Android drop shadows (`elevation` / `Modifier.shadow`) are completely absent across the entire codebase. The app is **dark-only**: no light theme is implemented, and system bar controllers explicitly enforce dark backgrounds with light text and icons (`isAppearanceLightStatusBars = false`).

---

## 2. Theme Setup & Architecture

### 2.1 Implementation Structure
The app's theme is defined in `com.nis.nistv.core.designsystem.theme`. It wraps Google Material 3's `MaterialTheme`.

- **Dark vs. Light Handling:** The app is strictly dark mode. No `lightColorScheme` is declared in the codebase.
- **Dynamic Colour:** Dynamic theming (Material You / Monet on Android 12+) is **not used** or supported.
- **Window Insets & Edge-to-Edge:** `enableEdgeToEdge()` is invoked in `MainActivity.onCreate()` (`app/src/main/java/com/nis/nistv/MainActivity.kt:149`). The root `Scaffold` specifies `contentWindowInsets = WindowInsets(0, 0, 0, 0)`. Status bar and navigation bar insets are managed manually using `.statusBarsPadding()` and `.navigationBarsPadding()`.
- **System Bars:** `Theme.kt` dynamically overrides the window status and navigation bar colours to `NisBackground` (`#0B0D12`) on every render and sets `isAppearanceLightStatusBars = false` and `isAppearanceLightNavigationBars = false`.
- **CompositionLocals:** `LocalSpacing` is defined as a data class instance in `Spacing.kt:18`, but **never** provided via `CompositionLocalProvider`. Navigation and transitions supply `LocalSharedTransitionScope` and `LocalNavAnimatedVisibilityScope`.
- **MaterialTheme integration discrepancy:** In `Theme.kt`, `MaterialTheme` is provided with `colorScheme` and `typography`, but `shapes` parameter is **omitted**, so `NisShapes` (`Shape.kt`) is **never passed** to the MaterialTheme runtime!

### 2.2 Complete Verbatim Theme Files

#### `app/src/main/java/com/nis/nistv/core/designsystem/theme/Color.kt`
```kotlin
package com.nis.nistv.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// Cinematic Dark Palette (Tokens per iptv-ui-redesign)
val NisBackground = Color(0xFF0B0D12)       // App background
val NisSurface = Color(0xFF141821)          // Primary card / bar surface
val NisSurfaceElevated = Color(0xFF1C2230)  // Raised surface / tile container
val NisBorder = Color(0xFF262D3D)           // Subtle border
val NisCardBorder = NisBorder
val NisDivider = NisBorder
val NisCardHover = Color(0xFF22293D)
val NisNavbarBg = Color(0xF50B0D12)
val NisNavbarBorder = NisBorder

// Primary & Accent Brand Tokens
val NisAccentPrimary = Color(0xFF6C8CFF)    // Accent periwinkle blue
val NisAccentPrimaryLight = Color(0xFF8DA7FF)
val NisAccentCyan = Color(0xFF38BDF8)       // Secondary sky accent
val NisAccentAmber = Color(0xFFF59E0B)      // Star / rating amber
val NisLiveRed = Color(0xFFFF4D5E)          // Vibrant live stream badge
val NisLiveEmerald = Color(0xFF10B981)      // Online indicator

// Backwards compatibility tokens
val NisAccentGreen = NisAccentPrimary
val NisAccentGreenPressed = Color(0xFF5B7BE8)
val NisError = Color(0xFFFF4D5E)
val NisWarning = Color(0xFFF59E0B)

// Typography
val NisTextPrimary = Color(0xFFF2F4F8)      // Primary high-contrast text
val NisTextSecondary = Color(0xFF9AA3B5)    // Secondary subtitle / meta text
val NisTextMuted = Color(0xFF6B7280)        // Subtle tertiary text

// Legacy / Style Aliases
val NisCardBackground = NisSurface
val NtvCanvas = NisBackground
val NtvSurface = NisSurface
val NtvSurfaceElevated = NisSurfaceElevated
val NtvCardBorder = NisCardBorder
val NtvAccent = NisAccentPrimary
val NtvCyan = NisAccentCyan
val NtvGold = NisAccentAmber
```

#### `app/src/main/java/com/nis/nistv/core/designsystem/theme/Type.kt`
```kotlin
package com.nis.nistv.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Typography per iptv-ui-redesign token scale:
// Sizes: 12, 14, 16, 20, 28. Weights: 500 (Medium), 600 (SemiBold), 700 (Bold) only.
val NisTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold, // 700
        fontSize = 28.sp,
        lineHeight = 34.sp,
        color = NisTextPrimary
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold, // 700
        fontSize = 20.sp,
        lineHeight = 26.sp,
        color = NisTextPrimary
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold, // 600
        fontSize = 20.sp,
        lineHeight = 26.sp,
        color = NisTextPrimary
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold, // 700
        fontSize = 20.sp,
        lineHeight = 26.sp,
        color = NisTextPrimary
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold, // 600
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = NisTextPrimary
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium, // 500
        fontSize = 14.sp,
        lineHeight = 18.sp,
        color = NisTextSecondary
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium, // 500
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = NisTextPrimary
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium, // 500
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = NisTextSecondary
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium, // 500
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = NisTextMuted
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold, // 600
        fontSize = 14.sp,
        lineHeight = 18.sp,
        color = NisTextPrimary
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium, // 500
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = NisTextSecondary
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold, // 700
        fontSize = 12.sp,
        lineHeight = 14.sp,
        color = NisAccentPrimary
    )
)
```

#### `app/src/main/java/com/nis/nistv/core/designsystem/theme/Shape.kt`
```kotlin
package com.nis.nistv.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val NisShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

val NisSmallControlShape = RoundedCornerShape(8.dp)
val NisButtonShape = RoundedCornerShape(12.dp)
val NisChannelCardShape = RoundedCornerShape(12.dp)
val NisFeaturedCardShape = RoundedCornerShape(16.dp)
val NisChipShape = RoundedCornerShape(999.dp)
val NisBottomSheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
```

#### `app/src/main/java/com/nis/nistv/core/designsystem/theme/Spacing.kt`
```kotlin
package com.nis.nistv.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class NisSpacing(
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 12.dp,
    val standard: Dp = 16.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp,
    val section: Dp = 36.dp
)

val LocalSpacing = NisSpacing()
```

#### `app/src/main/java/com/nis/nistv/core/designsystem/theme/Theme.kt`
```kotlin
package com.nis.nistv.core.designsystem.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = NisAccentPrimary,
    onPrimary = NisBackground,
    primaryContainer = NisSurfaceElevated,
    onPrimaryContainer = NisAccentPrimary,
    secondary = NisAccentPrimaryLight,
    onSecondary = NisBackground,
    background = NisBackground,
    onBackground = NisTextPrimary,
    surface = NisSurface,
    onSurface = NisTextPrimary,
    surfaceVariant = NisSurfaceElevated,
    onSurfaceVariant = NisTextSecondary,
    outline = NisBorder,
    error = NisError,
    onError = NisTextPrimary
)

@Composable
fun NisTvTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NisTypography,
        content = content
    )
}
```

#### `app/src/main/res/values/themes.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.NisTV" parent="android:Theme.Material.NoActionBar">
        <item name="android:statusBarColor">#0B0D12</item>
        <item name="android:navigationBarColor">#0B0D12</item>
        <item name="android:windowBackground">#0B0D12</item>
    </style>

    <style name="Theme.NisTV.Starting" parent="Theme.SplashScreen">
        <item name="windowSplashScreenBackground">#0B0D12</item>
        <item name="windowSplashScreenAnimatedIcon">@drawable/ic_launcher_foreground</item>
        <item name="postSplashScreenTheme">@style/Theme.NisTV</item>
    </style>
</resources>
```

#### `app/src/main/res/values/colors.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="nistv_background">#07090D</color>
    <color name="nistv_surface">#10141C</color>
    <color name="nistv_surface_elevated">#171D27</color>
    <color name="nistv_accent_green">#39D98A</color>
    <color name="nistv_accent_green_pressed">#27B875</color>
    <color name="nistv_divider">#242B36</color>
    <color name="nistv_error">#FF626B</color>
    <color name="nistv_warning">#F4BE58</color>
</resources>
```

*(Note: Configuration folders `values-night/` and `values-v31/` are **UNKNOWN / NOT FOUND** in `app/src/main/res/`.)*

---

## 3. Colour Tokens, Palettes & Hardcoded Colours

### 3.1 Design System Colour Tokens
| Token Name | Hex Value | Alpha | Defined Location | Functional Role | Material colorScheme Slot |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `NisBackground` | `#0B0D12` | 100% | `Color.kt:6` | Main window / screen background | `background` |
| `NisSurface` | `#141821` | 100% | `Color.kt:7` | Card surfaces, bottom bar, top bars | `surface` |
| `NisSurfaceElevated` | `#1C2230` | 100% | `Color.kt:8` | Raised container, tiles, chips, inputs | `surfaceVariant` / `primaryContainer` |
| `NisBorder` | `#262D3D` | 100% | `Color.kt:9` | Card outlines, dividers, chip strokes | `outline` |
| `NisCardBorder` | `#262D3D` | 100% | `Color.kt:10` | Alias to `NisBorder` | `outline` |
| `NisDivider` | `#262D3D` | 100% | `Color.kt:11` | Alias to `NisBorder` | `outline` |
| `NisCardHover` | `#22293D` | 100% | `Color.kt:12` | Interactive card hover/focus tint | Unmapped |
| `NisNavbarBg` | `#0B0D12` | 96% (`0xF5`) | `Color.kt:13` | Semi-transparent bottom navigation | Unmapped |
| `NisNavbarBorder` | `#262D3D` | 100% | `Color.kt:14` | Bottom navigation top outline | `outline` |
| `NisAccentPrimary` | `#6C8CFF` | 100% | `Color.kt:17` | Brand primary periwinkle, primary buttons | `primary` |
| `NisAccentPrimaryLight` | `#8DA7FF` | 100% | `Color.kt:18` | Light periwinkle hover / highlights | `secondary` |
| `NisAccentCyan` | `#38BDF8` | 100% | `Color.kt:19` | Secondary accent, TV guide, HD badges | Unmapped |
| `NisAccentAmber` | `#F59E0B` | 100% | `Color.kt:20` | Star ratings, badges, alert warnings | Unmapped |
| `NisLiveRed` | `#FF4D5E` | 100% | `Color.kt:21` | "LIVE" badge, favorite active heart | `error` |
| `NisLiveEmerald` | `#10B981` | 100% | `Color.kt:22` | Online status dot, healthy stream | Unmapped |
| `NisAccentGreen` | `#6C8CFF` | 100% | `Color.kt:25` | Legacy alias pointing to `NisAccentPrimary` | `primary` |
| `NisAccentGreenPressed` | `#5B7BE8` | 100% | `Color.kt:26` | Darker periwinkle pressed state | Unmapped |
| `NisError` | `#FF4D5E` | 100% | `Color.kt:27` | Error states, delete action | `error` |
| `NisWarning` | `#F59E0B` | 100% | `Color.kt:28` | Warning pills, offline mode badge | Unmapped |
| `NisTextPrimary` | `#F2F4F8` | 100% | `Color.kt:31` | High-contrast body, titles, headings | `onBackground`, `onSurface`, `onError` |
| `NisTextSecondary` | `#9AA3B5` | 100% | `Color.kt:32` | Subtitles, metadata, inactive items | `onSurfaceVariant` |
| `NisTextMuted` | `#6B7280` | 100% | `Color.kt:33` | Captions, placeholders, inactive icons | Unmapped |

### 3.2 Gradient Catalog
| Gradient Name | Type | Direction / Geometry | Color Stops | Where Used |
| :--- | :--- | :--- | :--- | :--- |
| **Splash Top Glow** | Vertical Linear | Top -> Bottom | `#0F1523` -> `#0B0D12` -> `#0B0D12` | `SplashScreen.kt:269` |
| **Logo Squircle Base** | Radial | Center (54, 54), radius 50 | `#221A44` -> `#131028` -> `#07090E` | `NisLogoGeometry.kt:180` |
| **Logo Border Glow** | Linear | (6, 6) -> (102, 102) | `0x8000F2FE` (50%) -> `0x666366F1` (40%) -> `0x26FFFFFF` (15%) | `NisLogoGeometry.kt:189` |
| **Logo Ambient Center** | Radial | Center (54, 54), radius 30 | `0x3338BDF8` (20%) -> `0x1A6366F1` (10%) -> `Color.Transparent` | `NisLogoGeometry.kt:199` |
| **Logo Left Pillar** | Vertical Linear | Y=27 -> Y=81 | `#4338CA` -> `#3730A3` -> `#1E1B4B` | `NisLogoGeometry.kt:209` |
| **Logo Right Pillar** | Vertical Linear | Y=27 -> Y=81 | `#312E81` -> `#4C1D95` -> `#2E1065` | `NisLogoGeometry.kt:219` |
| **Logo Ribbon** | Linear | (30, 27) -> (78, 81) | `#00F2FE` -> `#0EA5E9` -> `#6366F1` -> `#A855F7` | `NisLogoGeometry.kt:241` |
| **Logo Play Core** | Linear | (49, 47) -> (63, 61) | `#FFFFFF` -> `#E0F2FE` -> `#67E8F9` | `NisLogoGeometry.kt:253` |
| **Logo Sheen Sweep** | Linear | (-sweep, -sweep) -> (+sweep, +sweep) | `Transparent` -> `0x14FFFFFF` -> `0x99FFFFFF` -> `0x14FFFFFF` -> `Transparent` | `NisLogoGeometry.kt:294` |
| **Skeleton Shimmer** | Linear | Offset(x-400, y-400) -> Offset(x, y) | `#141821` -> `#1C2230` -> `#232B3E` -> `#1C2230` -> `#141821` | `NisSkeletonLoader.kt:57` |
| **Channel Monogram** | Linear | Top-Left -> Bottom-Right | `NisSurfaceElevated` (`#1C2230`) -> `#232B3E` | `NisChannelCard.kt:356` |
| **Backdrop Card Scrim**| Vertical Linear | Top -> Bottom | `Color.Transparent` -> `Color(0x400B0D12)` -> `Color(0xDD0B0D12)` | `NisBackdropMediaCard.kt:94` |
| **Hero Vignette** | Vertical Linear | Top -> Bottom | `0x990B0D12` -> `0x300B0D12` -> `0xCC0B0D12` -> `#0B0D12` | `HomeScreen.kt:952` |
| **Player Top Scrim** | Vertical Linear | Top -> Bottom | `Color.Black.copy(alpha = 0.85f)` -> `Color.Transparent` | `PlayerScreen.kt:371` |
| **Player Bottom Scrim**| Vertical Linear | Top -> Bottom | `Color.Transparent` -> `Color.Black.copy(alpha = 0.90f)` | `PlayerScreen.kt:479` |
| **Netflix Bar Sweep** | Horizontal Linear| -barWidth -> +barWidth | `Transparent` -> `NisAccentCyan` -> `NisAccentPrimary` -> `Transparent` | `NetflixLoadingScreen.kt:140` |

### 3.3 Hardcoded Colours Outside the Theme
Across `app/src/main/java`, exactly **251** instances of `Color(0x...)` exist across **130** distinct hex values, in addition to **258** usages of standard color constants (`Color.White`, `Color.Black`, `Color.Transparent`).

#### Comprehensive Hardcoded Colour Sample Table
| File and Line | Hardcoded Value | Context |
| :--- | :--- | :--- |
| `core/designsystem/component/NisBadge.kt:100` | `Color(0xFF38BDF8)` | Resolution badge text (should use `NisAccentCyan`) |
| `core/designsystem/component/NisBookmarkButton.kt:65` | `Color(0xCC0B0D12)` | Bookmark button background |
| `core/designsystem/component/NisPosterMediaCard.kt:118` | `Color(0xCC0B0D12)` | Rating pill background |
| `core/designsystem/component/NisSkeletonLoader.kt:61` | `Color(0xFF232B3E)` | Skeleton shimmer specular highlight stop |
| `core/designsystem/component/NisChannelCard.kt:359` | `Color(0xFF232B3E)` | Monogram gradient background |
| `core/designsystem/component/NetflixLoadingScreen.kt:132` | `Color(0xFF1E2538)` | Netflix loading track bar background |
| `core/designsystem/component/GoogleAuthGateDialog.kt:434` | `Color(0xFF4285F4)` | Google 'G' icon blue path |
| `core/designsystem/component/GoogleAuthGateDialog.kt:445` | `Color(0xFF34A853)` | Google 'G' icon green path |
| `core/designsystem/component/GoogleAuthGateDialog.kt:456` | `Color(0xFFFBBC05)` | Google 'G' icon yellow path |
| `core/designsystem/component/GoogleAuthGateDialog.kt:467` | `Color(0xFFEA4335)` | Google 'G' icon red path |
| `core/designsystem/component/AfdsAuthGateDialog.kt:294` | `Color(0xFF0F172A)` | Dialog container surface |
| `core/designsystem/component/AfdsAuthGateDialog.kt:302` | `Color(0xFF1E293B)` | Dialog card elevated surface |
| `core/designsystem/component/AfdsAuthGateDialog.kt:308` | `Color(0xFF10B981)` | Success checkmark green |
| `core/designsystem/component/LanguageOnboardingDialog.kt:101`| `Color(0xFF141F3B)` | Onboarding language icon background |
| `core/designsystem/component/AppUpdateDialog.kt:85` | `Color(0xFF141A2B)` | Update dialog container background |
| `feature/home/HomeScreen.kt:769` | `Color(0xD90B0F19)` | Hero recommendation badge container |
| `feature/home/HomeScreen.kt:938` | `Color(0xFF131A29)` | Media hero fallback gradient stop 1 |
| `feature/home/HomeScreen.kt:939` | `Color(0xFF101522)` | Media hero fallback gradient stop 2 |
| `feature/home/TonightPicksSheet.kt:101` | `Color(0xFFFBBF24)` | Tonight picks gold star badge |
| `feature/player/PlayerScreen.kt:479` | `Color(0xCC000000)` | Player bottom scrim gradient stop |
| `feature/player/PlayerScreen.kt:1115` | `Color(0xFF1E293B)` | Player channel drawer surface |
| `feature/player/components/NisMiniPlayer.kt:256` | `Color(0x33F59E0B)` | Demo countdown pill background |
| `feature/player/components/NisMiniPlayer.kt:262` | `Color(0xFFFBBF24)` | Demo countdown pill text |
| `feature/player/components/LiveEpgCard.kt:151` | `Color(0xFF10B981)` | EPG timeline progress bar fill |
| `feature/player/components/BrightnessVolumeHuds.kt:66` | `Color(0xCC000000)` | Volume / Brightness HUD pill backdrop |
| `feature/details/MediaDetailsScreen.kt:448` | `Color(0xFF131828)` | Details backdrop fallback color |
| `feature/details/MediaDetailsHeader.kt:142` | `Color(0xDD0B0D12)` | Header title scrim background |
| `feature/details/components/InAppTrailerDialog.kt:115` | `Color(0xFF0F1422)` | Trailer dialog frame background |
| `feature/downloads/DownloadsScreen.kt:782` | `Color(0xFF10B981)` | Storage meter free space indicator |
| `feature/downloads/OfflineDetailsScreen.kt:1020` | `Color(0xFF10B981)` | Autopilot active badge |
| `feature/splash/SplashScreen.kt:64` | `Color(0xFF0B0D12)` | Redundant splash background constant |
| `feature/splash/SplashScreen.kt:271` | `Color(0xFF0F1523)` | Splash navy gradient top stop |

---

## 4. Typography & Type Scale

### 4.1 Font Families
- **Bundled Font Files in `app/src/main/res/font/`:** **UNKNOWN / NOT FOUND**. No custom `.ttf` or `.otf` font assets exist in the project.
- **Theme Default:** `FontFamily.Default` (Android system Roboto / sans-serif) applied across all 12 `NisTypography` styles in `Type.kt`.
- **Specialized Hardcoded Font Families:**
  - `FontFamily.SansSerif` used in `NtvLogo.kt:70` for the "TV" brand wordmark.
  - `FontFamily.Monospace` used in `StreamDiagnosticsOverlay.kt:71, 121, 128` for stream telemetry (bitrate, fps, buffer ms, codec).
  - Subtitle styling in `OfflinePlayerTrackDialog.kt:693` supports dynamic user selection among `DEFAULT`, `MONOSPACE`, `SERIF`, and `SANS_SERIF`.

### 4.2 Type Scale Specification
| Style Name | Size | Weight | Line Height | Letter Spacing | Target & Primary Usage |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `headlineLarge` | 28.sp | Bold (700) | 34.sp | Default (0.sp) | Screen hero titles, Splash wordmark (`SplashScreen.kt:316`) |
| `headlineMedium`| 20.sp | Bold (700) | 26.sp | Default (0.sp) | Top bar headings, Section headers (`NisSectionHeader.kt:54`) |
| `headlineSmall` | 20.sp | SemiBold (600) | 26.sp | Default (0.sp) | Modal & Dialog headers (`AfdsAuthGateDialog.kt:390`) |
| `titleLarge` | 20.sp | Bold (700) | 26.sp | Default (0.sp) | Channel name in Player overlay (`PlayerScreen.kt:412`) |
| `titleMedium` | 16.sp | SemiBold (600) | 22.sp | Default (0.sp) | List item titles, Season & episode titles (`EpisodeRowItem`) |
| `titleSmall` | 14.sp | Medium (500) | 18.sp | Default (0.sp) | Secondary titles, `NisBackdropMediaCard` label |
| `bodyLarge` | 16.sp | Medium (500) | 22.sp | Default (0.sp) | Primary body descriptions, Overview paragraphs |
| `bodyMedium` | 14.sp | Medium (500) | 20.sp | Default (0.sp) | Channel card titles, Dialog body text, Empty state body |
| `bodySmall` | 12.sp | Medium (500) | 16.sp | Default (0.sp) | Metadata text, genres, release year, timestamps |
| `labelLarge` | 14.sp | SemiBold (600) | 18.sp | Default (0.sp) | Primary & secondary button labels (`NisButton.kt:68, 107`) |
| `labelMedium` | 12.sp | Medium (500) | 16.sp | Default (0.sp) | Chip text, action pills, category labels |
| `labelSmall` | 12.sp | Bold (700) | 14.sp | Default (0.sp) | Badges, rating counters, uppercase category chips |

### 4.3 Typography Usage Debt
Only **2** places in the entire app reference `MaterialTheme.typography` (`NisBackdropMediaCard.kt:122` uses `MaterialTheme.typography.titleSmall`). The remaining **929** typography call sites write hardcoded `fontSize = X.sp` directly inside composables.

#### Sample Hardcoded Font Sizes & Weights
| File and Line | Hardcoded Size / Weight | Intended Token Equivalent |
| :--- | :--- | :--- |
| `core/navigation/NisNavigation.kt:158` | `fontSize = 11.sp, fontWeight = Medium/Bold` | `labelSmall` (12.sp) |
| `core/designsystem/component/NisBadge.kt:52` | `fontSize = 10.sp, fontWeight = Bold` | Sub-token micro badge (10.sp) |
| `core/designsystem/component/NisBadge.kt:138` | `fontSize = 13.sp, fontWeight = Medium/Bold` | `bodySmall` (12.sp) / `bodyMedium` (14.sp) |
| `core/designsystem/component/NisChannelCard.kt:152` | `fontSize = 15.sp, fontWeight = SemiBold` | `titleMedium` (16.sp) |
| `core/designsystem/component/NisSearchBar.kt:81` | `fontSize = 14.sp, fontWeight = Medium` | `bodyMedium` (14.sp) |
| `core/designsystem/component/NisSectionHeader.kt:54`| `fontSize = 20.sp, letterSpacing = (-0.3).sp` | `headlineMedium` (20.sp) |
| `feature/splash/SplashScreen.kt:328` | `fontSize = 10.5.sp, letterSpacing = 1.4.sp` | Sub-token micro tagline |
| `feature/home/HomeScreen.kt:800` | `fontSize = 24.sp, fontWeight = Bold` | Sub-token hero title (24.sp) |

---

## 5. Shapes, Spacing, Elevation & Borders

### 5.1 Corner Radii Architecture
The app defines formal tokens in `Shape.kt`, but components heavily bypass them in favour of hardcoded `RoundedCornerShape(X.dp)` (398 occurrences).

| Component Category | Formal Token | Actual Radii Used | Inconsistencies / Deviations |
| :--- | :--- | :--- | :--- |
| **Buttons (Primary / Secondary)** | `NisButtonShape` (`12.dp`) | `12.dp` | Consistent in `NisButton.kt`. Dialog text buttons use `4.dp` or `8.dp`. |
| **Chips / Filter Pills** | `NisChipShape` (`999.dp`) | `999.dp` | Consistent in `NisBadge.kt`, `SearchScreen.kt`, and `ProfileScreen.kt`. |
| **Channel Cards (Grid)** | `NisChannelCardShape` (`12.dp`) | `12.dp` | Uses `NisChannelCardShape`. Logo tile inside uses `10.dp`. |
| **Channel Cards (Rail)** | `NisChannelCardShape` (`12.dp`) | `12.dp` | Uses `NisChannelCardShape`. Inner logo box uses `8.dp`. |
| **Media Cards (2:3 Poster)**| `NisShapes.medium` (`12.dp`) | `12.dp` | Hardcoded `RoundedCornerShape(12.dp)` in `NisPosterMediaCard.kt:44`. |
| **Media Cards (16:9 Backdrop)**| `NisFeaturedCardShape` (`16.dp`)| `12.dp` | Hardcoded `RoundedCornerShape(12.dp)` in `NisBackdropMediaCard.kt:53` (token `NisFeaturedCardShape` ignored). |
| **Mini-Player** | `NisChannelCardShape` (`12.dp`) | `12.dp` | Uses `NisChannelCardShape`. Video preview container uses `8.dp`. |
| **Badges / Live Pills** | None (ad-hoc) | `4.dp`, `6.dp` | Resolution badge: `4.dp`; Live badge: `6.dp`; Warning badge: `6.dp`. |
| **Bottom Navigation Bar** | None (ad-hoc) | `topStart = 16.dp, topEnd = 16.dp` | Hardcoded in `NisNavigation.kt:116, 118`. |
| **Bottom Sheets** | `NisBottomSheetShape` (`20.dp`) | `topStart = 20.dp, topEnd = 20.dp` | Hardcoded in sheets (`TonightPicksSheet.kt`, `PlaybackSpeedSheet.kt`); token ignored. |
| **Dialogs (Modals)** | None (ad-hoc) | `16.dp` to `24.dp` | `AfdsAuthGateDialog` uses `20.dp`; `AppUpdateDialog` uses `20.dp`; `ChangelogDialog` uses `18.dp`. |

### 5.2 Spacing Scale
The formal scale is defined in `Spacing.kt:8-16`:
- `extraSmall`: `4.dp`
- `small`: `8.dp`
- `medium`: `12.dp`
- `standard`: `16.dp`
- `large`: `24.dp`
- `extraLarge`: `32.dp`
- `section`: `36.dp`

#### Standard Actual Margins & Paddings Found in UI
- **Screen horizontal padding:** `16.dp` across Home, Discover, Live TV, and Search. Detail screens use edge-to-edge backdrop hero with `20.dp` content horizontal padding. Dialogs use `24.dp` or `32.dp`.
- **Vertical list gaps:** `12.dp` between channel cards in grids; `10.dp` to `12.dp` between items in horizontal rails; `18.dp` to `24.dp` between vertical sections.
- **Card inner padding:** `12.dp` for `NisChannelCard`; `10.dp` for `NisRailChannelCard`; `8.dp` for `NisMiniPlayer`.

### 5.3 Elevation, Shadows, Blurs & Borders
- **Elevation:** Standard elevation is **0.dp** across the entire UI. Material surface elevations are disabled.
- **Drop Shadows:** `Modifier.shadow(...)` is **never called** (0 occurrences).
- **Surface Elevation Paradigm:** Achieved strictly through background color shifting:
  - Base window: `NisBackground` (`#0B0D12`)
  - Standard cards: `NisSurface` (`#141821`)
  - Elevated tiles / active controls: `NisSurfaceElevated` (`#1C2230`)
- **Borders & Strokes:** Universally 1.dp with `NisBorder` (`#262D3D`). Rating badges use `0.5.dp`. Focus or active states use 1.dp `NisAccentPrimary` (`#6C8CFF`).

---

## 6. Iconography & Launcher Assets

### 6.1 `NisIcons` Catalogue
All application icons are defined in `app/src/main/java/com/nis/nistv/core/designsystem/icon/NisIcons.kt`. The catalogue contains exactly **51** custom vector drawables programmatically generated via `ImageVector.Builder`.

- **Conventions:** `defaultWidth = 24.dp`, `defaultHeight = 24.dp`, `viewportWidth = 24f`, `viewportHeight = 24f`.
- **Fill:** SolidColor `Color.White`, tinted dynamically via composable callers. (`Star` has a hardcoded default fill of `Color(0xFFF59E0B)`).

| Icon Name | Style | Visual Representation | Key Screen Usage |
| :--- | :--- | :--- | :--- |
| `Home` | Filled | House silhouette | Bottom Navigation Bar |
| `LiveTv` | Outlined | TV receiver with antennae | Bottom Nav, Live TV tab, EPG Guide |
| `Search` | Outlined | Magnifying glass | Bottom Nav, Search Bar, Header quick action |
| `Favorites` | Filled | Solid heart | Channel card favorited, My List |
| `FavoritesBorder` | Outlined | Outlined heart | Channel card unfavorited, Home top bar |
| `Profile` | Filled | Person silhouette in circle | Bottom Nav, Profile / Settings screen |
| `Settings` | Outlined | Mechanical gear | Profile options |
| `Play` | Filled | Right-pointing triangle ▶ | MiniPlayer, Buttons, Video overlay |
| `Pause` | Filled | Double vertical bars ⏸ | MiniPlayer, Player controls |
| `Back` | Outlined | Left arrow ← | Top bars, Player back, Details back |
| `Collapse` | Outlined | Downward chevron ⌄ | Drawer collapse, Sheet dismiss |
| `Close` | Outlined | Cross mark ✕ | Clear search, MiniPlayer close, Dialog close |
| `Fullscreen` | Outlined | Four outward corner brackets ⛶ | Player portrait to landscape |
| `FullscreenExit` | Outlined | Four inward corner brackets | Player landscape to portrait |
| `OpenInNew` | Outlined | Box with top-right arrow ↗ | External links, TMDB attribution |
| `VolumeUp` | Outlined | Speaker with two wave arcs | Volume gesture HUD, Player controls |
| `VolumeMute` | Outlined | Speaker with zero arcs | Mute HUD |
| `Refresh` | Outlined | Circular counter-clockwise arrow ⟳ | Retry buttons, EPG refresh |
| `SkipNext` | Filled | Right triangle with end bar ⏭ | Next channel, next episode |
| `SkipPrevious` | Filled | Left triangle with start bar ⏮ | Previous channel |
| `AspectRatio` | Outlined | Rectangular frame with diagonal arrows | Player aspect ratio toggle (FIT/ZOOM/16:9) |
| `PictureInPicture`| Outlined | Screen with embedded mini-screen | PiP trigger button |
| `Info` | Outlined | Circle with lowercase "i" ⓘ | Details action, Error states |
| `FilterList` | Outlined | Three descending horizontal bars ⚞ | Live TV reset filter, Category menus |
| `Check` | Outlined | Checkmark ✓ | Dialog confirmations, Selected tracks |
| `Brightness` | Outlined | Eight-point sun burst ☼ | Brightness gesture HUD |
| `Lock` | Filled | Closed padlock | Locked adult channels, Parental controls |
| `LockOpen` | Outlined | Open padlock | Unlocked state |
| `Timer` | Outlined | Stopwatch with top pusher ⏱ | Sleep timer sheet |
| `Discover` | Outlined | Compass dial 🧭 | Bottom Navigation Bar |
| `Download` | Outlined | Cloud with downward arrow ⤓ | Bottom Nav, Details download button |
| `DownloadDone` | Outlined | Checkmark on horizontal tray ✓ | Completed download indicator |
| `Save` | Outlined | 3.5-inch floppy disk 💾 | Backup & Restore action |
| `Delete` | Outlined | Trash can with lid 🗑 | Delete download, Clear search history |
| `Star` | Filled | 5-point star ★ (`#F59E0B`) | TMDB rating badge |
| `Bookmark` | Filled | Solid ribbon bookmark | Watch Later saved state |
| `BookmarkBorder`| Outlined | Outlined ribbon bookmark | Watch Later unsaved state |
| `MoreVert` | Filled | Three vertical dots ⋮ | Card options, Continue Watching menu |
| `Subtitles` | Outlined | Rectangular TV screen with subtitle lines | Track selector subtitle tab |
| `Audiotrack` | Outlined | Musical note ♪ | Track selector audio language tab |
| `Tune` | Outlined | Three slider adjustment toggles 🎚 | Subtitle customizer, Diagnostics |
| `Cast` | Outlined | Screen with Wi-Fi radiating from corner | Chromecast / DLNA screencast dialog |
| `CastConnected` | Outlined | Screen with connected Wi-Fi arcs | Active screencast session |
| `Add` | Outlined | Plus sign ＋ | Add playlist, Add custom M3U stream |
| `Playlists` | Outlined | Stacked horizontal list with play arrow | My Playlists screen |
| `Alarm` | Outlined | Twin-bell alarm clock ⏰ | EPG program reminder |
| `AlarmOn` | Outlined | Twin-bell clock with checkmark | Active program reminder |
| `Notifications` | Outlined | Bell silhouette 🔔 | System notifications toggle |
| `NotificationsActive`| Outlined | Ringing bell with vibration arcs | Active episode radar alert |
| `MultiView` | Outlined | Split screen dual-pane rectangle ◫ | Player 2-4 channel multi-view mode |
| `Headphones` | Outlined | Over-ear audio headphones 🎧 | Background audio-only listen mode |

### 6.2 Launcher Icon Setup
Configured in `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml`:
- **Background (`@drawable/ic_launcher_background`):** 108dp x 108dp vector with deep obsidian base `#07090E`, central ambient cosmic violet glows `#1E193C` / `#251D4A`, and concentric halo rings `#1E2A47` / `#131B2E`.
- **Foreground (`@drawable/ic_launcher_foreground`):** 108dp x 108dp vector with ambient cyan aura `#2638BDF8`, sculpted 3D 'N' pillars (`#4338CA` / `#312E81`), sweeping cyan-to-purple diagonal ribbon (`#00F2FE`), play core (`#FFFFFF`), and live ruby beacon dot (`#FF4D5E`).
- **Monochrome Themed Icon (Android 13+):** **UNKNOWN / NOT FOUND**. No `<monochrome>` drawable layer is configured.
- **Legacy Bitmaps:** Pre-rendered mipmaps exist in `mdpi`, `hdpi`, `xhdpi`, `xxhdpi`, and `xxxhdpi` folders.

### 6.3 Notification Icons
- **Media Playback:** System media style notification managed by Media3 `PlaybackService`. Small icon defaults to application icon `@mipmap/ic_launcher`.
- **Download Engine:** Uses platform drawables `android.R.drawable.stat_sys_download` (in-progress) and `android.R.drawable.stat_sys_download_done` (completed).
- **Reminders & Autopilot:** Uses `@mipmap/ic_launcher`.

---

## 7. Component Library Catalogue

### 7.1 Buttons & Action Controls
#### `NisPrimaryButton`
- **File:** `app/src/main/java/com/nis/nistv/core/designsystem/component/NisButton.kt:36`
- **Signature:**
  ```kotlin
  fun NisPrimaryButton(
      text: String,
      onClick: () -> Unit,
      modifier: Modifier = Modifier,
      icon: ImageVector? = null,
      enabled: Boolean = true
  )
  ```
- **Visuals:** Height 44.dp min; container `NisAccentPrimary` (`#6C8CFF`), content `NisBackground` (`#0B0D12`). Disabled: container `NisSurfaceElevated`, content `NisTextPrimary` 38% alpha. Shape: `NisButtonShape` (12.dp). Padding: horizontal 20.dp, vertical 10.dp. Icon size: 18.dp. Typography: 14.sp Bold.
- **Used In:** Home hero, Discover hero, Error states, Empty states, Dialogs, Auth gate.

#### `NisSecondaryButton`
- **File:** `app/src/main/java/com/nis/nistv/core/designsystem/component/NisButton.kt:75`
- **Signature:**
  ```kotlin
  fun NisSecondaryButton(
      text: String,
      onClick: () -> Unit,
      modifier: Modifier = Modifier,
      icon: ImageVector? = null,
      enabled: Boolean = true
  )
  ```
- **Visuals:** Height 44.dp min; container `NisSurfaceElevated` (`#1C2230`), border 1dp `NisBorder` (`#262D3D`), content `NisTextPrimary` (`#F2F4F8`). Shape: `NisButtonShape` (12.dp). Padding: horizontal 18.dp, vertical 10.dp. Typography: 14.sp SemiBold.
- **Used In:** Home hero "Details", Discover "Load More", Error state secondary action, Dialog cancel buttons.

#### `NisIconButton`
- **File:** `app/src/main/java/com/nis/nistv/core/designsystem/component/NisButton.kt:114`
- **Signature:**
  ```kotlin
  fun NisIconButton(
      icon: ImageVector,
      contentDescription: String,
      onClick: () -> Unit,
      modifier: Modifier = Modifier,
      tint: Color = NisTextPrimary,
      backgroundColor: Color = Color.Transparent
  )
  ```
- **Visuals:** Size 44.dp x 44.dp min touch target; icon 22.dp; `indication = null` (no ripple feedback).
- **Used In:** Player overlay controls, top navigation headers.

#### `NisBookmarkButton`
- **File:** `app/src/main/java/com/nis/nistv/core/designsystem/component/NisBookmarkButton.kt:34`
- **Signature:**
  ```kotlin
  fun NisBookmarkButton(
      mediaType: String,
      mediaId: Long,
      title: String,
      posterPath: String? = null,
      backdropPath: String? = null,
      overview: String? = null,
      releaseYear: String? = null,
      voteAverage: Double? = null,
      genres: String? = null,
      modifier: Modifier = Modifier,
      size: Dp = 32.dp,
      iconSize: Dp = 18.dp,
      showBackground: Boolean = true,
      showToastFeedback: Boolean = true,
      onToggle: ((Boolean) -> Unit)? = null
  )
  ```
- **Visuals:** Size 32.dp (default) circular button (`CircleShape`). Background: unselected `Color(0xCC0B0D12)` with `NisBorder`; selected `NisAccentPrimary.copy(alpha = 0.18f)` with `NisAccentPrimary` border. Icon animates between `BookmarkBorder` (`#F2F4F8`) and `Bookmark` (`#6C8CFF`).
- **Used In:** `NisPosterMediaCard` (top-start corner), Details screen header.

### 7.2 Chips, Badges & Indicators
#### `NisCategoryChip`
- **File:** `app/src/main/java/com/nis/nistv/core/designsystem/component/NisBadge.kt:111`
- **Visuals:** Height 44.dp min; pill shape `NisChipShape` (`999.dp`). Selected: background `NisAccentPrimary` (`#6C8CFF`), text `NisBackground` (`#0B0D12`), weight Bold. Unselected: background `NisSurfaceElevated` (`#1C2230`), border 1dp `NisBorder`, text `NisTextSecondary` (`#9AA3B5`), weight Medium.
- **Used In:** Top navigation category filters (Home, Discover, Live TV, Search, Profile).

#### `NisLiveBadge`
- **File:** `app/src/main/java/com/nis/nistv/core/designsystem/component/NisBadge.kt:59`
- **Visuals:** Rounded rectangle (6.dp radius), background `NisLiveRed.copy(0.16f)`, border 1dp `NisLiveRed.copy(0.5f)`. Left 6.dp solid red dot (`CircleShape`) + "LIVE" text (10.sp Bold, letterSpacing 0.5.sp).
- **Used In:** Live TV channel cards, MiniPlayer video corner, Player overlay.

#### `NisResolutionBadge`
- **File:** `app/src/main/java/com/nis/nistv/core/designsystem/component/NisBadge.kt:87`
- **Visuals:** Rounded rectangle (4.dp radius), background `NisSurfaceElevated`, border 1dp `NisBorder`, text `Color(0xFF38BDF8)` (10.sp Bold uppercase, e.g. "FHD", "4K", "HD").
- **Used In:** `NisChannelCard` header.

#### `NisBadge` & `NisWarningBadge`
- **File:** `app/src/main/java/com/nis/nistv/core/designsystem/component/NisBadge.kt:36, 145`
- **Visuals:** Micro-pill 4.dp / 6.dp radius. `NisBadge`: customizable surface and border. `NisWarningBadge`: background `NisWarning.copy(0.15f)`, border `NisWarning.copy(0.4f)`, text `NisWarning` (`#F59E0B`, 10.sp SemiBold).
- **Used In:** Stream quality warnings, offline mode alerts.

#### `NisLoadingIndicator`
- **File:** `app/src/main/java/com/nis/nistv/core/designsystem/component/NisLoadingIndicator.kt:16`
- **Visuals:** Full-screen centered `CircularProgressIndicator`, size 40.dp (stroke 3.dp), color `NisAccentGreen` (`#6C8CFF`), trackColor `NisDivider` (`#262D3D`).

### 7.3 Cards & Media Components
#### `NisChannelCard`
- **File:** `app/src/main/java/com/nis/nistv/core/designsystem/component/NisChannelCard.kt:64`
- **Visuals:** Container `NisSurface` (`#141821`), border 1dp `NisBorder`, radius `NisChannelCardShape` (`12.dp`), padding 12.dp.
  - Header row: 46.dp logo tile (or monogram fallback), lock icon if restricted, resolution badge, 44.dp favorite heart toggle.
  - Title: 15.sp SemiBold (`NisTextPrimary`), 1 line ellipsis.
  - Subtitle: reliability status dot (if active), category (`NisTextSecondary` 12.sp) + " • " + language (`NisTextMuted` 12.sp).
- **Used In:** Live TV grid, Search channel results, Favorites channel tab.

#### `NisRailChannelCard`
- **File:** `app/src/main/java/com/nis/nistv/core/designsystem/component/NisChannelCard.kt:205`
- **Visuals:** Width 148.dp, container `NisSurface`, radius 12.dp, padding 10.dp. Upper logo box: full width x 84.dp height, container `NisSurfaceElevated`, border 1dp `NisBorder`, radius 8.dp. Title 13.sp SemiBold; Subtitle 11.sp Medium.
- **Used In:** Home screen horizontal live channel rails.

#### `NisPosterMediaCard`
- **File:** `app/src/main/java/com/nis/nistv/core/designsystem/component/NisPosterMediaCard.kt:36`
- **Visuals:** Width 115.dp (or fillMaxWidth in 3-column grids), aspect ratio 2:3 (height 165.dp if fixed). Radius 12.dp, background `NisSurface`, border 1dp `NisBorder`. Top-start 28.dp bookmark button; top-end rating pill (★ 8.2 in `#F59E0B`). Bottom title 12.sp Medium, release year 11.sp. Supports shared element transition keys for `CONTAINER`, `IMAGE`, and `TITLE`.
- **Used In:** Home VOD rails, Discover grid, Search results, Watch Later list.

#### `NisBackdropMediaCard`
- **File:** `app/src/main/java/com/nis/nistv/core/designsystem/component/NisBackdropMediaCard.kt:46`
- **Visuals:** Width 280.dp, aspect ratio 16:9. Radius 12.dp, background `NisSurface`. Full-bleed backdrop image with bottom 3-stop vertical gradient scrim (`Transparent` -> `0x400B0D12` -> `0xDD0B0D12`). Top-end rating pill. Bottom title `titleSmall` SemiBold (White, max 2 lines).
- **Used In:** Home featured media rails, trending carousels.

#### `NisMiniPlayer`
- **File:** `app/src/main/java/com/nis/nistv/feature/player/components/NisMiniPlayer.kt:75`
- **Visuals:** Docked floating bar above bottom navigation. Margins: horizontal 8.dp, vertical 4.dp. Height 64.dp. Radius `NisChannelCardShape` (12.dp), container `NisSurface`, border 1dp `NisBorder`.
  - Left preview: 88.dp x 50.dp 16:9 video surface running live `ExoPlayer` (or channel logo fallback), micro "LIVE" badge, buffering spinner.
  - Middle: Channel name (14.sp SemiBold) + Programme / Category (12.sp Medium in `NisAccentPrimary`).
  - Right: Demo time chip (if applicable), 44.dp Play/Pause button, 44.dp Close button.
  - Bottom: 2.dp full-width accent line in `NisAccentPrimary`.
  - Motion: Horizontal drag gesture (>90dp threshold) triggers dismiss animation. Tap expands to `PlayerScreen`.

### 7.4 Headers, Bars & Search
#### `NisSearchBar`
- **File:** `app/src/main/java/com/nis/nistv/core/designsystem/component/NisSearchBar.kt:43`
- **Visuals:** Height 48.dp, radius 12.dp, container `NisSurface`, border 1dp `NisBorder`, padding horizontal 14.dp. Leading 20.dp search icon (`NisTextSecondary`). `BasicTextField` with 14.sp Medium text, cursor in `NisAccentPrimary`. Trailing 44.dp clear '✕' button.
- **Used In:** Discover top bar, Live TV sticky search, Search screen.

#### `NisSectionHeader`
- **File:** `app/src/main/java/com/nis/nistv/core/designsystem/component/NisSectionHeader.kt:35`
- **Visuals:** Padding: horizontal 16.dp, vertical 8.dp. Title: 20.sp Bold (`NisTextPrimary`), letterSpacing -0.3.sp. Optional subtitle: 12.sp Medium (`NisTextSecondary`). Trailing optional action text (13.sp SemiBold in `NisAccentPrimary`) and 16.dp icon.
- **Used In:** Home sections, Discover rails, Search results.

#### `NisBottomNavigationBar`
- **File:** `app/src/main/java/com/nis/nistv/core/navigation/NisNavigation.kt:104`
- **Visuals:** Height 60.dp + `navigationBarsPadding()`. Top border 1dp `NisBorder`. Rounded top corners (16.dp). Background `NisSurface` (`#141821`).
  - 5 Navigation items: Home, Discover, Live TV, Downloads, Profile.
  - Each item: 56.dp height column, 22.dp icon + 11.sp text (3.dp top padding). Selected: `NisAccentPrimary` (`#6C8CFF`), Bold. Unselected: `NisTextMuted` (`#6B7280`), Medium.

### 7.5 States, Skeletons & Feedback
#### `NisEmptyState` & `NisErrorState`
- **Files:** `app/src/main/java/com/nis/nistv/core/designsystem/component/NisEmptyState.kt:33`, `NisErrorState.kt:39`
- **Visuals:** Full-screen column, padding 32.dp.
  - Circle container: 72.dp x 72.dp (`CircleShape`). Empty: `NisSurfaceElevated` with 32.dp icon (`NisTextSecondary`). Error: `NisError.copy(0.12f)` background, `NisError.copy(0.35f)` border, 32.dp `NisError` icon.
  - Title: 18.sp Bold (`NisTextPrimary`).
  - Body: 14.sp Regular (`NisTextSecondary`), line height 20.sp.
  - Action button: Primary "Retry" button (with refresh icon) or custom secondary action.

#### Skeletons (`NisSkeletonLoader.kt`)
- **Shimmer:** 1200ms `LinearEasing` sweep from `translateAnim - 400f` to `translateAnim`. Stops: `#141821` -> `#1C2230` -> `#232B3E` -> `#1C2230` -> `#141821`.
- **Variants:** `ChannelCardSkeleton` (grid card layout), `ChannelRailCardSkeleton` (horizontal card), `ChannelRailSkeleton` (rail title + 4 cards), `ChannelGridSkeleton` (adaptive grid with 6 cards), `HeroBannerSkeleton` (340dp billboard).

### 7.6 Dialogs & Modals
- **`AfdsAuthGateDialog`:** Full-screen `DialogProperties(usePlatformDefaultWidth = false)` modal. Surface `Color(0xFF0F172A)`, cards `Color(0xFF1E293B)`. Supports email/Telegram OTP sign-in, Google Sign-In, Google Web OAuth dialog, and two-step sign-up.
- **`AppUpdateDialog`:** Container `Color(0xFF141A2B)` with 20.dp radius and `NisAccentPrimary` border. Features release tag, changelog bullet list, and download progress meter.
- **`ChangelogDialog`:** Container `NisSurface` with 18.dp radius. Categorized badges: `Added` (Emerald), `Fixed` (Sky), `Security` (Ruby), `Changed` (Amber).
- **`LanguageOnboardingDialog`:** First-launch language picker dialog. 2-column grid of 6 Indian languages (Tamil, Hindi, Telugu, Malayalam, Kannada, English) with native script subtitles.
- **`TonightPicksSheet`:** `ModalBottomSheet` with `NisBottomSheetShape` (20.dp top radius) and `NisSurface` container. Displays curations for evening viewing.
- **`TrackSelectorBottomSheet`:** `ModalBottomSheet` with three tabs: Audio Tracks, Subtitle Tracks, and Video Quality Streams.

---

## 8. Screen-by-Screen Inventory & Wireframes

### 8.1 Splash Screen (`Screen.Splash`)
```text
+------------------------------------------+
|                                          |
|                                          |
|          [ NisAnimatedSvgLogo ]          |
|                 (124dp)                  |
|            (Breathing Pulse)             |
|                                          |
|                 NisTV                    |
|    NEXT-GEN ENTERTAINMENT & STREAMING    |
|                                          |
|         (Conditional Spinner)            |
|                                          |
+------------------------------------------+
```
- **Composables:** `NisAnimatedSvgLogo`, `Text` ("NisTV", 28.sp Bold), `Text` (Tagline, 10.5.sp SemiBold), `CircularProgressIndicator` (only if boot > 2300ms).
- **Background:** Solid `#0B0D12` base with top gradient settling from `#0F1523` over 350ms.
- **Scroll:** None (fixed full screen).
- **Motion:** 1900ms build timeline, 280ms exit fade + scale up (1.0 -> 1.06). Sonic audio played on `STREAM_MUSIC`.

### 8.2 Home Screen (`Screen.Home`)
```text
+------------------------------------------+
| [NisLogo] NisTV    ✨ [Search] [Profile] | <-- CinematicTopBar (Floating)
| [All] [Movies] [Series] [Live TV] [News] | <-- Filter Chips (LazyRow)
+------------------------------------------+
|                                          |
|       [ RecommendedHeroCarousel ]        | <-- HorizontalPager (16:9 Backdrop)
|      "BECAUSE YOU WATCHED {TITLE}"       |
|    Title | MOVIE | 2024 | ★ 8.4          |
|    [ ▶ Watch Now ]  [ ⓘ Details ]        |
|               ••••                       |
|                                          |
| Continue Watching                        |
| +------+ +------+ +------+               | <-- LazyRow (115x165dp + Progress)
| |Poster| |Poster| |Poster|               |
| +------+ +------+ +------+               |
|                                          |
| Trending Live Channels                   |
| +---------+ +---------+                  | <-- LazyRow (148x120dp Rail Cards)
| |Logo Tile| |Logo Tile|                  |
| +---------+ +---------+                  |
|                                          |
| Trending Movies                          |
| +-------------------+ +----------------+ | <-- LazyRow (280x157dp Backdrop Cards)
| | 16:9 Backdrop     | | 16:9 Backdrop  | |
| +-------------------+ +----------------+ |
|                                          |
+------------------------------------------+
| [Home]  [Discover] [Live TV] [Downloads] | <-- NisBottomNavigationBar (60dp)
+------------------------------------------+
```
- **Scroll Behaviour:** Vertical `LazyColumn`. Content padding: bottom 90.dp.
  - Hero carousel: `HorizontalPager` with auto-scroll delay (6500ms).
  - Horizontal rails: `LazyRow` with `spacedBy(10.dp)` or `spacedBy(12.dp)`.
- **Loading:** `HeroBannerSkeleton` (340dp) + two `ChannelRailSkeleton` rails.

### 8.3 Discover Screen (`Screen.Discover`)
```text
+------------------------------------------+
| Discover            [Movies] [Series]    | <-- DiscoverTopHeader
| [  🔍 Search titles, genres...         ] | <-- NisSearchBar (48dp)
| [All] [Action] [Drama] [Comedy] [Sci-Fi] | <-- Genre Filter Chips (LazyRow)
+------------------------------------------+
|                                          |
|  (Default Hub Mode)                      |
|  [ DiscoverHeroBanner (380dp) ]          | <-- Full-bleed Hero
|                                          |
|  Recommended For You                     |
|  +-----+ +-----+ +-----+ +-----+         | <-- LazyRow (115x165dp Poster Cards)
|  |     | |     | |     | |     |         |
|  +-----+ +-----+ +-----+ +-----+         |
|                                          |
|  Top Action Movies                       |
|  +-----+ +-----+ +-----+ +-----+         | <-- LazyRow (115x165dp Poster Cards)
|  |     | |     | |     | |     |         |
|  +-----+ +-----+ +-----+ +-----+         |
|                                          |
|  -- OR (Search / Genre Grid Mode) --     |
|  +-----+ +-----+ +-----+                 | <-- LazyVerticalGrid (3 Columns Fixed)
|  |Card | |Card | |Card |                 |
|  +-----+ +-----+ +-----+                 |
|                                          |
+------------------------------------------+
| [Home]  [Discover] [Live TV] [Downloads] |
+------------------------------------------+
```
- **Scroll Behaviour:** Mode 1: `LazyColumn` for rails. Mode 2: `LazyVerticalGrid` (3 fixed columns, `spacedBy(10.dp)` horizontal, `spacedBy(14.dp)` vertical).

### 8.4 Live TV Screen (`Screen.LiveTv`)
```text
+------------------------------------------+
| Browse Live TV             [EPG] [ + ]   | <-- Header (Title + Guide/Playlist)
| 1,248 channels                           |
| [  🔍 Filter by name, genre, lang...   ] | <-- NisSearchBar (48dp)
| [All] [My Playlists] [News] [Entertainment]| Filter Chips
| [Region: India] [Region: US] [Tamil]     | Sub-filter Chips
+------------------------------------------+
| +------------------+ +-----------------+ |
| | [Logo]  [FHD] [♡]| | [Logo]  [HD] [♡]| | <-- LazyVerticalGrid
| | Star Vijay       | | Sun TV          | |     (Adaptive minSize = 160dp)
| | Entertainment    | | General         | |     (~2 cols mobile, 3-4 tablet)
| +------------------+ +-----------------+ |
| +------------------+ +-----------------+ |
| | [Logo]  [HD]  [♡]| | [Logo]  [FHD] [♡]| |
| +------------------+ +-----------------+ |
+------------------------------------------+
| [Home]  [Discover] [Live TV] [Downloads] |
+------------------------------------------+
```
- **Scroll Behaviour:** `LazyVerticalGrid(GridCells.Adaptive(minSize = 160.dp))`, gaps `12.dp`, bottom padding `96.dp`.
- **Loading:** `ChannelGridSkeleton` (6 cards).
- **Empty State:** `NisEmptyState` with filter reset action.

### 8.5 Downloads Screen (`Screen.Downloads`)
```text
+------------------------------------------+
| Downloads       [● Online]  [ + URL ]    | <-- Header + Network badge
+------------------------------------------+
| [ Storage Meter Card ]                   |
| 12.4 GB used • 45.2 GB free              |
| [===================--------------]      |
|                                          |
| ACTIVE DOWNLOADS (1)                     |
| +--------------------------------------+ |
| | [Poster] Inception                   | | <-- ActiveDownloadCard
| | 45% • 1.2 MB/s • ETA: 4m             | |
| | [========--------------------------] | |
| +--------------------------------------+ |
|                                          |
| MOVIES (3)                               |
| +--------------------------------------+ |
| | [Poster] Interstellar (2014)     [⋮] | | <-- MovieDownloadCard
| | 1080p • 2.4 GB • [ ▶ Watch ]         | |
| +--------------------------------------+ |
|                                          |
| TV SERIES (2)                            |
| +--------------------------------------+ |
| | [Poster] Breaking Bad            [⋮] | | <-- SeriesDownloadCard
| | 3 Seasons • 18 Episodes • 8.6 GB     | |
| +--------------------------------------+ |
+------------------------------------------+
| [Home]  [Discover] [Live TV] [Downloads] |
+------------------------------------------+
```
- **Scroll Behaviour:** Vertical `LazyColumn`, content padding bottom 96.dp.

### 8.6 Offline Details Screen (`Screen.OfflineDetails`)
```text
+------------------------------------------+
| [ ← Back ]                     [ ⋮ More ]| <-- Floating Top Bar
| [ Full-Bleed 16:9 Backdrop Image       ] |
|                                          |
| [Poster]  Title of Movie / Series        | <-- Details Header
| (2:3)     Year • Rating • Storage Size   |
|           [ ▶ Play All ]  [ Autopilot ]  |
|                                          |
| Synopsis / Overview text...              |
|                                          |
| Season 1 (10 Episodes)                   |
| +--------------------------------------+ |
| | E01 • Pilot                      [▶] | | <-- OfflineEpisodeRow
| | 45m • 420 MB • 1080p                 | |
| +--------------------------------------+ |
| | E02 • Cat's in the Bag           [▶] | |
| +--------------------------------------+ |
+------------------------------------------+
```
- **Scroll Behaviour:** Vertical `LazyColumn`.

### 8.7 Media Details Screen (`Screen.MediaDetails`)
```text
+------------------------------------------+
| [ ← Back ]                               | <-- Overlay Back (Z-index 4)
| [ Hero Backdrop Image (Height 310dp)   ] |
|                                          |
| [Poster]  Title of Movie / TV Show       | <-- MediaDetailsHeader (Shared Transform)
| (115x165) Year • Rating • Genres         |
|           [ ▶ Watch Trailer ]            |
|                                          |
| [ ⤓ Download ]   [ 🔖 Watch Later ]      | <-- Primary CTAs
|                                          |
| Overview & Storyline...                  |
|                                          |
| Top Billed Cast                          |
| (○) Actor 1   (○) Actor 2   (○) Actor 3  | <-- LazyRow (CastRailSection)
|                                          |
| Where to Watch Live Channels             |
| [Logo] Star Movies • Playing in 15m      | <-- WhereToWatchSection
|                                          |
| More Like This                           |
| [Poster]  [Poster]  [Poster]  [Poster]   | <-- Similar Titles Rail
+------------------------------------------+
```
- **Scroll Behaviour:** Vertical `LazyColumn`.
- **Transitions:** Full shared element bounds for container, image, and title from originating cards.

### 8.8 Live Player Screen (`Screen.Player`)
```text
+------------------------------------------+
| [←] Sun TV HD       [Cast] [Multi] [⚙]   | <-- Top Overlay (Gradient Scrim)
|     Tamil • 1080p • 60fps                |
|                                          |
|  [Vol HUD]               [Bright HUD]    | <-- Left/Right Gesture HUDs
|                                          |
|         [ ◀◀ ]   [ ▶ / ⏸ ]   [ ▶▶ ]      | <-- Center Controls (Tap reveal)
|                                          |
| [LIVE] • 4.2 MB/s • EPG: Evening News    | <-- Bottom Overlay (Gradient Scrim)
| [ 🗂 Quick Drawer ]   [ ⏱ Sleep ]   [⛶]   |
+------------------------------------------+
| <--- ChannelQuickDrawer (Slide from Right) |
+------------------------------------------+
```
- **Scroll Behaviour:** Fixed surface with immersive gestures (drag brightness on left half, drag volume on right half, double tap seek). Overlays fade out after 4000ms idle.

### 8.9 Offline Player Screen (`Screen.OfflinePlayer`)
- Similar layout to Live Player, with custom seekable scrubber bar, dual-subtitle overlay renderer, track selector dialog, sleep timer, and auto-play next episode popup card.

### 8.10 Search Screen (`Screen.Search`)
- Top title + search input bar + horizontal quick-filter pills (popular genres/languages) + search results (split into "Channels" grid and "Movies & Series" 3-column poster grid).

### 8.11 Favorites Screen (`Screen.Favorites`)
- Top tabs: "Live Channels" and "Watch Later" (Movies & Series). Channels render in 2-column adaptive grid; media items render in 3-column 2:3 poster grid.

### 8.12 Profile & Settings Screen (`Screen.Profile`)
- Vertical scrolling list: User Profile / AFDS Account card, Language selector chips, Data Saver toggle & meter, Live TV / Playlist link cards, History & Privacy, Parental Controls PIN, Backup & Restore, Subtitle appearance customization, Cache clearing, Version info.

### 8.13 TV Guide Screen (`Screen.Guide`)
- Date picker tab row, channel list column on left, horizontal time slots on right displaying programme titles and times.

### 8.14 Playlists Screen (`Screen.Playlists`)
- Custom M3U playlist cards with channel counts, sync status, edit, rename, and delete actions. Floating Action Button (+) to add new playlist URL.

### 8.15 History & Privacy Screen (`Screen.HistoryPrivacy`)
- Tabs: "Channel Watch History" and "Downloaded History", with search, clear all history button, and individual delete buttons.

### 8.16 Parental Controls Screen (`Screen.ParentalControls`)
- 4-digit PIN setup/change, Master toggle, Category restriction switches (Adult, News, Movies, Kids), and temporary unlock timer options.

### 8.17 Backup & Restore Screen (`Screen.BackupRestore`)
- Card for exporting configuration, playlists, favorites, and history to a JSON backup file, and import card to restore.

### 8.18 Multi-View Screen (`feature/player/multiview/MultiViewScreen.kt`)
- 2, 3, or 4 simultaneous video tiles in a grid with independent audio focus switcher, mute toggles, and channel selector popup.

---

## 9. Navigation UI & Shared Element Transitions

### 9.1 Bottom Navigation Bar
- **Visibility:** Visible on top-level routes (`home`, `discover`, `livetv`, `downloads`, `profile`). Hidden on `splash`, `player/*`, `offline_player/*`, `details/*`, `offline_details/*`, `guide`, `playlists`, `history_privacy`, `parental_controls`, and `backup_restore`.
- **Dimensions:** Height 60.dp + system navigation bar padding. Top border 1.dp `NisBorder`. Clip: `RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)`.
- **Colours:** Container `NisSurface` (`#141821`). Selected item: `NisAccentPrimary` (`#6C8CFF`). Unselected item: `NisTextMuted` (`#6B7280`).
- **Indicator:** No Material 3 pill indicator background; selection is conveyed solely via tint and text weight change (`Bold` vs `Medium`).

### 9.2 Navigation Animations & Screen Transitions
Configured via `nisComposable` helper in `NisNavigation.kt:173`:
```kotlin
enterTransition = { fadeIn(tween(220)) }
exitTransition = { fadeOut(tween(160)) }
popEnterTransition = { fadeIn(tween(220)) }
popExitTransition = { fadeOut(tween(160)) }
```
*(Notice: No spatial slides are applied between top-level screens; shared bounds own the physical movement).*

### 9.3 Shared Element Transitions Contract
Implemented in `NisSharedTransition.kt`:
- **Roles:** `enum class SharedRole { CONTAINER, IMAGE, TITLE }`
- **Key Formula:** `SharedKeys.of(mediaType, tmdbId, role, origin)` -> e.g. `"movie_image_550_home_trending"`
- **Origins:**
  - `SharedOrigins.homeRow(rowId)` -> `"home_$rowId"`
  - `SharedOrigins.discoverRow(rowId)` -> `"discover_$rowId"`
  - `SharedOrigins.SIMILAR` -> `"similar"`
  - `SharedOrigins.SEARCH` -> `"search"`
  - `SharedOrigins.WATCH_LATER` -> `"watchlater"`
- **Bounds Transform Spring:**
  ```kotlin
  val NisBoundsTransform = BoundsTransform { _, _ ->
      spring<Rect>(
          dampingRatio = 0.88f,
          stiffness = Spring.StiffnessMediumLow,
          visibilityThreshold = Rect.VisibilityThreshold,
      )
  }
  ```
- **Details Content Reveal:** `Modifier.nisContentEnterExit()` fades in over 300ms with a 120ms delay and small vertical slide (`it / 14`).
- **Double Tap Guard:** `Modifier.debouncedClickable()` prevents duplicate navigation triggers when lifecycle is not at least `RESUMED`.

---

## 10. Motion, Micro-Interactions & Animations

### 10.1 Master Animation Table
| Component / Screen | Property Animated | Duration / Spec | Easing / Spring | Trigger |
| :--- | :--- | :--- | :--- | :--- |
| **Splash Container** | Exit Alpha & Scale | 280ms (`EXIT_MS`) | `FastOutSlowInEasing` (scale 1.0 -> 1.06, alpha 1 -> 0) | Boot completion + min display threshold |
| **Splash Top Navy** | Gradient alpha | 350ms (0 to 350ms) | `FastOutSlowInEasing` | Screen launch |
| **Logo Squircle Base** | Scale & Alpha | 350ms (0 to 350ms) | `EaseOutQuint` (scale 0.94 -> 1.0) | Splash timeline start |
| **Logo Border Stroke** | Path length | 600ms (100 to 700ms)| `EaseInOutCubic` (PathMeasure segment) | Splash timeline |
| **Logo Pillars (L/R)** | Clip Y height | 450ms (150-600ms, 230-680ms) | `EaseOutQuint` (staggered rise) | Splash timeline |
| **Logo Cyber Ribbon** | Clip Wipe & Leading Head | 550ms (500 to 1050ms)| `EaseInOutCubic` | Splash timeline |
| **Logo Play Core** | Scale | Spring (damping 0.65, stiffness MediumLow) | Spring pop from 0f to 1f | Delay 850ms |
| **Logo Play Burst** | Radius & Alpha | 450ms (850 to 1300ms)| `FastOutSlowInEasing` | Delay 850ms |
| **Logo Ruby Beacon** | Scale & Pulse | Flare (220ms + 300ms) + 3 pings (750ms each) | `FastOutSlowInEasing` | Delay 1150ms |
| **Logo Sheen Sweep** | Diagonal translation| 600ms (1300 to 1900ms)| `LinearEasing` | Delay 1300ms |
| **Logo Breathing** | Scale | 1800ms reverse loop | `FastOutSlowInEasing` (0.985 <-> 1.015) | Post-build ambient idle |
| **Splash Wordmark** | Translation Y & Alpha| 450ms (900 to 1350ms)| `EaseOutQuint` (10dp slide) | Splash timeline |
| **Splash Tagline** | Translation Y & Alpha| 450ms (1100 to 1550ms)| `EaseOutQuint` (10dp slide) | Splash timeline |
| **Skeleton Shimmer** | Linear offset | 1200ms restart loop | `LinearEasing` (0f to 1000f) | Data loading |
| **Netflix Loading Logo**| Scale & Glow Alpha | 1200ms reverse loop | `FastOutSlowInEasing` (0.94 <-> 1.06) | Channel switching |
| **Netflix Bar Sweep** | Translation X | 1400ms restart loop | `LinearEasing` (-1f to 2f) | Channel switching |
| **Bottom Navigation** | Vertical Slide | `slideInVertically` / `slideOutVertically` | System default tween | Player entry / exit |
| **MiniPlayer Dock** | Vertical Slide + Fade| Slide (full height) + 200ms fade | FastOutSlow | Playback start / stop |
| **MiniPlayer Dismiss** | Horizontal Offset | 180ms tween (snap back 200ms) | Linear | Swipe gesture > 90dp |
| **Bookmark Icon** | Color tint | Standard `animateColorAsState` | FastOutSlow (TextPrimary -> AccentPrimary) | Watch Later toggle |
| **Player Overlays** | Alpha | 250ms fade in / fade out | FastOutSlow | User screen tap / 4s idle timer |
| **Autoplay Next** | Circular progress | 10,000ms linear countdown | Linear | 15s before episode completion |

### 10.2 Sonic Splash Timeline
- **Audio Asset:** `app/src/main/res/raw/nistv_splash.mp3`
- **Duration:** 1,900 ms (exact match to `SplashTimeline.BUILD_END_MS`).
- **Stream:** Media stream (`STREAM_MUSIC`, `USAGE_MEDIA`).
- **Policy:** Guaranteed playback regardless of ringer mode (plays in silent/vibrate), suppressed only if cellular call is active (`inCall == true`). Continues to natural reverb finish during navigation to Home.

---

## 11. Imagery, Posters & Media Scrims

### 11.1 Coil Image Loading Architecture
- **Version:** Coil 3.0.0 (`coil3.compose.AsyncImage`)
- **Factory:** Configured in `NisTvApplication.kt:318`:
  ```kotlin
  val okHttpClient = OkHttpClient.Builder()
      .connectTimeout(15, TimeUnit.SECONDS)
      .readTimeout(15, TimeUnit.SECONDS)
      .followRedirects(true)
      .followSslRedirects(true)
      .addInterceptor { chain ->
          val request = chain.request().newBuilder()
              .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 NisTV/2.0")
              .build()
          chain.proceed(request)
      }
      .build()

  ImageLoader.Builder(context)
      .components { add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient })) }
      .crossfade(true)
      .build()
  ```
- **Pre-fetching:** In `MainActivity.kt:466-480`, clicking any card pre-enqueues both `posterUrl` and `backdropUrl` into `SingletonImageLoader` memory cache with deterministic keys (`tmdb_poster_{type}_{id}`) before firing navigation, guaranteeing zero-latency image presentation on the details screen.

### 11.2 TMDB Image Size Pipeline
- **Backdrops (Hero & 16:9 Rails):** `w780` (`https://image.tmdb.org/t/p/w780/...`)
- **Posters (2:3 Vertical Cards):** `w500` in `MediaPreview.kt`; `w342` in legacy `TmdbModels.kt`.
- **Episode Stills:** `w300`
- **Cast Profile Photos:** `w185` (rendered in 64.dp circles with `CircleShape`).
- **Production Studio Logos:** `w92`

### 11.3 Channel Logo Styling & Monogram Fallback
- **Aspect Ratio:** Inconsistent broadcast logo PNGs are enclosed in fixed-dimension neutral containers (`NisSurfaceElevated`, border 1dp `NisBorder`, radius 10.dp).
- **Scale:** `ContentScale.Fit` with internal padding (5.dp in grid, 8.dp in rail).
- **Monogram Fallback:** `ChannelMonogram` renders a 2-stop diagonal gradient (`#1C2230` to `#232B3E`) with extracted 1-2 letter uppercase channel acronyms in `NisAccentPrimary` (15.sp Bold).

---

## 12. Player UI & Overlay Systems

### 12.1 Overlay Structure & Layout
The video canvas is rendered via Media3 `PlayerView` wrapped in an `AndroidView` inside a `Box(fillMaxSize().background(Color.Black))`. Overlaid on top is a gesture detector and two scrim gradients:

1. **Top Bar Overlay:** 80dp vertical gradient (`Color.Black.copy(0.85f)` to `Transparent`). Back button (44dp), Channel Title & Stream info (Resolution, FPS, Language), Screencast button, Multi-view button, Track selector button.
2. **Bottom Bar Overlay:** 120dp vertical gradient (`Transparent` to `Color.Black.copy(0.90f)`). Live badge, Stream bandwidth telemetry, Channel Quick Drawer toggle button, Sleep timer button, Fullscreen/aspect toggle button.
3. **Gesture Interaction Areas:**
   - Left 40% width: Vertical drag controls display brightness (HUD pill with `☼` icon and 0-100% bar).
   - Right 40% width: Vertical drag controls media stream volume (HUD pill with `VolumeUp`/`VolumeMute` and bar).
   - Center 20% width: Single tap toggles overlay visibility with 4-second auto-hide timeout. Double tap left/right performs seek ±10s with ripple rings.

### 12.2 Player Sheets & Overlays
- **`ChannelQuickDrawer`:** Slides in from the right edge covering 75% width. Displays searchable, categorised channel list for rapid channel changing without leaving playback.
- **`ZapOverlay`:** Channel zapping HUD displayed briefly during channel up/down navigation.
- **`PlayerBrandingIntro`:** 2.5-second brand bumper playing `R.raw.nistv_branding.mp4` on fresh channel launches.
- **`TrackSelectorBottomSheet`:** Modal sheet to switch video renditions (HLS/DASH quality variants), audio language tracks, and embedded/sidecar subtitles.
- **`BrightnessVolumeHuds`:** Floating capsule HUD (`Color(0xCC000000)`, radius 24.dp) centered on screen.

### 12.3 Orientation & Fullscreen
Managed by `rememberPlayerOrientationController()` in `PlayerOrientationController.kt`:
- **Fullscreen Toggle:** Hides status bars and navigation bars via `WindowInsetsControllerCompat.hide(WindowInsetsCompat.Type.systemBars())` and sets `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`.
- **Orientation:** Automatically locks to landscape when fullscreen is toggled or when physical device rotation is triggered.

---

## 13. Accessibility, Responsiveness & Contrast

### 13.1 Content Descriptions Coverage
- Primary navigation and header actions provide explicit content descriptions (`"Search"`, `"Profile & Settings"`, `"TV Guide"`, `"My List"`).
- Many secondary decorative icons pass `contentDescription = null`.
- Custom `clickable` modifiers across `NisChannelCard`, `NisPosterMediaCard`, and `NisBottomNavigationBar` specify explicit `Role` and `onClickLabel` properties for TalkBack screen readers.

### 13.2 Touch Target Sizes Below 48dp
Google Material and Android Accessibility guidelines mandate interactive targets of at least **48.dp x 48.dp**. Multiple components in NisTV fall short:

| Component | Actual Touch Size | Location | Remediation Required |
| :--- | :--- | :--- | :--- |
| `NisPrimaryButton` | Min height 44.dp | `NisButton.kt:45` | Increase `defaultMinSize(minHeight = 48.dp)` |
| `NisSecondaryButton` | Min height 44.dp | `NisButton.kt:84` | Increase `defaultMinSize(minHeight = 48.dp)` |
| `NisIconButton` | 44.dp x 44.dp | `NisButton.kt:124` | Increase `defaultMinSize(48.dp, 48.dp)` |
| `NisBookmarkButton` | 32.dp x 32.dp | `NisBookmarkButton.kt:45` | Increase clickable bounding box to 48.dp |
| `HomeContinueWatchingCard` options | 28.dp x 28.dp | `HomeScreen.kt:1356` | Increase touch target to 48.dp |
| Channel Card favorite toggle | 44.dp x 44.dp | `NisChannelCard.kt:125` | Increase touch area to 48.dp |
| Details Screen floating back button | 40.dp x 40.dp | `MediaDetailsScreen.kt:387` | Increase touch area to 48.dp |
| Player Screen overlay buttons | 36.dp to 44.dp | `PlayerScreen.kt` | Expand touch targets |

### 13.3 Contrast Ratios & WCAG Compliance
Evaluated against `NisBackground` (`#0B0D12`, relative luminance ~0.006) and `NisSurface` (`#141821`, luminance ~0.009):

| Foreground Colour | Background Colour | Approximate Ratio | WCAG AA Status | Remediation Note |
| :--- | :--- | :--- | :--- | :--- |
| `NisTextPrimary` (`#F2F4F8`) | `NisBackground` (`#0B0D12`) | **16.1 : 1** | PASS (AAA) | High contrast, excellent readability |
| `NisTextSecondary` (`#9AA3B5`)| `NisBackground` (`#0B0D12`) | **7.4 : 1** | PASS (AAA) | Good readability for subtitles |
| `NisTextMuted` (`#6B7280`) | `NisBackground` (`#0B0D12`) | **3.7 : 1** | **FAIL (< 4.5:1)** | Fails WCAG AA for normal text (< 18sp) |
| `NisTextMuted` (`#6B7280`) | `NisSurfaceElevated` (`#1C2230`) | **3.2 : 1** | **FAIL (< 4.5:1)** | Low contrast on elevated card containers |
| Bottom Nav Inactive (`#6B7280`)| `NisSurface` (`#141821`) | **3.4 : 1** | **FAIL (< 4.5:1)** | Fails for 11sp navigation labels |
| Disabled Content (38% alpha) | `NisSurfaceElevated` (`#1C2230`) | **2.1 : 1** | Informational | Standard for disabled states |

### 13.4 Responsiveness & Large Screens
- **Phone vs. Tablet:** `LiveTvScreen` adapts gracefully via `GridCells.Adaptive(minSize = 160.dp)`. However, `DiscoverScreen` hardcodes `GridCells.Fixed(3)`, causing cards to stretch excessively on tablets and foldables.
- **RTL Support:** `android:supportsRtl="true"` is declared in `AndroidManifest.xml`. However, custom SVG vector icons like `Back` and `Play` are hardcoded left-to-right paths without automatic mirroring.
- **Reduced Motion:** Fully respected in `NisAnimatedSvgLogo.kt` and `SplashScreen.kt` via `rememberReducedMotion()`. When active, animated build times snap to 0ms and exit times drop to 150ms.

---

## 14. Brand Assets & Multi-Platform Alignment

### 14.1 Brand Assets Inventory
- **`assets/logo.svg`:** 512x512 vector source. Rounded square (`rx="112"`, fill `#0B0D12`), left indigo pillar `#4B3FB5`, right indigo pillar `#30268C`, diagonal cyan ribbon `#06B6D4`->`#3B6FD8`, play arrow facet `#22D3EE`.
- **`assets/logo_foreground.svg`:** 512x512 vector foreground emblem without background rectangle.
- **`ic_nistv_logo.xml`:** 108dp x 108dp Android VectorDrawable matching the squircle brand mark.
- **`NisLogo.kt`:** Pure Compose Canvas implementation with mathematical geometry matching 108x108 normalized grid coordinates.

### 14.2 The Two Divergent Logos in Code
The codebase contains **two completely different logos**:
1. **`NisLogo` (`NisLogo.kt`):** The primary official NisTV emblem. Features the obsidian squircle, dual shaded pillars, cyber cyan sweeping ribbon, embedded play triangle (▶), and ruby broadcast beacon. Used on Splash, App Icon, and headers.
2. **`NtvLogo` (`NtvLogo.kt`):** A secondary 3D ribbon 'N' monogram paired with bold geometric "TV" typography and an animated cyan live dot. Used exclusively in `NetflixLoadingScreen` and auth gate headers.

### 14.3 Website Branding vs. Mobile App Alignment
The companion promotional website located in `website/` uses Tailwind CSS:
- **Web Palette (`tailwind.config.js`):**
  - Background: `#07090e` (matches Android `colors.xml`, but differs from app's `#0B0D12`).
  - Cards: `#131822` (matches app `NisSurface` `#141821`).
  - Primary Brand: `#437df4` / `#2b5ee8` (standard royal blue, whereas the app uses periwinkle `#6C8CFF`).
  - Web Accents: cyan (`#00f2fe`), purple (`#7928ca`), pink (`#ff0080`), emerald (`#10b981`).
- **Typography:** Web uses `Inter` (`font-sans`), while Android app uses system Roboto.

---

## 15. Design Debt & Visual Inconsistencies

1. **Misnamed Core Color Token (`Color.kt:25-26`):**
   `NisAccentGreen` is defined as `val NisAccentGreen = NisAccentPrimary` (`#6C8CFF`), which is a periwinkle blue, NOT green! `NisAccentGreenPressed` is `#5B7BE8`. In `colors.xml`, `nistv_accent_green` is actually green (`#39D98A`).
2. **Background Color Divergence (`Color.kt` vs `colors.xml`):**
   `colors.xml` declares `nistv_background` as `#07090D`, while `themes.xml`, `Color.kt`, and `SplashScreen.kt` declare `#0B0D12`.
3. **Dead / Unused `LocalSpacing` Token (`Spacing.kt:18`):**
   `LocalSpacing` is defined but never supplied to a `CompositionLocalProvider` and never referenced in any UI file.
4. **Unused Shape Tokens (`Shape.kt`):**
   `NisSmallControlShape`, `NisFeaturedCardShape`, and `NisBottomSheetShape` are defined but never used. Furthermore, `NisShapes` is never passed to `MaterialTheme(shapes = ...)` in `Theme.kt`.
5. **Rampant Hardcoded Spacing & Typography:**
   - 929 hardcoded `.sp` calls.
   - 398 hardcoded `RoundedCornerShape(...)` calls.
   - 251 hardcoded `Color(0x...)` calls across 130 unique hex values.
6. **Touch Targets Violating Accessibility Guidelines:**
   Primary buttons, icon buttons, bookmark buttons, and overflow dots have touch heights of 44dp, 40dp, 32dp, and 28dp (below 48dp standard).
7. **`NisTextMuted` Contrast Failure:**
   `#6B7280` produces a 3.7:1 contrast ratio against the app background, failing WCAG AA requirements for body text.
8. **Disabled Ripple / Indication Feedback:**
   `NisIconButton`, `NisBottomNavigationBar`, `NisChannelCard` favorite button, and `NisSectionHeader` action specify `indication = null`, preventing standard visual ripple feedback when tapped.
9. **Missing Themed Launcher Icon:**
   `ic_launcher.xml` lacks the `<monochrome>` layer required for Android 13+ Material You themed home screens.
10. **Fixed Columns on Discover Grid (`DiscoverScreen.kt:143, 167`):**
    `GridCells.Fixed(3)` forces 3 columns on all form factors, breaking visual balance on large tablets and foldables.

---

## 16. Redesign Constraints & Safe Edit Plan

When redesigning or refactoring the NisTV theme, the following invariants **must remain stable**:

### 16.1 Critical Architectural Contracts
1. **Public Composable Signatures:** All signatures in `core/designsystem/component/` must retain parameter names and defaults (e.g. `NisPrimaryButton`, `NisChannelCard`, `NisPosterMediaCard`, `NisMiniPlayer`).
2. **Shared Transition Keys Contract:** `SharedKeys.of(mediaType, tmdbId, role, origin)` must NOT be changed. Any change will break container transform transitions and cause `SharedKeysTest.kt` to fail.
3. **Splash Choreography Contract:** Segment durations and constants in `SplashTimeline.kt` (`BUILD_END_MS = 1900`, `MIN_DISPLAY_MS = 1900L`, `HARD_TIMEOUT_MS = 4000L`, `EXIT_MS = 280`, `Squircle`, `Border`, etc.) are asserted by `SplashTimelineTest.kt`.
4. **Navigation Route Names:** Sealed routes in `Screen` (`"splash"`, `"home"`, `"discover"`, `"livetv"`, `"downloads"`, `"details/{mediaType}/{mediaId}?origin={origin}"`, `"player/{channelId}"`, etc.) must remain unchanged.

### 16.2 Single-Source-of-Truth Theme File Map
To execute clean visual changes without touching screen files:
- **Colour Palette & Token Values:** Edit `app/src/main/java/com/nis/nistv/core/designsystem/theme/Color.kt`. Synchronize with `app/src/main/res/values/colors.xml` and `themes.xml`.
- **Global Theme & System Bars:** Edit `app/src/main/java/com/nis/nistv/core/designsystem/theme/Theme.kt`. Connect `NisShapes` into `MaterialTheme(shapes = NisShapes)`.
- **Typography Scale:** Edit `app/src/main/java/com/nis/nistv/core/designsystem/theme/Type.kt`.
- **Shapes & Radii:** Edit `app/src/main/java/com/nis/nistv/core/designsystem/theme/Shape.kt`.
- **Spacing Token Values:** Edit `app/src/main/java/com/nis/nistv/core/designsystem/theme/Spacing.kt`.

---

## 17. Preview & Screenshot Readiness

### 17.1 Existing `@Preview` Survey
Only **1** `@Preview` composable currently exists in the codebase:
- **File:** `app/src/main/java/com/nis/nistv/core/designsystem/component/NisAnimatedSvgLogo.kt:221`
- **Composable:**
  ```kotlin
  @Preview(showBackground = true, backgroundColor = 0xFF0B0D12)
  @Composable
  fun NisLogoTimelineScrubberPreview()
  ```
  *(Provides an interactive slider to scrub the logo build timeline between 0ms and 1900ms).*

### 17.2 Screen Previewability & Mock Data Readiness
Most screens take ViewModels injected via `viewModel(factory = ...)`. To render them in Compose previews or offline review:
- **`HomeScreen` / `LiveTvScreen` / `DiscoverScreen`:** Decouple root screen composables into state-hoisted variants (`HomeContent(uiState = ...)`), or pass fake mock repository containers.
- **Card Previews:** `NisChannelCard`, `NisPosterMediaCard`, `NisBackdropMediaCard`, and `NisMiniPlayer` accept pure data models (`ChannelEntity`, `MediaPreview`) and can be wrapped in `@Preview` immediately with synthetic mock objects.
- **Shared Transitions in Previews:** `nisSharedBounds` gracefully falls back to a no-op if `LocalSharedTransitionScope.current` is null, meaning all cards and details screens can be rendered safely inside Compose previews without crashing.
