# NisTV Revamped — Splash Screen & Animated Logo Architecture Reference

> **Document Version:** 1.0.0  
> **Target Codebase:** NisTV Revamped (Android / Jetpack Compose)  
> **App Version at Verification:** 1.3.2 (VersionCode: 10302)  
> **Generated:** November 2026  
> **Status:** Verified from repository source code. All code quotes are verbatim.

---

## Table of Contents
1. [Overview](#1-overview)
2. [File Map](#2-file-map)
3. [System (Android) Splash](#3-system-android-splash)
4. [Logo Source](#4-logo-source)
   - [4.1 Pure Compose Canvas Implementation (`NisLogo.kt`)](#41-pure-compose-canvas-implementation-nislogokt)
   - [4.2 Android VectorDrawable (`ic_nistv_logo.xml`)](#42-android-vectordrawable-ic_nistv_logoxml)
   - [4.3 Launcher Icon VectorDrawables (`ic_launcher_*.xml`)](#43-launcher-icon-vectordrawables-ic_launcher_xml)
   - [4.4 Source SVG Vector Assets (`assets/logo*.svg`)](#44-source-svg-vector-assets-assetslogosvg)
5. [Animation Implementation](#5-animation-implementation)
   - [5.1 Full Verbatim Code: `SplashScreen.kt`](#51-full-verbatim-code-splashscreenkt)
   - [5.2 Full Verbatim Code: `NisAnimatedSvgLogo.kt`](#52-full-verbatim-code-nisanimatedsvglogokt)
   - [5.3 Comprehensive Animated Properties Table](#53-comprehensive-animated-properties-table)
6. [Timeline](#6-timeline)
7. [App Boot Work During Splash](#7-app-boot-work-during-splash)
8. [Layout and Styling](#8-layout-and-styling)
9. [Rendering and Performance](#9-rendering-and-performance)
10. [Known Issues and Observations](#10-known-issues-and-observations)
11. [Test Coverage](#11-test-coverage)
12. [Extension Points & Constraints](#12-extension-points--constraints)

---

## 1. Overview

When the user taps the NisTV launcher icon, the cold-boot experience progresses in three distinct, sequential phases:

```
[Tap Launcher Icon]
        │
        ▼
Phase 1: System Android Window Splash (Theme.NisTV windowBackground #07090D + OS adaptive icon on API 31+)
        │
        ▼ (Process fork, Application.onCreate, Activity.onCreate, Compose initialization)
Phase 2: Compose Navigation Splash (Screen.Splash / SplashScreen.kt)
        │ - 3-stop vertical gradient background (#0F1523 -> #0B0D12)
        │ - Container fade-in (0f -> 1f over 500ms)
        │ - NisAnimatedSvgLogo: squircle draw, pillar rise, ribbon wipe, play core spring pop, ruby beacon flare
        │ - Continuous loops: pulse scale, specular light beam sweep, traveling ribbon energy, radar ripples, aurora rotation
        │ - Parallel seed database warm-up (channelRepository.ensureSeeded())
        │ - Hardcoded delay(2000)
        │ - Container fade-out (1f -> 0f over 300ms)
        │
        ▼ (navController.navigate("home") with popUpTo("splash") { inclusive = true })
Phase 3: NisTV Home Screen (HomeScreen.kt)
```

First, Android displays the platform window background configured in `Theme.NisTV` (`#07090D`). On Android 12+ (API 31+), the platform draws the adaptive icon (`@mipmap/ic_launcher`) centered over this background. Second, as soon as `MainActivity.onCreate()` initializes and runs `setContent`, Navigation Compose loads the initial route `Screen.Splash` (`"splash"`). The dedicated `SplashScreen` composable renders an edge-to-edge dark vertical gradient (`#0F1523` to `#0B0D12`), fades in the logo and typography over 500 ms, triggers the choreographed entrance of the `NisAnimatedSvgLogo`, runs continuous ambient visual effects (radar rings, specular sheen sweep, ribbon energy pulse, and ruby beacon ping), and ensures channel seed data is warm in Room DB. Third, after a hardcoded 2,000 ms delay, the splash content fades out over 300 ms and navigates to `Screen.Home`, popping the splash destination off the back stack.

---

## 2. File Map

| File Path | Purpose | Line Count | Asset Format / Type |
| :--- | :--- | :--- | :--- |
| `app/src/main/java/com/nis/nistv/feature/splash/SplashScreen.kt` | Independent splash screen composable; orchestrates entrance fade, background seed check, delay, exit fade, and navigation callback | 139 | Kotlin (Jetpack Compose) |
| `app/src/main/java/com/nis/nistv/core/designsystem/component/NisAnimatedSvgLogo.kt` | Animated logo orchestrator; coordinates canvas entrance sequences, spring bounce, beacon flare, and infinite ambient loops | 313 | Kotlin (Jetpack Compose) |
| `app/src/main/java/com/nis/nistv/core/designsystem/component/NisLogo.kt` | Core brand emblem renderer; draws squircle, border, pillars, diagonal ribbon, play core, and ruby beacon via Canvas math | 356 | Kotlin (Compose Canvas / DrawScope) |
| `app/src/main/java/com/nis/nistv/core/designsystem/component/NtvLogo.kt` | Alternate 3D ribbon monogram and wordmark used in loading and auth gate dialogs (unused on splash) | 224 | Kotlin (Jetpack Compose) |
| `app/src/main/java/com/nis/nistv/MainActivity.kt` | Launcher activity; sets startDestination = `Screen.Splash.route`, hosts `NavHost`, and navigates to `Screen.Home.route` on finish | 718 | Kotlin (Android Activity / Compose) |
| `app/src/main/java/com/nis/nistv/NisTvApplication.kt` | Application class; initializes `DefaultAppContainer` and launches asynchronous background boot tasks (seed, repair, sync) | 314 | Kotlin (Android Application) |
| `app/src/main/java/com/nis/nistv/core/navigation/NisNavigation.kt` | Navigation contract defining sealed `Screen.Splash` (`"splash"`) route | 178 | Kotlin |
| `app/src/main/java/com/nis/nistv/core/designsystem/theme/Color.kt` | Color tokens: `NisBackground` (`#0B0D12`), `NisAccentPrimary` (`#6C8CFF`), `NisAccentCyan` (`#38BDF8`), `NisTextPrimary` (`#F2F4F8`) | 43 | Kotlin |
| `app/src/main/java/com/nis/nistv/core/designsystem/theme/Theme.kt` | Dark Material3 theme and system bar insets color overrides (`isAppearanceLightStatusBars = false`) | 51 | Kotlin (Jetpack Compose) |
| `app/src/main/res/values/themes.xml` | System window theme `Theme.NisTV` declaring windowBackground `#07090D` | 8 | Android XML |
| `app/src/main/res/values/colors.xml` | System color resources (`nistv_background` = `#07090D`) | 11 | Android XML |
| `app/src/main/res/drawable/ic_nistv_logo.xml` | Static VectorDrawable representation of the NisTV brand mark (108dp x 108dp) | 42 | Android VectorDrawable XML |
| `app/src/main/res/drawable/ic_launcher_background.xml` | Vector background for adaptive launcher icon (108dp x 108dp) | 33 | Android VectorDrawable XML |
| `app/src/main/res/drawable/ic_launcher_foreground.xml` | Vector foreground for adaptive launcher icon (108dp x 108dp) | 71 | Android VectorDrawable XML |
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` | Adaptive icon linking background and foreground drawables | 5 | Android Adaptive Icon XML |
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml` | Round adaptive icon linking background and foreground drawables | 5 | Android Adaptive Icon XML |
| `app/src/main/AndroidManifest.xml` | Declares `.MainActivity` as launcher activity with `@style/Theme.NisTV` | 70 | Android Manifest XML |
| `assets/logo.svg` | Full standalone brand logo SVG with background squircle and drop shadow filter (512x512) | 52 | Raw SVG File |
| `assets/logo_foreground.svg` | Standalone brand logo foreground SVG mark without canvas background (512x512) | 49 | Raw SVG File |
| `app/src/main/assets/splash.png` | Static raster splash image asset (116,879 bytes) | Binary | Raster PNG (Fallback / Dead) |
| `app/src/main/assets/logo_1024.png` | 1024x1024 branding raster icon asset (24,127 bytes) | Binary | Raster PNG (Static Asset) |
| `app/src/main/assets/logo_512.png` | 512x512 branding raster icon asset (10,637 bytes) | Binary | Raster PNG (Static Asset) |
| `app/src/main/assets/ic_launcher_foreground.png` | High-res foreground raster asset in assets (21,104 bytes) | Binary | Raster PNG (Static Asset) |
| `app/src/main/assets/logo-backup/*` | Four backup launcher icon XML definitions | XML | Android XML |
| `app/src/test/java/com/nis/nistv/StartupTest.kt` | Unit test verifying reflection class loading of `SplashScreenKt`, `NisLogoKt`, and `NisAnimatedSvgLogoKt` | 18 | Kotlin (JUnit4) |

---

## 3. System (Android) Splash

### 3.1 Theme Declaration
The system splash theme is defined in `app/src/main/res/values/themes.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.NisTV" parent="android:Theme.Material.NoActionBar">
        <item name="android:statusBarColor">#07090D</item>
        <item name="android:navigationBarColor">#07090D</item>
        <item name="android:windowBackground">#07090D</item>
    </style>
</resources>
```

In `app/src/main/AndroidManifest.xml`:
```xml
    <application
        android:name=".NisTvApplication"
        ...
        android:theme="@style/Theme.NisTV">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:configChanges="orientation|screenSize|screenLayout|keyboardHidden|smallestScreenSize"
            android:supportsPictureInPicture="true"
            android:theme="@style/Theme.NisTV">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
```

### 3.2 Android 12+ (API 31+) Attributes & androidx.core:splashscreen Verification
- **`androidx.core:core-splashscreen` / `Theme.SplashScreen`:** **UNKNOWN / NOT FOUND**. The dependency is not declared in `gradle/libs.versions.toml` or `app/build.gradle.kts`.
- **`windowSplashScreenBackground`:** **UNKNOWN / NOT FOUND**. (Falls back to `android:windowBackground` = `#07090D`).
- **`windowSplashScreenAnimatedIcon`:** **UNKNOWN / NOT FOUND**. (On API 31+, OS defaults to centering `@mipmap/ic_launcher`).
- **`windowSplashScreenAnimationDuration`:** **UNKNOWN / NOT FOUND**.
- **`windowSplashScreenIconBackgroundColor`:** **UNKNOWN / NOT FOUND**.
- **`postSplashScreenTheme`:** **UNKNOWN / NOT FOUND**.
- **`installSplashScreen()` usage:** **UNKNOWN / NOT FOUND**. `MainActivity.onCreate()` does not invoke `installSplashScreen()`.
- **`setKeepOnScreenCondition` usage:** **UNKNOWN / NOT FOUND**.

In `MainActivity.kt`, the entry point is standard Compose without splash interception:
```kotlin
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NisTvTheme {
                NisTvAppNav(
                    isInPipMode = isInPipMode,
                    onUpdatePipParams = { shouldAutoPip ->
                        updatePipParams(shouldAutoPip)
                    }
                )
            }
        }
    }
```

---

## 4. Logo Source

The NisTV logo is implemented primarily as **pure Jetpack Compose Canvas draw calls** in `NisLogo.kt` with a fixed coordinate system of 108x108 normalized units (`sx = width / 108f`, `sy = height / 108f`). Secondary definitions exist as an Android VectorDrawable (`ic_nistv_logo.xml`), launcher icon drawables, and standalone SVG files.

### 4.1 Pure Compose Canvas Implementation (`NisLogo.kt`)
Below is the verbatim geometry, path curves, and shaders from `app/src/main/java/com/nis/nistv/core/designsystem/component/NisLogo.kt` lines 52–354:

```kotlin
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val sx = w / 108f
            val sy = h / 108f

            // 1. Squircle Base Frame
            val baseAlpha = drawProgress.coerceIn(0f, 1f)
            if (baseAlpha > 0f) {
                val cornerR = 18f * sx
                val basePadding = 6f * sx
                val baseSize = 96f * sx
                val baseTopLeft = Offset(basePadding, basePadding)

                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF221A44).copy(alpha = baseAlpha),
                            Color(0xFF131028).copy(alpha = baseAlpha),
                            Color(0xFF07090E).copy(alpha = baseAlpha)
                        ),
                        center = Offset(54f * sx, 54f * sy),
                        radius = 50f * sx
                    ),
                    topLeft = baseTopLeft,
                    size = Size(baseSize, baseSize),
                    cornerRadius = CornerRadius(cornerR, cornerR)
                )

                // 2. Glowing Squircle Border
                val borderAlpha = ((drawProgress - 0.05f) / 0.45f).coerceIn(0f, 1f)
                if (borderAlpha > 0f) {
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0x8000F2FE).copy(alpha = 0.5f * borderAlpha),
                                Color(0x666366F1).copy(alpha = 0.4f * borderAlpha),
                                Color(0x26FFFFFF).copy(alpha = 0.15f * borderAlpha)
                            ),
                            start = Offset(basePadding, basePadding),
                            end = Offset(basePadding + baseSize, basePadding + baseSize)
                        ),
                        topLeft = baseTopLeft,
                        size = Size(baseSize, baseSize),
                        cornerRadius = CornerRadius(cornerR, cornerR),
                        style = Stroke(width = (1.2f * sx).coerceAtLeast(1f))
                    )
                }

                // 3. Ambient Center Glow
                val centerOffset = Offset(54f * sx, 54f * sy)
                val glowAlpha = ((drawProgress - 0.15f) / 0.5f).coerceIn(0f, 1f)
                if (glowAlpha > 0f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0x3338BDF8).copy(alpha = 0.2f * glowAlpha),
                                Color(0x1A6366F1).copy(alpha = 0.1f * glowAlpha),
                                Color.Transparent
                            ),
                            center = centerOffset,
                            radius = 30f * sx
                        ),
                        radius = 30f * sx,
                        center = centerOffset
                    )
                }

                // 4. Shaded Vertical Pillars (Progressive Rise)
                val pillarProgress = ((drawProgress - 0.2f) / 0.45f).coerceIn(0f, 1f)
                if (pillarProgress > 0f) {
                    clipRect(
                        left = 0f,
                        top = 81f * sy - (81f - 27f) * sy * pillarProgress,
                        right = w,
                        bottom = h
                    ) {
                        // Left Pillar (Stream Column with rounded top)
                        val leftPillar = Path().apply {
                            moveTo(30f * sx, 33f * sy)
                            cubicTo(30f * sx, 29.7f * sy, 32.7f * sx, 27f * sy, 36f * sx, 27f * sy)
                            lineTo(42f * sx, 27f * sy)
                            lineTo(42f * sx, 81f * sy)
                            lineTo(30f * sx, 81f * sy)
                            close()
                        }
                        drawPath(
                            path = leftPillar,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF4338CA),
                                    Color(0xFF3730A3),
                                    Color(0xFF1E1B4B)
                                ),
                                startY = 27f * sy,
                                endY = 81f * sy
                            ),
                            style = Fill
                        )

                        // Right Pillar (Stream Column with rounded bottom)
                        val rightPillar = Path().apply {
                            moveTo(66f * sx, 27f * sy)
                            lineTo(78f * sx, 27f * sy)
                            lineTo(78f * sx, 75f * sy)
                            cubicTo(78f * sx, 78.3f * sy, 75.3f * sx, 81f * sy, 72f * sx, 81f * sy)
                            lineTo(66f * sx, 81f * sy)
                            close()
                        }
                        drawPath(
                            path = rightPillar,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF312E81),
                                    Color(0xFF4C1D95),
                                    Color(0xFF2E1065)
                                ),
                                startY = 27f * sy,
                                endY = 81f * sy
                            ),
                            style = Fill
                        )

                        // Ambient Drop Shadows under Ribbon Overlap
                        val leftShadow = Path().apply {
                            moveTo(30f * sx, 27f * sy)
                            lineTo(42f * sx, 27f * sy)
                            lineTo(42f * sx, 54f * sy)
                            lineTo(30f * sx, 40f * sy)
                            close()
                        }
                        drawPath(
                            path = leftShadow,
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0x99000000), Color.Transparent),
                                start = Offset(36f * sx, 27f * sy),
                                end = Offset(36f * sx, 54f * sy)
                            )
                        )

                        val rightShadow = Path().apply {
                            moveTo(66f * sx, 54f * sy)
                            lineTo(78f * sx, 68f * sy)
                            lineTo(78f * sx, 81f * sy)
                            lineTo(66f * sx, 81f * sy)
                            close()
                        }
                        drawPath(
                            path = rightShadow,
                            brush = Brush.linearGradient(
                                colors = listOf(Color.Transparent, Color(0x99000000)),
                                start = Offset(72f * sx, 54f * sy),
                                end = Offset(72f * sx, 81f * sy)
                            )
                        )
                    }
                }

                // 5. Dynamic Sweeping Diagonal Ribbon (Progressive Diagonal Wipe)
                val ribbonProgress = ((drawProgress - 0.35f) / 0.5f).coerceIn(0f, 1f)
                if (ribbonProgress > 0f) {
                    clipRect(
                        left = 0f,
                        top = 0f,
                        right = (28f + 54f * ribbonProgress) * sx,
                        bottom = (25f + 58f * ribbonProgress) * sy
                    ) {
                        val diagonalRibbon = Path().apply {
                            moveTo(30f * sx, 33f * sy)
                            cubicTo(30f * sx, 29.7f * sy, 32.7f * sx, 27f * sy, 36f * sx, 27f * sy)
                            lineTo(43f * sx, 27f * sy)
                            lineTo(78f * sx, 75f * sy)
                            cubicTo(78f * sx, 78.3f * sy, 75.3f * sx, 81f * sy, 72f * sx, 81f * sy)
                            lineTo(65f * sx, 81f * sy)
                            close()
                        }
                        drawPath(
                            path = diagonalRibbon,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF00F2FE),
                                    Color(0xFF0EA5E9),
                                    Color(0xFF6366F1),
                                    Color(0xFFA855F7)
                                ),
                                start = Offset(30f * sx, 27f * sy),
                                end = Offset(78f * sx, 81f * sy)
                            ),
                            style = Fill
                        )

                        // Specular Top Glass Ridge on Ribbon
                        val topSheen = Path().apply {
                            moveTo(30f * sx, 33f * sy)
                            cubicTo(30f * sx, 30.3f * sy, 32f * sx, 28.2f * sy, 34.5f * sx, 27.5f * sy)
                            lineTo(42f * sx, 27.5f * sy)
                            lineTo(42f * sx, 27f * sy)
                            lineTo(36f * sx, 27f * sy)
                            cubicTo(32.7f * sx, 27f * sy, 30f * sx, 29.7f * sy, 30f * sx, 33f * sy)
                            close()
                        }
                        drawPath(
                            path = topSheen,
                            color = Color(0x99FFFFFF),
                            style = Fill
                        )
                    }

                    // Traveling Energy Stream along Ribbon
                    if (ribbonEnergyProgress in 0.001f..0.999f && ribbonProgress >= 0.7f) {
                        val t = ribbonEnergyProgress
                        val ex = (36.5f + (71.5f - 36.5f) * t) * sx
                        val ey = (27f + (81f - 27f) * t) * sy
                        val energyRadius = 7f * sx

                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.95f),
                                    Color(0xFF00F2FE).copy(alpha = 0.75f),
                                    Color(0xFF0EA5E9).copy(alpha = 0.40f),
                                    Color.Transparent
                                ),
                                center = Offset(ex, ey),
                                radius = energyRadius
                            ),
                            radius = energyRadius,
                            center = Offset(ex, ey)
                        )
                    }
                }

                // 6. Luminous Play Core (▶) Symbol with Elastic Bounce
                if (playScale > 0.01f) {
                    val playCenter = Offset(55.75f * sx, 55f * sy)
                    withTransform({
                        scale(scaleX = playScale, scaleY = playScale, pivot = playCenter)
                    }) {
                        val playShadow = Path().apply {
                            moveTo(49f * sx, 50.5f * sy)
                            cubicTo(49f * sx, 49.2f * sy, 50.4f * sx, 48.3f * sy, 51.5f * sx, 49.0f * sy)
                            lineTo(62.5f * sx, 54.8f * sy)
                            cubicTo(63.6f * sx, 55.4f * sy, 63.6f * sx, 56.6f * sy, 62.5f * sx, 57.2f * sy)
                            lineTo(51.5f * sx, 63.0f * sy)
                            cubicTo(50.4f * sx, 63.7f * sy, 49f * sx, 62.8f * sy, 49f * sx, 61.5f * sy)
                            close()
                        }
                        drawPath(
                            path = playShadow,
                            color = Color(0x66000000),
                            style = Fill
                        )

                        val playCore = Path().apply {
                            moveTo(49f * sx, 48.5f * sy)
                            cubicTo(49f * sx, 47.2f * sy, 50.4f * sx, 46.3f * sy, 51.5f * sx, 47.0f * sy)
                            lineTo(62.5f * sx, 52.8f * sy)
                            cubicTo(63.6f * sx, 53.4f * sy, 63.6f * sx, 54.6f * sy, 62.5f * sx, 55.2f * sy)
                            lineTo(51.5f * sx, 61.0f * sy)
                            cubicTo(50.4f * sx, 61.7f * sy, 49f * sx, 60.8f * sy, 49f * sx, 59.5f * sy)
                            close()
                        }
                        drawPath(
                            path = playCore,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFFFFFF),
                                    Color(0xFFE0F2FE),
                                    Color(0xFF67E8F9)
                                ),
                                start = Offset(49f * sx, 47f * sy),
                                end = Offset(63f * sx, 61f * sy)
                            ),
                            style = Fill
                        )
                    }
                }

                // 7. Live TV Broadcast Ruby Beacon (Top-Right Flare)
                if (beaconIntensity > 0.01f) {
                    val beaconCenter = Offset(75f * sx, 23.5f * sy)
                    val haloRadius = (4.5f * beaconIntensity * sx).coerceAtLeast(1f)
                    val coreRadius = (2.4f * (0.8f + 0.2f * beaconIntensity.coerceAtMost(1.5f)) * sx).coerceAtLeast(1f)
                    val dotRadius = (0.9f * sx).coerceAtLeast(0.5f)

                    drawCircle(
                        color = Color(0x66FF4D5E).copy(alpha = (0.45f * beaconIntensity).coerceIn(0f, 0.85f)),
                        radius = haloRadius,
                        center = beaconCenter
                    )
                    drawCircle(
                        color = Color(0xFFFF4D5E),
                        radius = coreRadius,
                        center = beaconCenter
                    )
                    drawCircle(
                        color = Color(0xFFFFFFFF),
                        radius = dotRadius,
                        center = Offset(beaconCenter.x - 0.4f * sx, beaconCenter.y - 0.4f * sy)
                    )
                }
            }
        }
```

### 4.2 Android VectorDrawable (`ic_nistv_logo.xml`)
Full verbatim content of `app/src/main/res/drawable/ic_nistv_logo.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">

    <!-- 1. Background Squircle Base -->
    <path
        android:fillColor="#131028"
        android:pathData="M 16 10 L 92 10 A 16 16 0 0 1 108 26 L 108 82 A 16 16 0 0 1 92 98 L 16 98 A 16 16 0 0 1 0 82 L 0 26 A 16 16 0 0 1 16 10 Z" />

    <!-- 2. Glowing Squircle Border -->
    <path
        android:strokeColor="#336366F1"
        android:strokeWidth="1.2"
        android:pathData="M 16 10 L 92 10 A 16 16 0 0 1 108 26 L 108 82 A 16 16 0 0 1 92 98 L 16 98 A 16 16 0 0 1 0 82 L 0 26 A 16 16 0 0 1 16 10 Z" />

    <!-- 3. Left Pillar -->
    <path
        android:fillColor="#4338CA"
        android:pathData="M 30 33 C 30 29.7 32.7 27 36 27 L 42 27 L 42 81 L 30 81 Z" />

    <!-- 4. Right Pillar -->
    <path
        android:fillColor="#312E81"
        android:pathData="M 66 27 L 78 27 L 78 75 C 78 78.3 75.3 81 72 81 L 66 81 Z" />

    <!-- 5. Dynamic Sweeping Diagonal Ribbon -->
    <path
        android:fillColor="#00F2FE"
        android:pathData="M 30 33 C 30 29.7 32.7 27 36 27 L 43 27 L 78 75 C 78 78.3 75.3 81 72 81 L 65 81 L 30 33 Z" />

    <!-- 6. Embedded Luminous Play Core -->
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M 49 49.5 C 49 48.2 50.4 47.3 51.5 48.0 L 62.5 53.8 C 63.6 54.4 63.6 55.6 62.5 56.2 L 51.5 62.0 C 50.4 62.7 49 61.8 49 60.5 Z" />

    <!-- 7. Live Ruby Beacon -->
    <path
        android:fillColor="#FF4D5E"
        android:pathData="M 75 23.5 m -2.5 0 a 2.5 2.5 0 1 0 5 0 a 2.5 2.5 0 1 0 -5 0" />
</vector>
```

### 4.3 Launcher Icon VectorDrawables (`ic_launcher_*.xml`)

`app/src/main/res/drawable/ic_launcher_foreground.xml`:
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">

    <!-- 1. Ambient Background Aura behind Emblem -->
    <path
        android:pathData="M 54 54 m -28 0 a 28 28 0 1 0 56 0 a 28 28 0 1 0 -56 0"
        android:fillColor="#2638BDF8" />

    <!-- 2. Left Pillar (Stream Column with sculpted rounded top) -->
    <path
        android:pathData="M 30 33 C 30 29.7 32.7 27 36 27 L 42 27 L 42 81 L 30 81 Z"
        android:fillColor="#4338CA" />

    <!-- 3. Right Pillar (Stream Column with sculpted rounded bottom) -->
    <path
        android:pathData="M 66 27 L 78 27 L 78 75 C 78 78.3 75.3 81 72 81 L 66 81 Z"
        android:fillColor="#312E81" />

    <!-- 4. Ambient Drop Shadow Under Top-Left Overlap -->
    <path
        android:pathData="M 30 27 L 42 27 L 42 54 L 30 40 Z"
        android:fillColor="#66000000" />

    <!-- 5. Ambient Drop Shadow Under Bottom-Right Overlap -->
    <path
        android:pathData="M 66 54 L 78 68 L 78 81 L 66 81 Z"
        android:fillColor="#66000000" />

    <!-- 6. Dynamic Sweeping Diagonal Ribbon -->
    <path
        android:pathData="M 30 33 C 30 29.7 32.7 27 36 27 L 43 27 L 78 75 C 78 78.3 75.3 81 72 81 L 65 81 L 30 33 Z"
        android:fillColor="#00F2FE" />

    <!-- 7. Specular Top Glass Ridge on Ribbon -->
    <path
        android:pathData="M 31 32 L 42 27.5 L 58 49 L 55.5 50 L 40 30 L 31 32 Z"
        android:fillColor="#33FFFFFF" />

    <path
        android:pathData="M 30 33 C 30 30.3 32 28.2 34.5 27.5 L 42 27.5 L 42 27 L 36 27 C 32.7 27 30 29.7 30 33 Z"
        android:fillColor="#80FFFFFF" />

    <!-- 8. Luminous Play Core Shadow (Elevation) -->
    <path
        android:pathData="M 49 49.5 C 49 48.2 50.4 47.3 51.5 48.0 L 62.5 53.8 C 63.6 54.4 63.6 55.6 62.5 56.2 L 51.5 62.0 C 50.4 62.7 49 61.8 49 60.5 Z"
        android:fillColor="#55000000" />

    <!-- 9. Luminous Play Core Emblem -->
    <path
        android:pathData="M 49 48.5 C 49 47.2 50.4 46.3 51.5 47.0 L 62.5 52.8 C 63.6 53.4 63.6 54.6 62.5 55.2 L 51.5 61.0 C 50.4 61.7 49 60.8 49 59.5 Z"
        android:fillColor="#FFFFFF" />

    <!-- 10. Live Broadcast Ruby Beacon Glow Halo -->
    <path
        android:pathData="M 75 23.5 m -4.5 0 a 4.5 4.5 0 1 0 9 0 a 4.5 4.5 0 1 0 -9 0"
        android:fillColor="#55FF4D5E" />

    <!-- 11. Live Broadcast Ruby Beacon Core Dot -->
    <path
        android:pathData="M 75 23.5 m -2.5 0 a 2.5 2.5 0 1 0 5 0 a 2.5 2.5 0 1 0 -5 0"
        android:fillColor="#FFFF4D5E" />

    <!-- 12. Micro Specular Dot on Beacon -->
    <path
        android:pathData="M 74.6 23.1 m -0.9 0 a 0.9 0.9 0 1 0 1.8 0 a 0.9 0.9 0 1 0 -1.8 0"
        android:fillColor="#FFFFFF" />

</vector>
```

`app/src/main/res/drawable/ic_launcher_background.xml`:
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">

    <!-- Deep obsidian void base -->
    <path
        android:pathData="M0,0h108v108h-108z"
        android:fillColor="#07090E" />

    <!-- Ambient central glow -->
    <path
        android:pathData="M54,54 m-45,0 a45,45 0 1,0 90,0 a45,45 0 1,0 -90,0"
        android:fillColor="#1E193C" />

    <path
        android:pathData="M54,54 m-30,0 a30,30 0 1,0 60,0 a30,30 0 1,0 -60,0"
        android:fillColor="#251D4A" />

    <!-- Subtle futuristic circular halo ring -->
    <path
        android:pathData="M54,22 A32,32 0 1,0 54,86 A32,32 0 1,0 54,22"
        android:strokeWidth="0.75"
        android:strokeColor="#1E2A47" />

    <!-- Secondary outer ambient ring -->
    <path
        android:pathData="M54,16 A38,38 0 1,0 54,92 A38,38 0 1,0 54,16"
        android:strokeWidth="0.5"
        android:strokeColor="#131B2E" />

</vector>
```

### 4.4 Source SVG Vector Assets (`assets/logo*.svg`)
Full verbatim content of root `assets/logo.svg`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="512" height="512">
  <defs>
    <linearGradient id="indigoLeft" x1="0%" y1="0%" x2="0%" y2="100%">
      <stop offset="0%" stop-color="#4B3FB5"/>
      <stop offset="100%" stop-color="#3A2FA0"/>
    </linearGradient>
    <linearGradient id="indigoRight" x1="0%" y1="0%" x2="0%" y2="100%">
      <stop offset="0%" stop-color="#4B3FB5"/>
      <stop offset="100%" stop-color="#30268C"/>
    </linearGradient>
    <linearGradient id="ribbonGrad" x1="156" y1="136" x2="356" y2="376" gradientUnits="userSpaceOnUse">
      <stop offset="0%" stop-color="#06B6D4"/>
      <stop offset="50%" stop-color="#0EA5E9"/>
      <stop offset="100%" stop-color="#3B6FD8"/>
    </linearGradient>
    <linearGradient id="playGrad" x1="208" y1="206" x2="302" y2="256" gradientUnits="userSpaceOnUse">
      <stop offset="0%" stop-color="#22D3EE" stop-opacity="0.95"/>
      <stop offset="100%" stop-color="#38BDF8" stop-opacity="0.75"/>
    </linearGradient>
    <linearGradient id="shadowGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#000000" stop-opacity="0.55"/>
      <stop offset="100%" stop-color="#000000" stop-opacity="0.0"/>
    </linearGradient>
    <linearGradient id="bottomShadow" x1="0%" y1="100%" x2="0%" y2="0%">
      <stop offset="0%" stop-color="#000000" stop-opacity="0.40"/>
      <stop offset="100%" stop-color="#000000" stop-opacity="0.0"/>
    </linearGradient>
    <filter id="softGlow" x="-20%" y="-20%" width="140%" height="140%">
      <feDropShadow dx="0" dy="6" stdDeviation="12" flood-color="#06B6D4" flood-opacity="0.25"/>
    </filter>
  </defs>
  <!-- Background Canvas -->
  <rect width="512" height="512" rx="112" fill="#0B0D12"/>
  <circle cx="256" cy="256" r="200" fill="#141824" opacity="0.4"/>
  <g id="nis-logo-mark">
    <!-- Left Vertical Stem -->
    <path d="M156,136 L208,136 L208,376 L156,376 Z" fill="url(#indigoLeft)"/>
    <!-- Right Vertical Stem -->
    <path d="M304,136 L356,136 L356,376 L304,376 Z" fill="url(#indigoRight)"/>
    <!-- Left Stem Ambient Shadow -->
    <path d="M156,136 L208,136 L208,240 L156,176 Z" fill="url(#shadowGrad)"/>
    <!-- Right Stem Ambient Shadow -->
    <path d="M304,300 L356,336 L356,376 L304,376 Z" fill="url(#bottomShadow)"/>
    <!-- Diagonal Ribbon (Cyan to Blue) -->
    <path d="M156,136 L208,136 L356,376 L304,376 Z" fill="url(#ribbonGrad)" filter="url(#softGlow)"/>
    <!-- Integrated Play-Arrow Facet -->
    <path d="M208,206 L302,256 L208,306 Z" fill="url(#playGrad)"/>
    <!-- Top Clean Light Refraction Edge -->
    <path d="M156,136 L208,136 L208,140 L156,140 Z" fill="#FFFFFF" opacity="0.4"/>
  </g>
</svg>
```

---

## 5. Animation Implementation

### 5.1 Full Verbatim Code: `SplashScreen.kt`
From `app/src/main/java/com/nis/nistv/feature/splash/SplashScreen.kt`:

```kotlin
package com.nis.nistv.feature.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nis.nistv.NisTvApplication
import com.nis.nistv.core.designsystem.component.NisAnimatedSvgLogo
import com.nis.nistv.core.designsystem.theme.NisAccentCyan
import com.nis.nistv.core.designsystem.theme.NisAccentPrimary
import com.nis.nistv.core.designsystem.theme.NisBackground
import com.nis.nistv.core.designsystem.theme.NisTextPrimary
import com.nis.nistv.core.designsystem.theme.NisTextSecondary
import kotlinx.coroutines.delay

/**
 * Dedicated Splash Screen for NisTV.
 *
 * Runs as its own independent destination in the navigation graph (Screen.Splash),
 * completely decoupled from HomeScreen and HomeViewModel lifecycles.
 *
 * Features:
 * 1. World-class animated vector SVG logo with breathing pulse scale, rotating cosmic nebula,
 *    expanding broadcast radar ripple waves, specular light sweep, and live ruby beacon ping.
 * 2. Background channel database warm-up.
 * 3. Smooth transition directly to HomeScreen once initialized.
 */
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val contentAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        contentAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
        )

        // Warm up and ensure essential local database seed is ready in background
        try {
            val app = context.applicationContext as? NisTvApplication
            app?.container?.channelRepository?.ensureSeeded()
        } catch (_: Exception) {}

        // Ensure smooth viewing duration for the animated logo entrance and shine
        delay(2000)
        contentAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
        )
        onSplashFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0F1523),
                        Color(0xFF0B0D12),
                        NisBackground
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .alpha(contentAlpha.value)
        ) {
            // Animated Vector Logo
            NisAnimatedSvgLogo(
                logoSize = 124.dp
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "NisTV",
                    color = NisTextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "NEXT-GEN ENTERTAINMENT & STREAMING",
                    color = NisAccentCyan,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.4.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            CircularProgressIndicator(
                color = NisAccentPrimary,
                modifier = Modifier.size(28.dp),
                strokeWidth = 2.4.dp
            )

            Text(
                text = "Preparing entertainment experience...",
                color = NisTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
```

### 5.2 Full Verbatim Code: `NisAnimatedSvgLogo.kt`
From `app/src/main/java/com/nis/nistv/core/designsystem/component/NisAnimatedSvgLogo.kt`:

```kotlin
package com.nis.nistv.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nis.nistv.core.designsystem.theme.NisAccentCyan
import com.nis.nistv.core.designsystem.theme.NisAccentPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * World-class animated SVG logo for NisTV splash and loading screens.
 *
 * Rendered 100% natively via Compose Canvas without XML parsing or runtime reflection:
 * 1. Dynamic SVG Path Entrance Sequence:
 *    - Squircle base frame scales and glows into view.
 *    - Twin shaded pillars rise progressively from the bottom.
 *    - Cyber neon diagonal ribbon swooshes across with a trailing energy comet.
 *    - Central Play Core (▶) pops in with an elastic overshoot bounce.
 *    - Live broadcast ruby beacon flares into life.
 * 2. Continuous Living Vector Effects:
 *    - Organic floating & breathing pulse scale.
 *    - Rotating ambient cosmic aurora nebula behind the emblem.
 *    - Expanding broadcast radar ripple rings.
 *    - Specular high-gloss sheen light beam sweeping across the emblem.
 *    - Radiant energy stream pulsing continuously along the diagonal ribbon.
 *    - Live broadcast ruby beacon with dynamic pulsing ping wave.
 */
@Composable
fun NisAnimatedSvgLogo(
    modifier: Modifier = Modifier,
    logoSize: Dp = 120.dp
) {
    // ---- 1. SVG Entrance Motion Controllers ----
    val introDrawProgress = remember { Animatable(0f) }
    val playScaleAnim = remember { Animatable(0f) }
    val beaconIntensityAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // A. Draw base frame, pillars, and diagonal ribbon
        launch {
            introDrawProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing)
            )
        }

        // B. Pop central Play Core with bouncy spring after ribbon sweeps
        launch {
            delay(420)
            playScaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.58f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }

        // C. Ignite Live Ruby Beacon flare
        launch {
            delay(750)
            beaconIntensityAnim.animateTo(
                targetValue = 1.9f,
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
            )
            beaconIntensityAnim.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing)
            )
        }
    }

    // ---- 2. Continuous Ambient Motion Loop ----
    val infiniteTransition = rememberInfiniteTransition(label = "NisAnimatedLogo")

    // Organic floating & breathing scale
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Specular light beam sweep position [-0.5f .. 1.6f]
    val shimmerProgress by infiniteTransition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerProgress"
    )

    // Continuous energy pulse cruising down diagonal ribbon
    val ribbonEnergyProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ribbonEnergyProgress"
    )

    // Radar ripple wave 1
    val ripple1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple1"
    )

    // Radar ripple wave 2 (phase offset)
    val ripple2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, delayMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple2"
    )

    // Live Ruby Beacon heartbeat ping
    val rubyBeaconPulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rubyBeaconPulse"
    )

    // Ambient aurora rotation angle
    val glowAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "glowAngle"
    )

    Box(
        modifier = modifier.size(logoSize * 1.5f),
        contentAlignment = Alignment.Center
    ) {
        // A. Expanding Broadcast Radar Waves
        Canvas(modifier = Modifier.size(logoSize * 1.5f)) {
            val maxR = size.width * 0.46f
            // Wave 1
            if (ripple1 > 0f && ripple1 < 1f && introDrawProgress.value >= 0.5f) {
                val r1 = (maxR * (0.35f + ripple1 * 0.65f)).coerceAtLeast(1f)
                val a1 = (1f - ripple1).coerceIn(0f, 1f) * 0.35f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            NisAccentCyan.copy(alpha = a1 * 0.5f),
                            NisAccentPrimary.copy(alpha = a1),
                            Color.Transparent
                        ),
                        center = center,
                        radius = r1
                    ),
                    radius = r1,
                    center = center,
                    style = Stroke(width = 1.6.dp.toPx())
                )
            }
            // Wave 2
            if (ripple2 > 0f && ripple2 < 1f && introDrawProgress.value >= 0.5f) {
                val r2 = (maxR * (0.35f + ripple2 * 0.65f)).coerceAtLeast(1f)
                val a2 = (1f - ripple2).coerceIn(0f, 1f) * 0.35f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            NisAccentCyan.copy(alpha = a2 * 0.5f),
                            NisAccentPrimary.copy(alpha = a2),
                            Color.Transparent
                        ),
                        center = center,
                        radius = r2
                    ),
                    radius = r2,
                    center = center,
                    style = Stroke(width = 1.6.dp.toPx())
                )
            }
        }

        // B. Rotating Ambient Nebula Aurora Glow
        Canvas(
            modifier = Modifier
                .size(logoSize * 1.35f)
                .graphicsLayer(
                    rotationZ = glowAngle,
                    alpha = introDrawProgress.value
                )
        ) {
            val glowRadius = (size.width * 0.5f).coerceAtLeast(1f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NisAccentCyan.copy(alpha = 0.28f),
                        NisAccentPrimary.copy(alpha = 0.22f),
                        Color(0xFFA855F7).copy(alpha = 0.14f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = center
            )
        }

        // C. Core NisTV Logo with Path Drawing Entrance & Specular Sheen Sweep
        Box(
            modifier = Modifier
                .size(logoSize)
                .graphicsLayer(
                    scaleX = pulseScale,
                    scaleY = pulseScale
                )
                .drawWithContent {
                    drawContent()

                    // Specular light sweep diagonally across the logo
                    if (introDrawProgress.value >= 0.6f) {
                        val sweepWidth = size.width * 0.45f
                        val sweepPos = shimmerProgress * (size.width + sweepWidth)
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.28f),
                                    Color(0xFFE0F2FE).copy(alpha = 0.15f),
                                    Color.Transparent
                                ),
                                start = Offset(sweepPos - sweepWidth, sweepPos - sweepWidth),
                                end = Offset(sweepPos + sweepWidth, sweepPos + sweepWidth)
                            )
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            NisLogo(
                size = logoSize,
                drawProgress = introDrawProgress.value,
                ribbonEnergyProgress = ribbonEnergyProgress,
                playScale = playScaleAnim.value,
                beaconIntensity = beaconIntensityAnim.value
            )

            // D. Pulsing Live Broadcast Ruby Beacon Ping Wave (Top-Right of Emblem)
            if (beaconIntensityAnim.value >= 0.5f) {
                Canvas(
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = (-logoSize * 0.13f), y = (logoSize * 0.08f))
                ) {
                    val pingRadius = (size.width * 0.18f + rubyBeaconPulse * size.width * 0.38f).coerceAtLeast(1f)
                    val pingAlpha = (1f - rubyBeaconPulse).coerceIn(0f, 1f) * 0.75f
                    drawCircle(
                        color = Color(0xFFFF4D5E).copy(alpha = pingAlpha),
                        radius = pingRadius,
                        center = center,
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                }
            }
        }
    }
}
```

### 5.3 Comprehensive Animated Properties Table

| Element / Identifier | Animated Property | Start Value | End Value | Duration | Delay | Easing / Spec | Repeat Mode | Trigger / Driver |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `SplashScreen` content column | `contentAlpha` (entrance) | `0f` | `1f` | 500 ms | 0 ms | `tween(500, easing = FastOutSlowInEasing)` | None (one-shot) | `LaunchedEffect(Unit)` |
| `SplashScreen` content column | `contentAlpha` (exit) | `1f` | `0f` | 300 ms | Post-delay | `tween(300, easing = FastOutSlowInEasing)` | None (one-shot) | Triggered after `delay(2000)` |
| `NisAnimatedSvgLogo` | `introDrawProgress` | `0f` | `1f` | 1100 ms | 0 ms | `tween(1100, easing = FastOutSlowInEasing)` | None (one-shot) | Coroutine inside `LaunchedEffect(Unit)` |
| `NisLogo` Squircle Base | `baseAlpha` | `0f` | `1f` | ~1100 ms | 0 ms | Derived: `introDrawProgress.coerceIn(0f, 1f)` | None | Driven by `introDrawProgress` |
| `NisLogo` Squircle Border | `borderAlpha` | `0f` | `1f` | ~495 ms | 55 ms | Derived: `((introDrawProgress - 0.05f) / 0.45f).coerceIn(0f, 1f)` | None | Active when `introDrawProgress` is 0.05..0.50 |
| `NisLogo` Center Glow | `glowAlpha` | `0f` | `1f` | ~550 ms | 165 ms | Derived: `((introDrawProgress - 0.15f) / 0.5f).coerceIn(0f, 1f)` | None | Active when `introDrawProgress` is 0.15..0.65 |
| `NisLogo` Vertical Pillars | `pillarProgress` (`clipRect` vertical rise) | `0f` (bottom `y=81`) | `1f` (top `y=27`) | ~495 ms | 220 ms | Derived: `((introDrawProgress - 0.2f) / 0.45f).coerceIn(0f, 1f)` | None | Active when `introDrawProgress` is 0.20..0.65 |
| `NisLogo` Diagonal Ribbon | `ribbonProgress` (`clipRect` diagonal wipe) | `0f` | `1f` | ~550 ms | 385 ms | Derived: `((introDrawProgress - 0.35f) / 0.5f).coerceIn(0f, 1f)` | None | Active when `introDrawProgress` is 0.35..0.85 |
| `NisLogo` Play Core (▶) | `playScaleAnim` | `0f` | `1f` | Settles ~500 ms | 420 ms | `spring(dampingRatio = 0.58f, stiffness = Spring.StiffnessMediumLow)` | None (elastic bounce) | Launched after `delay(420)` ms |
| `NisLogo` Ruby Beacon | `beaconIntensityAnim` (Stage 1 flare) | `0f` | `1.9f` | 260 ms | 750 ms | `tween(260, easing = FastOutSlowInEasing)` | None (one-shot) | Launched after `delay(750)` ms |
| `NisLogo` Ruby Beacon | `beaconIntensityAnim` (Stage 2 settle) | `1.9f` | `1.0f` | 380 ms | 1010 ms | `tween(380, easing = FastOutSlowInEasing)` | None (one-shot) | Follows Stage 1 completion |
| Logo Box Container | `pulseScale` | `0.96f` | `1.04f` | 1500 ms | 0 ms | `tween(1500, easing = FastOutSlowInEasing)` | `RepeatMode.Reverse` | `rememberInfiniteTransition` |
| Diagonal Specular Sheen | `shimmerProgress` (sweep coordinate) | `-0.5f` | `1.6f` | 2200 ms | 0 ms (renders at `introDrawProgress >= 0.6f`) | `tween(2200, easing = LinearEasing)` | `RepeatMode.Restart` | `rememberInfiniteTransition` |
| Ribbon Energy Stream | `ribbonEnergyProgress` (t on ribbon) | `0f` | `1f` | 1600 ms | 0 ms (renders at `ribbonProgress >= 0.7f`) | `tween(1600, easing = LinearEasing)` | `RepeatMode.Restart` | `rememberInfiniteTransition` |
| Radar Ripple Wave 1 | `ripple1` (radius & fade) | `0f` | `1f` | 1800 ms | 0 ms (renders at `introDrawProgress >= 0.5f`) | `tween(1800, easing = FastOutSlowInEasing)` | `RepeatMode.Restart` | `rememberInfiniteTransition` |
| Radar Ripple Wave 2 | `ripple2` (radius & fade) | `0f` | `1f` | 1800 ms | 900 ms offset | `tween(1800, delayMillis = 900, easing = FastOutSlowInEasing)` | `RepeatMode.Restart` | `rememberInfiniteTransition` |
| Ruby Beacon Ping Wave | `rubyBeaconPulse` (circle expansion) | `0f` | `1f` | 1100 ms | 0 ms (renders at `beaconIntensityAnim >= 0.5f`) | `tween(1100, easing = FastOutSlowInEasing)` | `RepeatMode.Restart` | `rememberInfiniteTransition` |
| Ambient Aurora Glow | `glowAngle` (Z-rotation) | `0f` | `360f` | 9000 ms | 0 ms | `tween(9000, easing = LinearEasing)` | `RepeatMode.Restart` | `rememberInfiniteTransition` |

---

## 6. Timeline

Assuming an already-seeded database warm check (<15 ms):

```
Time (ms)  Visual Action / State Transition
──────────────────────────────────────────────────────────────────────────────────────────
t = 0 ms   • Screen.Splash enters NavHost.
           • LaunchedEffect(Unit) in SplashScreen triggers contentAlpha: 0f -> 1f (500 ms).
           • LaunchedEffect(Unit) in NisAnimatedSvgLogo triggers introDrawProgress: 0f -> 1f (1100 ms).
           • Infinite transitions start (pulseScale, shimmerProgress, ribbonEnergyProgress, ripples, aurora, ping).
           • Squircle base frame begins fading in (#221A44 -> #07090E gradient).
           • ensureSeeded() begins executing on Dispatchers.IO.

t = 55 ms  • introDrawProgress crosses 0.05: Squircle glowing border (#00F2FE / #6366F1) begins rendering.

t = 165 ms • introDrawProgress crosses 0.15: Ambient radial center glow (#38BDF8 / #6366F1) begins expanding.

t = 220 ms • introDrawProgress crosses 0.20: Twin shaded pillars begin rising from bottom (y=81) to top (y=27) via clipRect.

t = 385 ms • introDrawProgress crosses 0.35: Dynamic diagonal cyber ribbon begins sweeping from top-left (30, 27) to bottom-right (78, 81).

t = 420 ms • delay(420) elapses in NisAnimatedSvgLogo: playScaleAnim begins spring bounce (dampingRatio = 0.58f).

t = 500 ms • contentAlpha reaches 1.0f (full column, title, subtitle, circular progress, and status text are fully opaque).

t = 515 ms • ensureSeeded() completes; SplashScreen coroutine enters delay(2000).

t = 550 ms • introDrawProgress crosses 0.50: Broadcast radar ripple waves 1 & 2 become visible, expanding outward.

t = 660 ms • introDrawProgress crosses 0.60: Specular light beam sweep becomes visible, sweeping diagonally across the logo.

t = 715 ms • introDrawProgress crosses 0.65: Both vertical pillars are fully revealed.

t = 750 ms • delay(750) elapses in NisAnimatedSvgLogo: beaconIntensityAnim begins Stage 1 flare (0f -> 1.9f over 260 ms).

t = 770 ms • introDrawProgress crosses 0.70: Traveling neon energy comet appears on the diagonal ribbon.

t = 880 ms • playScaleAnim reaches peak overshoot (~1.12x) and begins settling back to 1.0f.

t = 935 ms • introDrawProgress crosses 0.85: Diagonal ribbon wipe is fully complete.

t = 1010 ms • beaconIntensityAnim reaches 1.9f (peak ruby flare); Stage 2 settle begins (1.9f -> 1.0f over 380 ms).
            • beaconIntensityAnim >= 0.5f triggers appearance of top-right pulsing ruby beacon ping wave.

t = 1100 ms • introDrawProgress reaches 1.0f. Base emblem entrance choreography is fully drawn.

t = 1390 ms • beaconIntensityAnim finishes settling at 1.0f.

t = 1390..2515 ms • Static entrance is finished; ambient loops continue (breathing pulse, specular sheen, traveling ribbon energy, radar rings, ruby beacon ping, aurora rotation).

t = 2515 ms • delay(2000) concludes in SplashScreen.
            • contentAlpha starts exit fade: 1f -> 0f over 300 ms (FastOutSlowInEasing).

t = 2815 ms • contentAlpha reaches 0f.
            • onSplashFinished() invoked.
            • navController.navigate(Screen.Home.route) with popUpTo(Screen.Splash.route) { inclusive = true }.
            • Home Screen appears.
──────────────────────────────────────────────────────────────────────────────────────────
Total Splash Active Screen Time: ~2,815 ms (~2.8 seconds)
```

---

## 7. App Boot Work During Splash

### 7.1 Background Tasks Initiated at Startup
During application boot, two separate lifecycles trigger tasks:

1. **`NisTvApplication.onCreate()` (Independent `applicationScope` coroutines on Dispatchers.IO):**
   - `container.channelRepository.ensureSeeded()`: Checks if `channelDao.getChannelsCount() > 0`; if empty, decodes and inserts `seed/channels_seed.json`.
   - `container.downloadsRepairManager.runRepairIfNeeded()`: Validates and repairs downloaded media files and orphaned database entries.
   - `container.offlineMediaRepository.backfillPendingMetadata()`: Scans disk for missing poster/backdrop artwork and synchronizes TMDB metadata.
   - `container.networkMonitor.isOnline.collect { ... }`: Observes connectivity and re-runs pending series resolvers when connected.
   - `WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork("catalogue_periodic_sync", ...)`: Schedules 24-hour periodic IPTV catalogue sync.

2. **`SplashScreen.kt` (`LaunchedEffect(Unit)`):**
   - Calls `app?.container?.channelRepository?.ensureSeeded()` directly in its coroutine block:
     ```kotlin
     // Warm up and ensure essential local database seed is ready in background
     try {
         val app = context.applicationContext as? NisTvApplication
         app?.container?.channelRepository?.ensureSeeded()
     } catch (_: Exception) {}
     ```

### 7.2 What the Splash Waits For
- **Waited On:** `SplashScreen` suspends until `ensureSeeded()` returns. If this is a first install with cold database seeding, `importSeedIfNeeded()` decodes `channels_seed.json` (~14,000 lines) which may take 150–400 ms.
- **Not Waited On:** `SplashScreen` does **not** await `downloadsRepairManager`, `offlineMediaRepository`, `WorkManager`, or TMDB network requests.
- **Language Onboarding Gate:** `LanguageOnboardingDialog` is collected in `MainActivity.kt` via `userPreferences.hasSelectedLanguage`. It is rendered at the root of `NisTvAppNav` above the `Scaffold`. It does not pause or intercept the splash screen's timer.

### 7.3 Navigation Exit Code
In `MainActivity.kt` lines 343–353:

```kotlin
composable(Screen.Splash.route) {
    SplashScreen(
        onSplashFinished = {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Splash.route) {
                    inclusive = true
                }
                launchSingleTop = true
            }
        }
    )
}
```

### 7.4 Exit Transition
- **Composable Level:** `SplashScreen.kt` executes an internal alpha fade-out over 300 ms (`contentAlpha.animateTo(0f, tween(300, easing = FastOutSlowInEasing))`).
- **Navigation Level:** `composable(Screen.Splash.route)` does not specify custom enter/exit transitions; it relies on default Navigation Compose transitions into `Screen.Home.route`.

---

## 8. Layout and Styling

- **Logo Size:** `logoSize = 124.dp` passed to `NisAnimatedSvgLogo`.
  - Outer bounding box: `logoSize * 1.5f` = `186.dp` x `186.dp`.
  - Ambient aurora canvas: `logoSize * 1.35f` = `167.4.dp` x `167.4.dp`.
  - Emblem canvas: `124.dp` x `124.dp`.
  - Ruby ping wave: `24.dp` x `24.dp` positioned at `offset(x = -16.12.dp, y = 9.92.dp)`.
- **Background:** Vertical 3-stop gradient:
  ```kotlin
  Brush.verticalGradient(
      listOf(
          Color(0xFF0F1523), // Deep cosmic navy
          Color(0xFF0B0D12), // Obsidian base
          NisBackground      // Color(0xFF0B0D12)
      )
  )
  ```
- **System Bars & Window Insets:**
  - `enableEdgeToEdge()` invoked in `MainActivity.onCreate()`.
  - `Theme.NisTV` provides initial `#07090D` status and navigation bar color.
  - `NisTvTheme` forces dark system bar icons (`isAppearanceLightStatusBars = false`, `isAppearanceLightNavigationBars = false`) and applies `NisBackground` (`#0B0D12`).
  - `Scaffold` specifies `contentWindowInsets = WindowInsets(0, 0, 0, 0)`.
  - `SplashScreen.kt` does **not** add status bar or navigation bar padding, drawing edge-to-edge under system chrome.
- **Typography & Elements under Logo:**
  - `Column` spacing: `Arrangement.spacedBy(18.dp)`.
  - App Title: `"NisTV"`, `color = NisTextPrimary` (`#F2F4F8`), `fontSize = 28.sp`, `fontWeight = FontWeight.Bold`, `letterSpacing = 1.2.sp`.
  - Spacer: `2.dp`.
  - Tagline: `"NEXT-GEN ENTERTAINMENT & STREAMING"`, `color = NisAccentCyan` (`#38BDF8`), `fontSize = 10.5.sp`, `fontWeight = FontWeight.SemiBold`, `letterSpacing = 1.4.sp`.
  - Spacer: `6.dp`.
  - Progress Indicator: `CircularProgressIndicator(color = NisAccentPrimary /* #6C8CFF */, modifier = Modifier.size(28.dp), strokeWidth = 2.4.dp)`.
  - Status Subtext: `"Preparing entertainment experience..."`, `color = NisTextSecondary` (`#9AA3B5`), `fontSize = 12.sp`, `fontWeight = FontWeight.Medium`.
- **Dark / Light Behavior:** Hardcoded to dark mode. `NisTvTheme` ignores `isSystemInDarkTheme()` and always binds `DarkColorScheme`.
- **Orientation Lock:** Unlocked. `Screen.Splash` does not lock orientation. If device starts in landscape, splash centers within landscape bounds.

---

## 9. Rendering and Performance

- **Drawing Pipeline:**
  - Rendered 100% via Jetpack Compose `Canvas` (`androidx.compose.foundation.Canvas`), using `DrawScope` primitives (`drawRoundRect`, `drawCircle`, `drawPath`, `clipRect`, `drawRect`, `withTransform`).
  - No `ImageView`, `AndroidView`, XML vector inflation, or `LottieAnimationView`.
- **Recomposition vs Draw-Phase Layers:**
  - **Optimized Draw-Phase Layers:**
    - `pulseScale` applies via `Modifier.graphicsLayer(scaleX = pulseScale, scaleY = pulseScale)` (draw-phase only; no layout pass).
    - `glowAngle` applies via `Modifier.graphicsLayer(rotationZ = glowAngle, alpha = introDrawProgress.value)` (draw-phase only).
    - `shimmerProgress` applies via `Modifier.drawWithContent { ... }` (draw-phase only).
    - `ripple1` and `ripple2` are read inside `Canvas` lambda (`onDraw`), invalidating draw phase without triggering recomposition of the parent layout.
  - **Recomposition Bottleneck in `NisLogo.kt`:**
    - `NisLogo` accepts primitive `Float` values (`drawProgress`, `ribbonEnergyProgress`, `playScale`, `beaconIntensity`).
    - Every animation frame that updates these values causes a recomposition of the `NisLogo` composable function.
  - **Memory Allocation Churn in `NisLogo.kt`:**
    - Inside `Canvas { ... }`, the following paths are instantiated with `Path()` **on every draw frame**:
      - `val leftPillar = Path().apply { ... }` (line 130)
      - `val rightPillar = Path().apply { ... }` (line 153)
      - `val leftShadow = Path().apply { ... }` (line 176)
      - `val rightShadow = Path().apply { ... }` (line 192)
      - `val diagonalRibbon = Path().apply { ... }` (line 219)
      - `val topSheen = Path().apply { ... }` (line 244)
      - `val playShadow = Path().apply { ... }` (line 290)
      - `val playCore = Path().apply { ... }` (line 305)
    - Gradient `Brush` instances (`Brush.verticalGradient`, `Brush.linearGradient`, `Brush.radialGradient`) are also re-allocated every frame inside the draw scope instead of being cached or remembered.
- **State Management:**
  - Uses `remember { Animatable(0f) }` and `rememberInfiniteTransition()`.
  - `derivedStateOf` is not used.
- **Platform Compatibility:**
  - Compatible with `minSdk 24` (Android 7.0) through `targetSdk 34` (Android 14).
  - Does not rely on API 31+ RenderEffects, HardwareAccelerated blur, or Android 12 Splash Screen APIs.

---

## 10. Known Issues and Observations

1. **Abrupt System-to-Compose Splash Transition (Double Splash):**
   - The OS window splash displays a blank `#07090D` window (or a static adaptive icon on Android 12+).
   - When Compose renders, the system window splash abruptly terminates, and `SplashScreen` begins fading in its `#0F1523` gradient and logo over 500 ms.
   - Because `androidx.core:core-splashscreen` is absent, there is no shared exit animation or keep-on-screen synchronization.
2. **Hardcoded Unconditional `delay(2000)`:**
   - In `SplashScreen.kt` line 71, `delay(2000)` forces the user to wait a minimum of ~2.8 seconds on every single cold launch, even when all databases and caches are warm and ready in under 10 ms.
3. **Duplicate `ensureSeeded()` Execution:**
   - Database seeding is triggered in `NisTvApplication.onCreate()` on `applicationScope` AND called synchronously a second time inside `SplashScreen.kt`'s `LaunchedEffect(Unit)`.
4. **Draw-Scope Path Allocations (GC Pressure):**
   - Allocating eight new `Path` instances and multiple `Brush` objects per draw frame in `NisLogo.kt` creates continuous garbage collection pressure during the animation on 60 Hz / 120 Hz screens.
5. **Lack of Reduced-Motion Support:**
   - Neither `SplashScreen` nor `NisAnimatedSvgLogo` queries `Settings.Global.ANIMATOR_DURATION_SCALE` or system accessibility motion preferences. Animations run continuously even if the user has disabled animations in device settings.
6. **Orientation Change Restarts Splash Sequence:**
   - Because `Screen.Splash` does not lock orientation to portrait, rotating the device during the 2.8s splash sequence triggers a recomposition/re-creation that resets `contentAlpha` and re-runs the entire animation and delay from t=0.
7. **Unused / Dead Asset Files:**
   - `app/src/main/res/drawable/ic_nistv_logo.xml` is defined in resources but never referenced.
   - `app/src/main/assets/splash.png` (116 KB) is bundled in the APK assets but never loaded.
   - `app/src/main/assets/logo_1024.png` and `logo_512.png` are bundled but never referenced in code.

---

## 11. Test Coverage

The only test touching the splash or logo components is `app/src/test/java/com/nis/nistv/StartupTest.kt`:

```kotlin
package com.nis.nistv

import org.junit.Test
import org.junit.Assert.assertNotNull

class StartupTest {

    @Test
    fun testClassesLoad() {
        assertNotNull(Class.forName("com.nis.nistv.NisTvApplication"))
        assertNotNull(Class.forName("com.nis.nistv.MainActivity"))
        assertNotNull(Class.forName("com.nis.nistv.feature.home.HomeScreenKt"))
        assertNotNull(Class.forName("com.nis.nistv.feature.home.HomeViewModel"))
        assertNotNull(Class.forName("com.nis.nistv.core.designsystem.component.NisLogoKt"))
        assertNotNull(Class.forName("com.nis.nistv.core.designsystem.component.NisAnimatedSvgLogoKt"))
        assertNotNull(Class.forName("com.nis.nistv.feature.splash.SplashScreenKt"))
    }
}
```

- **What it asserts:** Confirms via Java reflection that `NisLogoKt`, `NisAnimatedSvgLogoKt`, and `SplashScreenKt` class bytecode exists on the classpath and can be loaded without runtime exceptions.
- **What is NOT covered:** No unit tests, Robolectric tests, or Compose UI tests verify animation timing, property interpolation, canvas drawing, seed completion synchronization, or navigation to `Screen.Home`.

---

## 12. Extension Points & Constraints

### 12.1 Files to Modify for a Splash / Logo Redesign
1. **Logo Geometry & Static Visuals:**
   - `app/src/main/java/com/nis/nistv/core/designsystem/component/NisLogo.kt`
   - Pre-allocate paths outside the draw loop (or pass normalized cached paths) to eliminate allocations.
2. **Animation Motion & Choreography:**
   - `app/src/main/java/com/nis/nistv/core/designsystem/component/NisAnimatedSvgLogo.kt`
   - Adjust `Animatable` targets, spring parameters (`dampingRatio`, `stiffness`), or infinite loop durations.
3. **Screen Orchestration & Duration:**
   - `app/src/main/java/com/nis/nistv/feature/splash/SplashScreen.kt`
   - Adjust entrance/exit fade durations or replace the hardcoded `delay(2000)` with a data-ready state collector.
4. **System Splash Integration:**
   - `app/src/main/res/values/themes.xml` and `MainActivity.kt`
   - If migrating to `androidx.core:core-splashscreen`, add `Theme.SplashScreen` in XML and call `installSplashScreen()` in `MainActivity.onCreate()`.

### 12.2 Critical Invariants to Preserve
- **Navigation Contract:** The route string `Screen.Splash.route` (`"splash"`) and callback `onSplashFinished: () -> Unit` must remain stable so `MainActivity.kt` can navigate cleanly to `Screen.Home.route`.
- **Data Readiness:** `channelRepository.ensureSeeded()` must finish before navigation to `HomeScreen` completes, preventing empty channel rails on first launch.
- **System Insets:** `Scaffold` in `MainActivity.kt` dynamically calculates bottom padding; keep `currentRoute == Screen.Splash.route` check intact so the bottom bar remains hidden and zero-padded during splash.
- **Lock-Free Launch:** The splash sequence must run asynchronously without blocking the Android main thread (UI thread) during database checks.
