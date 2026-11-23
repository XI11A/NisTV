# NisTV Revamped — Design System Specification
**Version:** 2.0 (Cinematic Obsidian)  
**Target Platform:** Android Jetpack Compose (Material 3, BOM 2024.12.01, minSdk 24)

---

## 1. Overview & Visual Identity

The NisTV design system follows the **Cinematic Obsidian 2.0** design language:
- **Canvas:** Ultra-deep, pure obsidian canvas (`NisBackground` `#0B0D12`).
- **Surface Hierarchy:** 4 discrete tonal layers (`NisBackground` -> `NisSurface` -> `NisSurfaceElevated` -> `NisSurfaceHigh`) defining spatial elevation without relying on heavy or unclipped `Modifier.shadow`.
- **Accent Palette:** Vibrant periwinkle primary (`NisAccentPrimary` `#7A8FFF`), streaming cyan (`NisAccentCyan` `#3CCBFF`), and tertiary violet (`NisAccentViolet` `#A978FF`).
- **Brand Gradient:** A 3-stop linear gradient (`listOf(NisAccentCyan, NisAccentPrimary, NisAccentViolet)`).
- **Legibility:** All text tokens meet or exceed WCAG AA (>= 4.5:1) against every surface container.
- **Multi-Script Harmony:** Centered line-height metrics (`LineHeightStyle.Alignment.Center`, `LineHeightStyle.Trim.None`) to ensure zero glyph clipping on regional South Asian scripts (Tamil, Devanagari, Telugu, Kannada, Malayalam).

---

## 2. Colour Tokens

### 2.1 Surface & Border Tokens
| Token | Hex | Alpha | Role / Description |
|---|---|---|---|
| `NisBackground` | `#0B0D12` | 100% | Base canvas for all screens and system splash |
| `NisSurface` | `#131823` | 100% | Primary card, channel item, and bottom bar base |
| `NisSurfaceElevated` | `#1A2130` | 100% | Raised tiles, modal dialogs, and sheets |
| `NisSurfaceHigh` | `#232C3F` | 100% | Popups, tooltips, and high-prominence containers |
| `NisBorder` | `#2A3347` | 100% | Standard 1dp structural border |
| `NisBorderSubtle` | `#1E2536` | 100% | Internal divider and card outline border |
| `NisCardHover` | `#202942` | 100% | Active / hover card background |
| `NisNavbarBg` | `#0B0D12` | 94% (`0xF0`) | Translucent blur background for bottom navigation |

### 2.2 Brand & Semantic Tokens
| Token | Hex | Role / Description |
|---|---|---|
| `NisAccentPrimary` | `#7A8FFF` | Primary interactive color (buttons, active tabs, focus rings) |
| `NisAccentPrimaryLight` | `#9DAEFF` | Primary hover / lighter tint |
| `NisAccentPrimaryPressed` | `#6377F0` | Tactile pressed button state |
| `NisAccentPrimaryContainer` | `#232C5E` | Subdued selection pill background |
| `NisAccentCyan` | `#3CCBFF` | HD resolution badges, live scrubber start, secondary accent |
| `NisAccentViolet` | `#A978FF` | Tertiary accent, brand gradient end |
| `NisLiveRed` | `#FF4D5E` | Live TV broadcast badge & pulsing indicator |
| `NisError` | `#FF5C6C` | Destructive actions, playback error alerts |
| `NisAccentAmber` / `NisWarning` | `#F5A524` | Star rating pills, warning banners |
| `NisLiveEmerald` / `NisSuccess` | `#34D399` | Online stream health indicator, completed checkmarks |

### 2.3 Text Tokens & Contrast Ratios
| Token | Hex | Contrast vs Bg (`#0B0D12`) | Contrast vs SurfaceHigh (`#232C3F`) | WCAG Rating |
|---|---|---|---|---|
| `NisTextPrimary` | `#F4F6FB` | 17.97:1 | 12.92:1 | AAA |
| `NisTextSecondary` | `#A9B2C6` | 9.13:1 | 6.57:1 | AA |
| `NisTextMuted` | `#8B94AA` | 6.40:1 | 4.60:1 | AA (>= 4.5:1) |

*Note: The legacy token `NisTextMuted` (`#6B7280`) was raised to `#8B94AA` to eliminate WCAG contrast failures across elevated containers.*

### 2.4 Scrim Tokens
| Token | Definition | Role |
|---|---|---|
| `NisScrimSoft` | `NisBackground` @ 40% (`#660B0D12`) | Subtle backdrop gradient tint |
| `NisScrimMedium` | `NisBackground` @ 70% (`#B30B0D12`) | Hero banner overlay, card metadata footer |
| `NisScrimStrong` | `NisBackground` @ 87% (`#DE0B0D12`) | Dialog backdrop dim, bottom sheet veil |
| `NisPlayerScrim` | `Color.Black` @ 85% (`#D9000000`) | Video player controls overlay |

### 2.5 Brand Gradients
- **`NisBrandGradient`**: `listOf(NisAccentCyan, NisAccentPrimary, NisAccentViolet)`
- **`NisBrandGradientSoft`**: `listOf(NisAccentCyan @ 40%, NisAccentPrimary @ 40%, NisAccentViolet @ 40%)`

---

## 3. Typography Scale

NisTV uses the **Inter** typeface (static weights 400, 500, 600, 700) bundled under `res/font/`.
- Licenced under the [SIL Open Font License 1.1](licenses/INTER_OFL_LICENSE.txt).
- Static TTF files: `inter_regular.ttf`, `inter_medium.ttf`, `inter_semibold.ttf`, `inter_bold.ttf`.

| Style Name | Size | Line Height | Weight | Tracking | Default Usage |
|---|---|---|---|---|---|
| `displayLarge` | 40sp | 48sp | Bold (700) | -0.5sp | Splash branding, hero titles |
| `displayMedium` | 32sp | 38sp | Bold (700) | -0.5sp | Prominent header titles |
| `displaySmall` | 24sp | 30sp | Bold (700) | -0.3sp | Section hero headers |
| `headlineLarge` | 28sp | 34sp | Bold (700) | -0.4sp | Details screen main title |
| `headlineMedium` | 22sp | 28sp | Bold (700) | -0.2sp | Rail / carousel section headers |
| `headlineSmall` | 20sp | 26sp | SemiBold (600) | 0.0sp | Dialog titles, sheet headings |
| `titleLarge` | 18sp | 24sp | SemiBold (600) | 0.0sp | Card modal titles |
| `titleMedium` | 16sp | 22sp | SemiBold (600) | 0.0sp | Channel & media item card titles |
| `titleSmall` | 14sp | 20sp | SemiBold (600) | 0.0sp | Episode titles, secondary card labels |
| `bodyLarge` | 16sp | 24sp | Normal (400) | 0.0sp | Long-form synopsis text |
| `bodyMedium` | 14sp | 20sp | Normal (400) | 0.0sp | Standard body copy, metadata lines |
| `bodySmall` | 12sp | 16sp | Normal (400) | +0.1sp | Timestamps, durations, taglines |
| `labelLarge` | 14sp | 20sp | SemiBold (600) | 0.0sp | Primary & secondary button labels |
| `labelMedium` | 12sp | 16sp | Medium (500) | +0.2sp | Filter chips, tab labels |
| `labelSmall` | 11sp | 16sp | SemiBold (600) | +0.3sp | Bottom navigation bar labels |

### 3.1 Extended Typography Tokens (`NisTextExtras`)
| Extra Token | Size | Line Height | Weight | Tracking | Role |
|---|---|---|---|---|---|
| `hero` | 26sp | 32sp | Bold (700) | -0.3sp | Details hero banner overlay |
| `micro` | 10sp | 12sp | Bold (700) | +0.4sp | LIVE badges, 4K/HD pills |
| `tagline` | 10.5sp | 14sp | Medium (500) | +1.4sp | Splash screen branded tagline |
| `mono` | 12sp | 16sp | Normal (400) | 0.0sp | Diagnostics overlay, bitrate stats |

### 3.2 South Asian Script Safety Sample Strings
The line-height style `LineHeightStyle(alignment = Center, trim = None)` guarantees zero clipping for ascenders/descenders across languages:
- **Tamil**: `"சன் டிவி HD — நேரலை ஒளிபரப்பு மற்றும் திரைப்படங்கள்"`
- **Devanagari (Hindi)**: `"स्टार प्लस HD — लाइव प्रसारण एवं धारावाहिक"`
- **Telugu**: `"ఈటీవీ హెచ్‌డీ — ప్రత్యక్ష ప్రసారం మరియు సినిమాలు"`

---

## 4. Shapes & Corner Radii

NisTV standardizes all corner rounding onto a 5-step semantic scale:

| Scale Token | Radius | Component Mapping |
|---|---|---|
| `extraSmall` | 6dp | `NisBadgeShape`, `NisSmallControlShape` (badges, live indicators, mini pills) |
| `small` | 10dp | `NisThumbnailShape` (channel logo tiles, compact previews) |
| `medium` | 14dp | `NisButtonShape`, `NisChannelCardShape`, `NisMediaCardShape` (buttons, cards, search input) |
| `large` | 18dp | `NisFeaturedCardShape`, `NisMiniPlayerShape` (hero banners, floating mini-player) |
| `extraLarge` | 24dp | `NisDialogShape`, `NisBottomSheetShape`, `NisSideDrawerShape` (top corners of modal sheets, alert dialogs, quick channel drawer) |
| `pill` | 999dp | `NisChipShape`, `NisPillShape` (filter chips, selection pills, pill buttons) |
| `pill` | 999dp | `NisChipShape` (filter chips, segmented category pills) |

> [!IMPORTANT]
> `NisMediaCardShape` (`RoundedCornerShape(14.dp)`) is the single canonical shape used by both Home poster/backdrop cards and the Media Details header to guarantee seamless shared-element morphing during navigation.

---

## 5. Spacing & Dimensions

The spacing system uses an 8-point grid with a 4dp micro-step:

| Token | Dp | Role |
|---|---|---|
| `scale4` / `extraSmall` | 4dp | Micro-gaps between badge dot and text, icon padding |
| `scale8` / `small` | 8dp | Compact intra-component padding, chip horizontal padding |
| `scale12` / `medium` | 12dp | Rail item spacing, card content padding |
| `scale16` / `standard` | 16dp | Phone screen horizontal padding, card internal padding |
| `scale20` | 20dp | Medium modal element separation |
| `scale24` / `large` | 24dp | Tablet screen padding, section vertical separation |
| `scale32` / `extraLarge` | 32dp | Hero banner top margin |
| `scale40` / `section` | 40dp | Section-to-section major rhythm |
| `scale48` | 48dp | Maximum grid gutters |

### Adaptive Dimensions
- **Touch Target**: Guaranteed minimum 48dp (`NisDimens.TouchTarget`).
- **Icon Sizing**: 20dp (small), 24dp (medium / action), 32dp (large header / dialog).
- **Max Content Width**: 840dp (`NisDimens.MaxContentWidth`) centered on tablets/foldables.
- **Screen Padding**: 16dp on phones (`screenWidthDp < 600`), 24dp on tablets (`screenWidthDp >= 600`).

---

## 6. Motion Tokens (`NisMotion`)

- **Durations**:
  - `DurationFastMs`: `120ms` (ripples, badge state changes, icon toggles)
  - `DurationNormalMs`: `220ms` (card press scales, tab indicator slides, sheet opens)
  - `DurationSlowMs`: `360ms` (modal sheet full expanses, scene crossfades)
- **Easing Curves**:
  - `EasingEmphasized`: `CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)` (enter/exit transitions)
  - `EasingStandard`: `CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)` (in-layout shifts)
- **Tactile Press Spring**:
  - `PressDampingRatio`: `0.7f`
  - `PressStiffness`: `800f`
  - `NisMotion.pressSpring<Float>()` for interactive press scaling

---

## 7. Guidelines: How to Add a Component

When adding or refactoring a UI component in NisTV:
1. **Colours**:
   - Never use `Color(0x...)` or raw hex constants in component code.
   - Read from `MaterialTheme.colorScheme` or `NisTheme.colors` / top-level design tokens (`NisSurface`, `NisAccentPrimary`, etc.).
2. **Typography**:
   - Use `MaterialTheme.typography` styles (`titleMedium`, `bodySmall`, `labelLarge`) or `NisTheme.textExtras`.
   - Never use numeric `.sp` font sizes directly in component layout code.
3. **Shapes**:
   - Reference `MaterialTheme.shapes` or named shape tokens (`NisButtonShape`, `NisMediaCardShape`, `NisBadgeShape`).
   - Do not instantiate arbitrary `RoundedCornerShape(X.dp)`.
4. **Touch Targets**:
   - Ensure every clickable control has a minimum hit area of 48x48dp via `Modifier.nisPressable` or `Modifier.defaultMinSize(minHeight = NisDimens.TouchTarget)`.
5. **Multi-Script & Contrast**:
   - Test text with both Latin and regional Indian scripts (Tamil / Devanagari) at `fontScale = 1.3f`.

---

## 8. Core Component Catalogue

| Component | File Path | Key Tokens & Properties |
|---|---|---|
| `Modifier.nisPressable` | `core/designsystem/component/NisPressable.kt` | Bounded ripple (`NisAccentPrimary`), 0.97 spring press scale, guaranteed 48dp target, reduced-motion safe |
| `NisPrimaryButton` | `core/designsystem/component/NisButton.kt` | Solid `NisAccentPrimary`, onPrimary (`#0B0D12`) text, `NisButtonShape` (14dp), min 48dp height, 38% disabled alpha |
| `NisSecondaryButton` | `core/designsystem/component/NisButton.kt` | `NisSurfaceElevated`, 1dp `NisBorderSubtle`, `NisButtonShape`, min 48dp height |
| `NisIconButton` | `core/designsystem/component/NisButton.kt` | 40dp visual container, 48dp interactive touch target (`nisPressable`) |
| `NisBookmarkButton` | `core/designsystem/component/NisBookmarkButton.kt` | Guaranteed 48dp hit area, `NisScrimMedium` backdrop, `NisAccentPrimary` when active |
| `NisLiveBadge` | `core/designsystem/component/NisBadge.kt` | `NisLiveRed` fill, white text, pulsing live beacon dot (reduced-motion safe) |
| `NisResolutionBadge` | `core/designsystem/component/NisBadge.kt` | `NisAccentCyan` text on 12% cyan container, `NisBadgeShape` (6dp) |
| `NisWarningBadge` | `core/designsystem/component/NisBadge.kt` | `NisWarning` text on 14% amber container, `NisBadgeShape` (6dp) |
| `NisCategoryChip` | `core/designsystem/component/NisBadge.kt` | `NisChipShape` (999dp), `primaryContainer` when selected, `surfaceElevated` + `borderSubtle` unselected, 48dp touch target |
| `NisChannelCard` | `core/designsystem/component/NisChannelCard.kt` | `NisChannelCardShape` (14dp), `NisBorderSubtle`, 10dp logo tile, 48dp favorite target, tactile press |
| `NisPosterMediaCard` | `core/designsystem/component/NisPosterMediaCard.kt` | `NisMediaCardShape` (14dp), `NisBorderSubtle`, `NisScrimMedium` rating pill, `nisSharedBounds` |
| `NisBackdropMediaCard` | `core/designsystem/component/NisBackdropMediaCard.kt` | `NisMediaCardShape` (14dp), `NisBorderSubtle`, multi-stop scrim gradient, `NisScrimMedium` rating pill |
| `NisSearchBar` | `core/designsystem/component/NisSearchBar.kt` | `NisSurfaceElevated`, 14dp radius, 1.5dp focus ring in `NisAccentPrimary`, 48dp clear button |
| `NisBottomNavigationBar` | `core/navigation/NisNavigation.kt` | `NisNavbarBg` (94% alpha), 24dp top radius, 1dp border, 64dp bar height, `NisAccentPrimaryContainer` pill indicator |
| `NisMiniPlayer` | `feature/player/components/NisMiniPlayer.kt` | `NisMiniPlayerShape` (18dp), `NisBorderSubtle`, 10dp thumbnail clip, 48dp controls, `NisBrandGradient` bottom accent line |
| `NisSkeletonLoader` | `core/designsystem/component/NisSkeletonLoader.kt` | Shimmer gradient progression (`surface` -> `elevated` -> `high`), static gradient on reduced motion |
| `NisEmptyState` | `core/designsystem/component/NisEmptyState.kt` | 48dp icon in 72dp `NisAccentPrimaryContainer` circle, type-scale text, `NisPrimaryButton` |
| `NisErrorState` | `core/designsystem/component/NisErrorState.kt` | 48dp icon in 72dp 12% `NisError` circle, type-scale text, `NisPrimaryButton` |
| `NisLoadingIndicator` | `core/designsystem/component/NisLoadingIndicator.kt` | Circular spinner in `NisAccentPrimary` with `NisBorderSubtle` track |
| `NisLinearProgressBar` | `core/designsystem/component/NisLoadingIndicator.kt` | Smooth animated progress bar with `progressSpec`, draw-phase evaluation, reduced-motion snap |
| `AppUpdateDialog` | `core/designsystem/component/AppUpdateDialog.kt` | `NisDialogShape` (24dp), `NisSurfaceElevated`, `NisBorderSubtle`, `NisScrimStrong` backdrop |
| `ChangelogDialog` | `core/designsystem/component/ChangelogDialog.kt` | `NisDialogShape` (24dp), `NisSurfaceElevated`, `NisBorderSubtle`, `NisScrimStrong` backdrop |
| `LanguageOnboardingDialog`| `core/designsystem/component/LanguageOnboardingDialog.kt`| `NisDialogShape` (24dp), `NisMediaCardShape` language tiles, `NisBrandGradient` button |

---

## 9. Design Guard & CI Enforcement

Automated tests prevent visual regression or token drift across the codebase:
1. **`DesignGuardTest` (`app/src/test/java/com/nis/nistv/DesignGuardTest.kt`)**:
   - `testNoUnauthorizedHardcodedColorsAcrossFeaturesAndComponents()`: Enforces zero unauthorized raw `Color(0x...)` occurrences in any feature screen or reusable component.
   - `testNoUnauthorizedAdHocRoundedCornerShapesAcrossFeaturesAndComponents()`: Enforces zero arbitrary `RoundedCornerShape(...)` instantiations outside `Shape.kt`.
   - `testNoUnauthorizedHardcodedFontSizesAcrossFeaturesAndComponents()`: Enforces zero unauthorized `fontSize = ...sp` literals across features and components.
   - `testNoDeprecatedNisAccentGreenUsages()`: Enforces zero usage of deprecated `NisAccentGreen` across all source files.
   - `testShapeTokensHierarchy()`: Validates all semantic shape tokens are instantiated and non-null.
2. **`ComponentDesignGuardTest` (`app/src/test/java/com/nis/nistv/ComponentDesignGuardTest.kt`)**:
   - Enforces 48dp minimum touch targets (`NisDimens.TouchTarget`), RTL `autoMirror` on directional icons, and motion durations/physics.
3. **`ContrastTest` (`app/src/test/java/com/nis/nistv/ContrastTest.kt`)**:
   - Mathematically verifies WCAG AA compliance (>= 4.5:1 for body copy, >= 3:1 for large text / UI elements) across all 4 surface tiers.
4. **`ResponsivenessAndA11yTest` (`app/src/test/java/com/nis/nistv/ResponsivenessAndA11yTest.kt`)**:
   - `testThemedMonochromeIconExistsAndDeclared()`: Validates vector drawable monochrome silhouette and declaration in adaptive launcher icons.
   - `testResponsiveDimensionTokens()`: Verifies `MaxContentWidth` (840dp), `TouchTarget` (48dp), `Scale24` (24dp), `Scale16` (16dp), `PosterAspectRatio` (2:3).
   - `testAdaptiveGridsOnDiscoverAndLiveTvAndFavorites()`: Ensures `GridCells.Adaptive` is active across Discover, Live TV, and Favorites screens.
   - `testDynamicTypeResilienceOnBannersAndHeaders()`: Ensures flexible `heightIn(min = ...)` bounds on hero banners and media details headers.
   - `testTabletMaxContentWidthCappingAppliedAcrossScreens()`: Validates centered content capping at 840dp across 12 major screens.
   - `testModalBottomSheetsUseConsistentShapeAndWidth()`: Verifies bottom sheets use `NisBottomSheetShape` and tablet width capping.
5. **`MotionDesignGuardTest` (`app/src/test/java/com/nis/nistv/MotionDesignGuardTest.kt`)**:
   - `testDurationTokensScale()`: Verifies 120ms (fast), 220ms (normal), 360ms (slow) token progression.
   - `testTactileSpringSpecifications()`: Enforces damping ratio 0.7f, stiffness 800f for button/card press, and damping 0.8f, stiffness 400f for item placement.
   - `testTransitionsReducedMotionSafety()`: Validates that dialog and sheet transitions produce valid non-null animations in both normal and reduced-motion modes.
   - `testPressableDrawPhaseEvaluation()`: Enforces draw-phase scale execution in `NisPressable` and lifecycle-aware debouncing.
   - `testLiveBadgePulsingDrawPhaseEvaluation()`: Enforces draw-phase alpha animation on pulsing live beacon dot.
   - `testLinearProgressBarDrawPhaseEvaluation()`: Validates lambda-based progress provider and reduced-motion accessibility fallback in `NisLinearProgressBar`.
   - `testBottomNavigationTabAnimation()`: Validates animated indicator pill width and draw-phase alpha in `NisBottomNavigationBar`.
   - `testMiniPlayerDismissUsesMotionTokens()`: Validates swipe-to-dismiss motion tokens in `NisMiniPlayer`.

---

## 10. Screen Polish, Tablet Responsiveness & Accessibility

### 10.1 Large Screen & Tablet Layouts
- **Centered Content Columns**: On devices wider than 840dp (tablets, foldables in tablet mode, desktop/landscape), all main screens wrap their primary scrolling columns/grids in `Modifier.widthIn(max = NisDimens.MaxContentWidth)` with centered horizontal alignment (`Alignment.CenterHorizontally` / `Alignment.TopCenter`).
- **Adaptive Column Counts**:
  - `DiscoverScreen`: `GridCells.Adaptive(minSize = 110.dp)` automatically scales from 3 columns on standard phones to 5–7 columns on large tablets.
  - `LiveTvScreen`: `GridCells.Adaptive(minSize = 160.dp)` comfortably scales channel tiles.
  - `FavoritesScreen`: `GridCells.Adaptive(minSize = 115.dp)` for movies/shows and `160.dp` for channels.

### 10.2 Dynamic Type & Font Scaling
- Any composable containing variable-length text (titles, taglines, badges, descriptions) uses `Modifier.heightIn(min = ...)` instead of rigid `.height(...)`.
- Allows graceful vertical expansion when users select 1.3x or 2.0x font scaling in Android Accessibility Settings, eliminating clipped or overlapping typography.

### 10.3 Android 13+ Themed App Icon
- Adaptive launcher icons in `mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml` define a `<monochrome>` layer pointing to `@drawable/ic_launcher_monochrome`.
- When users activate themed icons in launcher settings on Android 13+ (API 33+), the OS renders the clean NisTV brand silhouette tinted with the user's wallpaper dynamic palette.

---

## 11. Motion, Feedback & Reduced-Motion Safety

### 11.1 Motion Tokens & Physics (`Motion.kt`)
- **Durations:**
  - `DurationFastMs`: `120ms` for micro-interactions, ripples, badge toggles, and instant fades.
  - `DurationNormalMs`: `220ms` for tactile card press feedback, tab pill transitions, and dialog animations.
  - `DurationSlowMs`: `360ms` for bottom sheet expansions and major layout shifts.
- **Easing Curves:**
  - `EasingEmphasized`: `CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)` for purposeful entrances and dismiss gestures.
  - `EasingStandard`: `CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)` for color, alpha, and size transitions.
- **Tactile Springs:**
  - `pressSpring()`: `SpringSpec(dampingRatio = 0.7f, stiffness = 800f)` for crisp, bounce-free tactile card/button feedback.
  - `itemPlacementSpring()`: `SpringSpec(dampingRatio = 0.8f, stiffness = 400f)` for smooth list item animations.
- **Transitions:**
  - `dialogEnterTransition` / `dialogExitTransition`: Subtle scale (0.94x) + fade in normal mode; fade-only in reduced motion.
  - `sheetEnterTransition` / `sheetExitTransition`: Slide up/down + fade in normal mode; fade-only in reduced motion.

### 11.2 Draw-Phase Performance Optimization
To guarantee fluid 60/120fps animations without UI thread stutter or garbage collection churn:
- **Zero Per-Frame Recomposition:** Animated float/dp values are evaluated inside draw or layout phase lambdas (`Modifier.graphicsLayer { scaleX = ...; scaleY = ... }` or `LinearProgressIndicator(progress = { ... })`).
- **`Modifier.nisPressable`:** Scales content to 0.97x purely within `graphicsLayer` without recomposing the card's child hierarchy.
- **`NisLiveBadge`:** Pulses dot alpha in `graphicsLayer { alpha = ... }` without triggering recomposition loops during live stream viewing.
- **`NisLinearProgressBar`:** Feeds animated progress into a draw-phase lambda provider with standard easing.

### 11.3 Accessibility & Reduced-Motion Safety
- **System Check:** `isReducedMotionEnabled(context)` checks `Settings.Global.ANIMATOR_DURATION_SCALE == 0f`.
- **Graceful Fallbacks:**
  - When reduced motion is enabled, scale animations and infinite shimmers snap to fixed positions or fall back to short fades (`fadeIn` / `fadeOut`).
  - Dialogs and bottom sheets transition purely via subtle fades rather than dramatic slides or scale zooms.

---

## 12. Light Theme Palette & Dynamic Theme System

### 12.1 Light Theme Tokens (`Color.kt`)
The **Cinematic Light Theme** introduces a clean, daylight-friendly visual canvas while keeping the obsidian identity and contrast standards:

| Token | Hex | Alpha | Role / Description |
|---|---|---|---|
| `NisLightBackground` | `#F6F7FB` | 100% | Crisp, off-white canvas for all non-immersive screens |
| `NisLightSurface` | `#FFFFFF` | 100% | Pure white container for cards, items, and bottom bar base |
| `NisLightSurfaceElevated` | `#EEF0F7` | 100% | Raised surface for dialogs, bottom sheets, and chip backgrounds |
| `NisLightSurfaceHigh` | `#E2E6F0` | 100% | High-elevation surface for popups, menus, and tooltips |
| `NisLightBorder` | `#D5DAE6` | 100% | Structural container outline & card border |
| `NisLightBorderSubtle` | `#E4E8F2` | 100% | Subtle card divider, thumbnail outline |
| `NisLightCardHover` | `#E8EDF8` | 100% | Pressed feedback container tint |
| `NisLightNavbarBg` | `#FAFBFD` | 94% | Translucent navigation bar container |
| `NisLightNavbarBorder` | `#E4E8F2` | 100% | Navigation bar top divider border |
| `NisLightPrimary` | `#4C5FD5` | 100% | High-contrast indigo primary action color |
| `NisLightPrimaryLight` | `#6C7CE8` | 100% | Light primary variant for highlights and focus |
| `NisLightPrimaryPressed` | `#3B4BBF` | 100% | Darkened indigo pressed state |
| `NisLightPrimaryContainer` | `#E8EBFC` | 100% | Subtle primary selection container |
| `NisLightAccentCyan` | `#007B99` | 100% | Deep cyan for badges, pills, and informational tags |
| `NisLightAccentViolet` | `#7E4BD6` | 100% | Tertiary brand accent stop |
| `NisLightLiveRed` | `#D92534` | 100% | Live stream pulsing dot and urgent alerts |
| `NisLightError` | `#D92D3E` | 100% | Playback and network error messages |
| `NisLightAccentAmber` | `#9E5A00` | 100% | Ratings stars and warning badges |
| `NisLightLiveEmerald` | `#0E804D` | 100% | Online network indicator & download success |
| `NisLightTextPrimary` | `#0E1220` | 100% | High-contrast dark ink for headlines and titles |
| `NisLightTextSecondary` | `#4A5266` | 100% | Mid-contrast secondary metadata and descriptions |
| `NisLightTextMuted` | `#586277` | 100% | Tertiary muted text (>= 4.9:1 on all light surfaces) |

### 12.2 Light Palette WCAG AA Contrast Compliance
All text tokens in the light palette strictly achieve WCAG AA contrast (>= 4.5:1 for body, >= 3.0:1 for graphical UI objects) across all 4 light surface tiers:

| Text Token | on `LightBackground` (`#F6F7FB`) | on `LightSurface` (`#FFFFFF`) | on `LightSurfaceElevated` (`#EEF0F7`) | on `LightSurfaceHigh` (`#E2E6F0`) | WCAG AA Status |
|---|---|---|---|---|---|
| `NisLightTextPrimary` (`#0E1220`) | **16.2:1** | **17.4:1** | **15.3:1** | **13.9:1** | PASS (AAA) |
| `NisLightTextSecondary` (`#4A5266`) | **7.5:1** | **8.1:1** | **7.1:1** | **6.4:1** | PASS (AA) |
| `NisLightTextMuted` (`#586277`) | **5.7:1** | **6.1:1** | **5.4:1** | **4.9:1** | PASS (AA) |
| `NisLightPrimary` (`#4C5FD5`) | **4.8:1** | **5.1:1** | **4.5:1** | **4.1:1** | PASS (UI >= 3:1) |
| `onPrimary` on `LightPrimary` | **5.38:1** | — | — | — | PASS (AA >= 4.5:1) |

### 12.3 Theme Mode Configuration (`AppThemeConfig`)
- **Supported Modes:**
  - `AppThemeConfig.SYSTEM`: Follows Android system dark mode toggle (`isSystemInDarkTheme()`).
  - `AppThemeConfig.DARK`: Forces the Obsidian Dark theme (default setting).
  - `AppThemeConfig.LIGHT`: Forces the Cinematic Light theme.
- **Persistence & Reactivity:**
  - Saved to DataStore via `UserPreferences.appTheme` (`KEY_APP_THEME = stringPreferencesKey("app_theme")`).
  - Collected as Compose `State` in `MainActivity` and passed to `NisTvTheme(appTheme = appTheme)`.
  - Switches dynamically without Activity recreation.
  - Automatically updates window insets controller for status bar and navigation bar icon contrast (`WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark`).
- **User Control:** Accessible in Profile > "Playback & Display" via interactive filter chips.

### 12.4 Guaranteed Immersive Dark Surfaces (`NisImmersive*`)
To preserve true cinematic presentation, specific playback and brand-defining surfaces **always** remain dark regardless of whether the app is in Dark or Light mode:
- **Splash Screen:** System starting window and brand emblem animation stay `#0B0D12`.
- **Online Live TV Player:** Fullscreen player controls, channel zapper, and audio/subtitle selectors use `NisImmersivePlayerScrim` and `NisImmersiveSurface`.
- **Offline Video Player:** Gestures, controls HUD, brightness/volume indicators, and sleep timer sheets use `NisImmersiveBackground` and `NisImmersiveTextPrimary`.
- **Hero Billboard Scrims:** Top and bottom gradient scrims on `HomeScreen` remain obsidian scrims to ensure high contrast against bright video art.
- **In-App Trailer Dialog:** Modal video player card and controls use `NisImmersiveSurfaceElevated`, `NisImmersiveBorderSubtle`, and `NisImmersiveTextPrimary`.




