# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [2.0.3] - 2026-11-26

### Fixed
- **AC-3 / E-AC-3 / DTS Audio Track Playback (`ERROR_CODE_DECODER_INIT_FAILED`)**:
  - Added `org.jellyfin.media3:media3-ffmpeg-decoder:1.5.0+1` so `DefaultRenderersFactory` (`EXTENSION_RENDERER_MODE_PREFER`) software-decodes `audio/ac3`, `audio/eac3`, `audio/vnd.dts`, `audio/true-hd`, and other surround/lossless audio formats unsupported by stock Android `MediaCodec`.
  - Added graceful audio-track override fallback in `OfflinePlayerScreen` if `ERROR_CODE_DECODER_INIT_FAILED` occurs while switching audio tracks, reverting to the default track instead of halting playback.

## [2.0.2] - 2026-11-26

### Removed
- **Hidden Client-Side Daily Download Limit**:
  - Removed the default 10 GB daily download cap enforcement in `DownloadManager`, `UserPreferences` (default `dailyDownloadLimitGb` set to `0` / unlimited), and `AutopilotManager`, preventing unconfigured client-side `"Daily download limit exceeded"` pauses.

## [2.0.1] - 2026-11-26

### Fixed
- **Download Random Pause & Resume Loop**:
  - Fixed O(N^2) byte-usage inflation in `MultiThreadedDownloader` (`DownloadByteBatcher`) where DataStore flush deltas were not advancing the flushed baseline, causing single downloads to prematurely trip the 10 GB daily download limit, pause mid-download, and immediately re-pause on resume.
  - Serialized per-download pause/resume/retry operations in `DownloadManager` with per-ID mutexes and monotonic operation sequences so async `pauseDownload` updates cannot overwrite subsequent `resumeDownload` transitions, and cleared stale pause/error messages when starting or resuming.
  - Updated `DownloadsScreen` to display `errorMessage` on `PAUSED` downloads (e.g., Wi-Fi-only wait or daily limit reached) in addition to `FAILED` downloads.

## [2.0.0] - 2026-11-22

### Added
- **Next-Gen Modern UI Overhaul**:
  - Floating Island Glass Navigation dock with centered capsule pill shape (`NisPillShape`), active tab glow highlight, and dynamic scroll hiding.
  - Cinematic ambient lighting hero carousels with smooth segmented story progress bars and glass tag badges.
  - Refined tactile spring motion across media cards, channel tiles, and navigation items.

### Changed
- **Modern Segmented Download Engine (OkHttp + NIO FileChannel)**:
  - Replaced legacy `com.liulishuo.okdownload` suite with a modern Kotlin Coroutines + OkHttp + Java NIO `FileChannel` segmented engine.
  - Implemented concurrent positional writes (`FileChannel.write(buffer, position)` mapping to kernel `pwrite`), eliminating FUSE filesystem lock contention and speed degradation.
  - Retained full multi-threaded parallel downloading across 1-8 configurable worker threads with per-thread live progress visualization (`ThreadProgress`).
  - State persistence backed by `${filePath}.parts.json`, enabling seamless pause and resume across app restarts and dynamic signed URL renewals.

### Fixed
- **Download Progress Stuck at 99%**:
  - Fixed `DownloadEntity.progressPercent` to guarantee 100% when `status == COMPLETED` or `downloadedBytes >= fileSizeBytes`, preventing integer truncation of 99.9% from displaying 99% on completed files.
  - Ensured `MultiThreadedDownloader` synchronizes `channel.force(true)` to disk on completion, purges `.parts.json`, scans media with `MediaScannerConnection`, and updates Room with exact matching `downloadedBytes` and `fileSizeBytes`.

## [1.21.0] - 2026-11-19

### Added
- **Multi-Profile System ("Who's Watching?")**:
  - Support for up to 5 individual user profiles with dedicated avatars, preferences, watch history, favorites, watch later queue, continue watching playback progress, taste profile interactions, and program reminders.
  - New route `profiles` (`ProfilesScreen`): "Who's watching?" profile picker featuring animated 96dp avatars, manage mode, lock indicators, Kids badges, and "+" tile for profile addition.
  - New route `profile_edit` (`ProfileEditScreen`): profile customization with 12 local gradient/emoji avatar presets (`NisProfileAvatar`), name input (max 16 characters), preferred language and country chips, Kids profile toggle, Profile Lock toggle, and destructive profile deletion with cascading confirmation.
  - Quick Profile Switcher bottom sheet (`HomeProfileSwitcherSheet`) accessible directly from the Home screen top app bar with profile avatar indicator.
  - Dedicated Kids Profile enforcement via `KidsContentFilter`: restricts Live TV channels and VOD media to kid-friendly categories (TMDB genre IDs 16, 10751, 10762; Live TV categories: Animation, Education, Family, Kids), locks adult content, disables destructive download management, and gates Parental Controls, Backup & Restore, and Custom Playlists. Exiting a Kids profile requires the parental PIN if set.
  - Profile Lock: integrates with existing PBKDF2 Parental PIN and lockout mechanism to prevent unauthorized access to locked profiles.
  - Dynamic launcher shortcut ("Resume: last channel") now respects the active profile.
  - Zero-friction backwards compatibility: single-profile users experience no launch interruption or UX change; profile selector is triggered on launch only when multiple profiles exist and `ask_profile_on_launch` is enabled.

### Changed
- **Room Database Migration 10 -> 11**:
  - `profiles`: Added `isKids`, `isLocked`, `preferredCountry`, `historyPaused`, and `sortOrder` columns; ensured default profile row exists with non-destructive fallback.
  - `watch_later`: Rebuilt table with composite primary key `(profileId, id)` and foreign key constraint cascading from `profiles(id)`. Migrated existing rows to the default profile.
  - `media_interactions`: Added `profileId` column with index `(profileId, timestamp)` for taste profile isolation.
  - `program_reminders`: Added `profileId` column with index `(profileId)`.
  - `playback_progress`: Created new dedicated table with composite primary key `(profileId, downloadId)` and foreign keys referencing `profiles(id)` and `downloads(id)`; backfilled progress from `downloads` for active/completed items.
- **Repository & State Isolation**:
  - Implemented `ProfileRepository` / `DefaultProfileRepository` as an AppContainer singleton managing active profile state and lifecycle operations (create, update, delete, switch).
  - Wired `activeProfileId` dynamically across `WatchLaterRepository`, `ChannelRepository`, `IntelligentRecommendationEngine`, `ContinueWatchingHelper`, `ProgramReminderManager`, `HistoryAndPrivacyViewModel`, and `AutopilotManager`.
  - Updated `OfflinePlayerScreen` and `OfflinePlayerViewModel` to write and observe profile-specific `PlaybackProgressEntity`.
- **Backup & Restore v2**:
  - Upgraded backup schema to `schemaVersion = 2`, exporting profiles, playback progress, and reminders alongside favorites and playlists.
  - Maintained full backward compatibility for legacy v1 backups, automatically mapping v1 records to the active/default profile.

### Fixed
- **Download Pause & Resume Breakpoint Reset**:
  - Fixed an issue where pausing and resuming an active download caused it to restart from byte 0 instead of resuming from the already-downloaded byte offset.
  - Eliminated eager, proactive AFDS link regeneration on resume in `DownloadManager.resumeDownload`, which generated a new signed URL with a new expiration token, changed OkDownload's `DownloadTask.id`, invalidated the SQLite breakpoint store, and forced OkDownload's local check to discard the partial file.
  - Implemented OkDownload `BreakpointInfo` migration via `copyWithReplaceIdAndUrl` and `store.update` on URL renewals in `MultiThreadedDownloader` and retries in `DownloadManager`, preserving multi-thread chunk blocks and offsets across link refreshes.
  - Added orphaned breakpoint recovery in `MultiThreadedDownloader` to automatically salvage existing file chunks from previous sessions or URL shifts.
  - Fixed immediate progress persistence on pause in `DownloadManager.pauseDownload`, ensuring in-memory byte counts are committed to Room before updating status to `PAUSED`, preventing the UI and database from falling back to 0 bytes or stale 15s checkpoints.

## [1.20.0] - 2026-11-14

### Added
- **OkDownload High-Performance Multi-Threaded Engine Integration**:
  - Integrated `com.liulishuo.okdownload:okdownload:1.0.7`, `sqlite:1.0.7`, `okhttp:1.0.7`, and `ktx:1.0.7` as the core download pipeline in `MultiThreadedDownloader`.
  - Configured OkDownload singleton in `NisTvApplication` and `MultiThreadedDownloader` with `DownloadOkHttp3Connection.Factory` for connection reuse with the app's isolated download OkHttpClient.
  - Retained full live multi-thread UI breakdown (`ThreadProgress`) by mapping OkDownload's `BreakpointInfo` and `BlockInfo` chunks directly to the UI progress bars in `DownloadsScreen`.

### Fixed
- **App Hanging & Freezing During Active Downloads**:
  - Decoupled `DownloadsViewModel` UI state flow: completely isolated `completedDownloads` (running regex grouping on `Dispatchers.Default` only when completed media changes) and mapped in-memory `activeProgressMap` for live bytes and speed without SQLite queries.
  - Resolved UI thread starvation caused by continuous Room SQLite updates: throttled `downloadDao.updateProgress()` checkpoints in `DownloadManager` to once every 15 seconds during downloading, eliminating continuous Room table invalidations and recomposition loops.
  - Eliminated blocking filesystem disk walks: decoupled `getStorageStats()` and `File.walkTopDown()` from the UI thread combination loop into asynchronous background refreshes.
  - Batched Jetpack DataStore disk writes: accumulated downloaded bytes in `MultiThreadedDownloader` to flush at most once per 10 seconds or 5MB, and throttled daily limit checks to 10 seconds, eliminating DataStore mutex contention.
  - Throttled system `NotificationManager` Binder IPC calls (`updateNotification`) to at most once per 1000ms, preventing Android `system_server` IPC saturation.
  - Configured OkDownload with an isolated high-concurrency client dispatcher (`maxRequests = 64, maxRequestsPerHost = 24`) and 1000ms background callback interval (`setAutoCallbackToUIThread(false)`).
- **Download Speed Degradation & FUSE Filesystem Lock Contention**:
  - Replaced unbuffered concurrent `RandomAccessFile` direct writes with OkDownload's `MultiPointOutputStream` buffered asynchronous output streams, eliminating Android FUSE emulated storage inode lock contention across parallel threads.
  - Maintained full support for chunk persistence, HTTP Range detection, signed URL auto-renewal, and daily download limit enforcement.

## [1.19.2] - 2026-11-14

### Fixed
- **Search Bar Rapid Typing Letter Inversion & Cursor Jumping**:
  - Resolved asynchronous Compose state race condition in `SearchScreen` where rapid keystrokes (e.g. typing "Fire") caused the cursor to jump to index 0 and reverse character order (resulting in "iref").
  - Decoupled the search input field's immediate display state from the asynchronous 5-flow `combine` / `stateIn` pipeline in `SearchViewModel`, replacing it with synchronous, immediate local state via `rememberSaveable { mutableStateOf("") }`.
  - Upgraded `NisSearchBar` to manage an internal `TextFieldValue` with selection range preservation, ensuring keystroke updates happen synchronously on the UI thread without IME composition invalidation.
  - Added unit test `searchUiState_rapidTypingQuerySequence_maintainsCorrectQuery` in `SearchViewModelTest` verifying rapid sequential query updates.

## [1.19.1] - 2026-11-13

### Fixed
- **Multi-Threaded Download Concurrency & Concurrency Ramp-Up**:
  - Isolated `downloadOkHttpClient` with a dedicated `Dispatcher(maxRequests = 32, maxRequestsPerHost = 16)` and `ConnectionPool(16, 5, TimeUnit.MINUTES)` to prevent download worker threads from being throttled or queued behind UI image loading and TMDB API requests.
  - Implemented fast-ramp progress ticker (250ms interval for initial 4 ticks, settling to 1000ms) in `MultiThreadedDownloader` so that workers connecting after socket establishment and CDN seek delays report their bytes to the UI immediately.
  - Enforced strict HTTP status code validation in chunk workers (`response.code == 206 Partial Content`), preventing silent data corruption or stalled fallback if servers return full content (HTTP 200) on non-zero range requests.

## [1.19.0] - 2026-11-12

### Added
- **Multi-Threaded Live Chunk Progress Reporting & UI Breakdown**:
  - Implemented real-time per-chunk progress reporting from `MultiThreadedDownloader` to `DownloadManager`, `DownloadsViewModel`, and `DownloadsScreen`.
  - Added `ThreadProgress` model capturing chunk index, start/end byte offsets, downloaded bytes, total bytes, progress fraction, and completion status.
  - Exposed `activeThreadProgress: StateFlow<Map<String, List<ThreadProgress>>>` in `DownloadManager` with state persistence support from `.parts.json`.
  - Added real-time chunk status and individual thread progress bars inside `ActiveDownloadCard` on `DownloadsScreen`:
    - Header indicator displaying active chunk count (e.g. `THREADS (4 Chunks Active)`) and total percentage.
    - Per-thread row displaying thread index (`Thread 1`, `Thread 2`, etc.), completion badge (`Done` in emerald green), downloaded vs chunk total size, and percentage.
    - Dedicated per-thread `NisLinearProgressBar` displaying live chunk fill progress.

### Fixed
- **Media Details Screen Light Theme Adaptation**:
  - Fixed invisible / low-contrast text and hardcoded dark boxes identified in `Screenshot_20261112-132244.png`.
  - Updated "Runtime / Seasons" and release year metadata below poster to use `NisTheme.colors.textSecondary`.
  - Resolved invisible "Synopsis" and "Where to Watch" section headers by switching from static `NisTextPrimary` to `NisTheme.colors.textPrimary`.
  - Fixed synopsis body copy contrast by switching from static `NisTextSecondary` to `NisTheme.colors.textSecondary`.
  - Fixed episode list items (`EpisodeRowItem`): replaced hardcoded `#131823` (`NisSurface`) cards with adaptive `NisTheme.colors.surface`, `NisTheme.colors.borderSubtle`, and `NisTheme.colors.textPrimary`.
  - Fixed episode thumbnail container to use `NisTheme.colors.surfaceElevated`.
  - Fixed genre pills and episode radar notification bell button to use adaptive surfaces and border tokens.
  - Fixed floating back button to use adaptive surface with subtle border.
  - Fixed download selection dialogs and download started banner to use adaptive surfaces and primary/success colors.
- **Global Typography System Light Theme Inheritance (`Type.kt`)**:
  - Removed hardcoded dark tokens (`NisTextPrimary`, `NisTextSecondary`, `NisTextMuted`) from `NisTypography` and `NisTextExtras` text styles.
  - Standardized text styles to default to `Color.Unspecified`, allowing `Text` composables to automatically resolve to `LocalContentColor.current` (`NisLightColorScheme.onBackground` / `onSurface` in Light Theme).
  - Explicitly bound `NisSecondaryButton` text to `colors.textPrimary`, fixing white text on light buttons.

## [1.18.0] - 2026-11-12

### Added
- **Multi-Threaded Content Downloader Engine (`MultiThreadedDownloader`)**:
  - Implemented a parallel chunk downloader for media content in `DownloadManager`.
  - HTTP Range probe validates partial content support (`HTTP 206` / `Content-Range`) and determines file size upfront.
  - Automatically divides large media into parallel byte ranges across coroutines with independent `RandomAccessFile` file descriptors for high-throughput, lock-free parallel disk writes.
  - State persistence via `.parts.json` chunk state serialization (`kotlinx.serialization`), enabling resume of in-progress multi-threaded downloads without restarting chunks.
  - Automatic link renewal integration: proactively refreshes temporary signed links (e.g. AFDS) upon expiry during chunk fetches and retries seamlessly.
  - Resilient fallback: gracefully degrades to single-stream download with resume support when Range headers or Content-Length are unsupported.
  - Added user-configurable concurrency setting `downloadThreads` in `UserPreferences` and `ProfileScreen` under Data & Storage (options: 1, 2, 4, 6, 8 threads; default 4).
  - Integrated `downloadThreads` into `BackupRestoreManager` and backup serialization.

### Fixed
- **Player Screen & Overlay Light Theme Support**:
  - Resolved issue where player screens remained dark/unreadable in Light Theme during portrait mode playback.
  - Migrated `PlayerScreen` portrait bottom details (channel info, action buttons, metadata, related channels) and dialogs to dynamic `NisTheme.colors.*`.
  - Updated `OfflinePlayerScreen` portrait details, metadata badges, file info card, and action buttons to dynamic theme colors.
  - Adapted player drawers, bottom sheets, and modal dialogs to support light theme cleanly:
    - `LiveEpgCard`: Adaptive surface, subtle border, on air indicator, and timeline schedule container.
    - `ChannelQuickDrawer`: Adaptive container, filter chip row, search textfield, channel rows, and resolution badge.
    - `TrackSelectorBottomSheet`: Adaptive surface elevated container, tab pill switcher, and audio/subtitle track rows.
    - `PlaybackSpeedSheet` & `SleepTimerSheet`: Adaptive bottom sheet container, drag handle, and option selection rows.
    - `ChannelJumpDialog`: Adaptive elevated dialog, subtle borders, text fields, and buttons.
    - `ScreencastDialog`: Adaptive dialog background, display status card, action rows, and dismiss icon.
    - `OfflinePlayerTrackDialog`: Adaptive dialog card, tab strip, audio/subtitle cards, delay controls, custom preview text box, and style chips.
    - `OfflineMediaPlayerDialog`: Adaptive fallback dialog surface and text colors.
  - Preserved immersive dark video canvas (16:9 viewport and landscape fullscreen video scrims) across both themes.

## [1.17.1] - 2026-11-12

### Fixed
- **Comprehensive Light Theme Adaptation Across All Screens & Components**:
  - Resolved issue where app screen canvases, cards, and headers remained pitch black in Light Theme due to static token references.
  - Migrated core design system components to dynamic tokens (`NisTheme.colors.*`): `NisSurface` (`NisCardSurface`, `NisElevatedSurface`), `NisSectionHeader`, `NisBadge`, `NisCategoryChip`, `NisButton`, `NisChannelCard`, `NisPosterMediaCard`, `NisBackdropMediaCard`, `NisSearchBar`, `NisEmptyState`, `NisErrorState`, `NisSkeletonLoader`, and `NisMiniPlayer`.
  - Updated all feature screens to use dynamic background, surface, text, and border tokens: `HomeScreen`, `DiscoverScreen`, `LiveTvScreen`, `PlaylistsScreen`, `SearchScreen`, `FavoritesScreen`, `DownloadsScreen`, `OfflineDetailsScreen`, `GuideScreen`, `ProfileScreen`, `HistoryAndPrivacyScreen`, `ParentalControlsScreen`, and `BackupRestoreScreen`.
  - Seamlessly adapted hero carousel and media detail header backdrops with adaptive vertical vignette fades matching `NisTheme.colors.background` for clean contrast in both modes.
  - Dynamically styled modal dialogs: `ChangelogDialog`, `AppUpdateDialog`, `LanguageOnboardingDialog`, `GoogleAuthGateDialog`, and `GoogleOAuthDialog`.
  - Preserved guaranteed immersive dark surfaces (`NisImmersive*`) across video players, player overlays, splash screen, and trailer dialogs in both light and dark themes.

## [1.17.0] - 2026-11-11

### Added
- **Cinematic Light Theme Palette**:
  - Introduced clean, high-contrast light theme tokens: `NisLightBackground` (`#F6F7FB`), `NisLightSurface` (`#FFFFFF`), `NisLightSurfaceElevated` (`#EEF0F7`), `NisLightSurfaceHigh` (`#E2E6F0`), and structural borders `NisLightBorder` (`#D5DAE6`), `NisLightBorderSubtle` (`#E4E8F2`).
  - Added light brand accents: `NisLightPrimary` (`#4C5FD5`), `NisLightPrimaryLight` (`#6C7CE8`), `NisLightPrimaryPressed` (`#3B4BBF`), `NisLightPrimaryContainer` (`#E8EBFC`), `NisLightAccentCyan` (`#007B99`), and `NisLightAccentViolet` (`#7E4BD6`).
  - Tuned semantic light tokens for WCAG AA compliance (>= 3:1 for UI components): `NisLightLiveRed` (`#D92534`), `NisLightError` (`#D92D3E`), `NisLightAccentAmber` (`#9E5A00`), `NisLightLiveEmerald` (`#0E804D`), and `NisLightTextMuted` (`#586277`, achieving >= 4.9:1 on all light surfaces).
  - Defined complete `NisLightColorScheme` conforming to Material 3 standards.
- **Dynamic Theme Mode Configuration (`AppThemeConfig`)**:
  - Added user-facing theme setting in Profile > Playback & Display (`System` / `Dark` / `Light`) backed by `UserPreferences.appTheme` with default `DARK` (leaving the current obsidian look 100% unchanged unless explicitly toggled).
  - Theme switching is applied dynamically in `MainActivity` and `NisTvTheme` without Activity recreation, immediately updating window status bar and navigation bar icon colors (`isAppearanceLightStatusBars`, `isAppearanceLightNavigationBars`).
- **Guaranteed Immersive Dark Surfaces (`NisImmersive*`)**:
  - Introduced dedicated immersive tokens (`NisImmersiveBackground`, `NisImmersiveSurface`, `NisImmersiveSurfaceElevated`, `NisImmersiveBorderSubtle`, `NisImmersiveTextPrimary`, `NisImmersiveScrimStrong`, `NisImmersivePlayerScrim`) ensuring Splash screen, online/offline media players, hero scrims, and In-App Trailer dialog always remain dark in both theme modes.
- **Theme Previews & Automated Contrast Tests**:
  - Expanded `ContrastTest` to verify WCAG AA compliance (>= 4.5:1 text, >= 3.0:1 UI accents) across all light surfaces.
  - Added light theme preview `NisDesignGalleryPreviewLight` to `NisDesignGalleryPreview.kt`.

## [1.16.3] - 2026-11-11

### Added
- **Motion System Foundations (`NisMotion`)**:
  - Centralized motion tokens in `Motion.kt`: `isReducedMotionEnabled(context)` accessibility check, `itemPlacementSpring`, `progressSpec`, `tabIndicatorSpec`, and transition specs `dialogEnterTransition`, `dialogExitTransition`, `sheetEnterTransition`, `sheetExitTransition`.
  - New `NisLinearProgressBar` component with lambda-based progress evaluation, standard easing (`NisMotion.progressSpec`), and accessibility snap fallback for reduced motion.
  - `MotionDesignGuardTest` automated test suite asserting motion durations, tactile spring parameters, draw-phase execution, and reduced-motion fallbacks.

### Changed
- **Tactile Press Feedback & Draw-Phase Optimization**:
  - Upgraded `nisPressable` to read animated scale directly inside `graphicsLayer { ... }` draw lambdas, eliminating per-frame composable recomposition during tactile bounces. Added lifecycle-aware `debounced` click protection.
  - Migrated `NisPosterMediaCard` and `NisBackdropMediaCard` to unified `nisPressable` with spring scaling.
  - Refactored `NisLiveBadge` pulsing indicator dot from `.alpha()` to `graphicsLayer { alpha = ... }` to eliminate infinite recomposition loops.
  - Upgraded `NisCategoryChip` selection transitions to animate container and border colors smoothly with `NisMotion.tabIndicatorSpec`.
  - Enhanced `NisBottomNavigationBar` with animated indicator pill width (0.dp -> 28.dp) and alpha transitions with reduced-motion accessibility support.
  - Replaced hardcoded drag and dismiss animations in `NisMiniPlayer` with `NisMotion.DurationNormalMs`, `EasingEmphasized`, and `EasingStandard`.
  - Upgraded bottom bar and mini-player `AnimatedVisibility` in `MainActivity` to utilize `NisMotion` durations, easing, and instant fade fallbacks when reduced-motion is requested.
  - Replaced all raw `LinearProgressIndicator` instances across `HomeScreen`, `DownloadsScreen`, `LiveEpgCard`, `ProfileScreen`, `HistoryAndPrivacyScreen`, and `AutoplayNextOverlay` with animated `NisLinearProgressBar`.

## [1.16.2] - 2026-11-11

### Fixed
- **HomeScreen Recommended Hero Carousel Visual Polish**:
  - Adjusted hero carousel height from 440dp to a spacious 500dp billboard (`Modifier.heightIn(min = 430.dp).height(500.dp)`), opening a generous 140dp visible artwork window that prevents category chips from colliding with actors' faces.
  - Upgraded vertical gradient scrim to include a 100% transparent middle window (`0.22f to Color.Transparent, 0.55f to Color.Transparent`), removing murky dark washes and keeping movie backdrops vibrant and crisp.
  - Modernized recommendation badges so only page 0 displays the personalized watch-history hook, while subsequent slides display punchy contextual badges ("CRITICS' CHOICE", "TOP MOVIE PICK", "TOP SERIES PICK", "RECOMMENDED FOR YOU") instead of repeating the same watch history string.
  - Centered overview description text (`textAlign = TextAlign.Center`) to align with the centered title, meta badges, and action buttons.

## [1.16.1] - 2026-11-11

### Fixed
- **HomeScreen Recommended Hero Carousel**:
  - Bound vertical constraints with explicit `Modifier.heightIn(min = 430.dp).height(440.dp)` on the carousel parent `Box`, eliminating unbounded cross-axis layout collapse inside `LazyColumn` where `HorizontalPager` and backdrop `AsyncImage` previously collapsed to 16:9 intrinsic height (~220dp) with a large blank gap below.
  - Stabilized `LazyColumn` item keys for the hero carousel (`hero_carousel_all`, `hero_carousel_movies`, `hero_carousel_series`), preventing the entire pager and image pipeline from tearing down and restarting whenever recommendations or watch history update.
  - Replaced the uncoordinated while-loop auto-advance timer with a reactive `LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress, items.size)` ticker that pauses during active user drag/swipe and grants a full 5.5-second dwell time before advancing.
  - Replaced forced `pagerState.scrollToPage(0)` on data changes with graceful boundary clamping (`pagerState.currentPage >= items.size`), maintaining the user's active page when recommendations refresh.
  - Added safe index access `items.getOrNull(page) ?: return@HorizontalPager` to eliminate potential `IndexOutOfBoundsException` during page count transitions.
  - Increased bottom padding on the hero content column to 24dp and positioned indicator dots cleanly at 8dp bottom padding to prevent overlap with the 48dp action buttons.

## [1.16.0] - 2026-11-11

### Added
- **Android 13+ Themed Monochrome Launcher Icon**:
  - Created vector drawable `ic_launcher_monochrome.xml` representing the crisp silhouette of the NisTV brand mark (left and right pillars, dynamic diagonal ribbon, live beacon dot).
  - Added `<monochrome android:drawable="@drawable/ic_launcher_monochrome" />` layer to `ic_launcher.xml` and `ic_launcher_round.xml` in `mipmap-anydpi-v26/` for seamless Material You dynamic theming on Android 13+.
- **Automated Responsiveness & A11y Unit Test Suite**:
  - Added `ResponsivenessAndA11yTest.kt` with 6 automated tests validating themed monochrome icon presence, responsive dimension tokens (`MaxContentWidth = 840.dp`, `TouchTarget = 48.dp`, `Scale24 = 24.dp`), adaptive grid configuration (`GridCells.Adaptive`), Dynamic Type height resilience (`heightIn(min = ...)`), tablet width capping across 12 screens, and modal sheet shapes.
- **Dimension Token Additions**:
  - Added `NisDimens.PosterAspectRatio` (`2f / 3f`) for standard 2:3 vertical media poster cards.

### Changed
- **Tablet & Foldable Responsiveness (`NisDimens.MaxContentWidth = 840.dp`)**:
  - Upgraded `DiscoverScreen` from fixed 3-column grid (`GridCells.Fixed(3)`) to responsive adaptive grid (`GridCells.Adaptive(minSize = 110.dp)`), ensuring optimal card sizing across phones, foldables, and wide tablet screens.
  - Centered and capped content columns with `widthIn(max = NisDimens.MaxContentWidth)` across all major screens: `HomeScreen`, `DiscoverScreen`, `LiveTvScreen`, `SearchScreen`, `DownloadsScreen`, `OfflineDetailsScreen`, `ProfileScreen`, `FavoritesScreen`, `PlaylistsScreen`, `HistoryAndPrivacyScreen`, `ParentalControlsScreen`, and `BackupRestoreScreen`.
  - Centered and constrained modal bottom sheets (`TonightPicksSheet`, `PlaybackSpeedSheet`, `SleepTimerSheet`) to `MaxContentWidth` with `NisBottomSheetShape` (24dp top radius).
- **Dynamic Type Scaling Resilience**:
  - Replaced rigid fixed heights containing text with `Modifier.heightIn(min = ...)` in `MediaDetailsHeader` (`min = 400.dp`), `HomeScreen` hero banners (`min = 410.dp` / `min = 430.dp`), and `OfflineDetailsScreen` header (`min = 340.dp`), preventing text clipping at 1.3x and 2.0x accessibility font scales.
- **Screen Polish & Visual Hierarchy**:
  - Established consistent 24dp vertical rhythm between sections (`Arrangement.spacedBy(NisDimens.Scale24)`) and 12dp within media rails.
  - Added scrolled border line `NisBorderSubtle` via `drawBehind` in `HomeScreen` cinematic top bar when content scrolls.
  - Upgraded floating detail screen back and options buttons to guaranteed 48dp hit targets (`NisDimens.TouchTarget`) using `Modifier.nisPressable`.
  - Upgraded vertical HUD percentages in `BrightnessVolumeHuds` to tabular monospace typography (`NisTheme.textExtras.mono`) to prevent visual jitter during slider adjustments.

## [1.15.1] - 2026-11-11

### Changed
- **Stage 3: Screen & Feature Hardcoded Values Migration ("Cinematic Obsidian 2.0")**:
  - **Color Tokens Migration**: Eliminated all raw hardcoded `Color(0x...)` occurrences across all screens (`HomeScreen`, `DiscoverScreen`, `LiveTvScreen`, `PlaylistsScreen`, `MediaDetailsScreen`, `PlayerScreen`, `DownloadsScreen`, `OfflineDetailsScreen`, `OfflinePlayerScreen`, `ProfileScreen`, `ParentalControlsScreen`, `BackupRestoreScreen`, `HistoryAndPrivacyScreen`, `SearchScreen`, `NisNavigation`, and all dialogs/sheets), migrating them to semantic tokens (`NisBackground`, `NisSurface`, `NisSurfaceElevated`, `NisSurfaceHigh`, `NisBorder`, `NisBorderSubtle`, `NisAccentPrimary`, `NisAccentCyan`, `NisAccentViolet`, `NisLiveRed`, `NisLiveEmerald`, `NisAccentAmber`, `NisScrimSoft`, `NisScrimMedium`, `NisScrimStrong`, `NisPlayerScrim`).
  - **Typography Scale Migration**: Migrated all raw `fontSize = ...sp` calls to Material 3 typography scale (`headlineLarge/Medium/Small`, `titleLarge/Medium/Small`, `bodyLarge/Medium/Small`, `labelLarge/Medium/Small`) or `NisTheme.textExtras` (`hero`, `tagline`, `micro`, `mono`), guaranteeing accessible, consistent dynamic-type-ready typography throughout.
  - **Shapes System Migration**: Replaced all ad-hoc `RoundedCornerShape(...)` instances with semantic shape tokens (`NisBadgeShape`, `NisThumbnailShape`, `NisButtonShape`, `NisMediaCardShape`, `NisChannelCardShape`, `NisFeaturedCardShape`, `NisMiniPlayerShape`, `NisDialogShape`, `NisBottomSheetShape`, `NisSideDrawerShape`, `NisChipShape`, `NisPillShape`, `NisShapes.*`).
  - **Deprecated Tokens Cleanup**: Completely removed all references to deprecated `NisAccentGreen` and `NisAccentGreenPressed` across feature screens, standardizing on `NisAccentPrimary`.
  - **Design System Extensions**: Added `NisPillShape` (`RoundedCornerShape(999.dp)`) and `NisSideDrawerShape` (`RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)`) to `Shape.kt`.
  - **Automated Design Guard Suite**: Implemented `DesignGuardTest` in `app/src/test/java/com/nis/nistv/DesignGuardTest.kt` with 5 automated test methods enforcing zero unauthorized hardcoded colors, zero ad-hoc shapes, zero unauthorized font sizes, and zero deprecated tokens across all features and components.

## [1.15.0] - 2026-11-11

### Added
- **Cinematic Obsidian 2.0 Component Library Restyle**:
  - `Modifier.nisPressable`: Tactile interaction modifier featuring bounded primary-tinted ripple, 0.97 spring press feedback (`NisMotion.pressSpring`), guaranteed 48dp minimum touch target area, accessibility reduced-motion awareness, and optional long-press haptic feedback.
  - `NisButton`: Rebuilt with solid `NisAccentPrimary`, onPrimary (`#0B0D12`) typography, active pressed color `NisAccentPrimaryPressed`, 14dp radius, and disabled state at 38% alpha; secondary button in `NisSurfaceElevated` with subtle 1dp border; `NisIconButton` with guaranteed 48dp hit area.
  - `NisBadge` & `NisCategoryChip`: Live badge with accessible pulsating beacon (respects reduced motion), cyan resolution pill on 12% fill, amber warning badge on 14% fill, and `NisCategoryChip` with full pill shape, `primaryContainer` fill when active, and 48dp touch target.
  - Media & Channel Cards (`NisPosterMediaCard`, `NisBackdropMediaCard`, `NisChannelCard`): Standardized to unified `NisMediaCardShape` (14dp), 1dp `NisBorderSubtle`, `NisScrimMedium` overlays for rating pills, and tactile press scaling.
  - `NisSearchBar`: Restyled with `NisSurfaceElevated`, 14dp corner radius, 1.5dp `NisAccentPrimary` active focus ring, and 48dp hit area on clear button.
  - `NisNavigation`: Redesigned `NisBottomNavigationBar` with `NisNavbarBg` (94% alpha), 24dp top radius (`NisBottomSheetShape`), 1dp top border, 64dp bar height + window insets, pill indicator in `NisAccentPrimaryContainer`, and `nisPressable` navigation tabs.
  - `NisMiniPlayer`: Restyled floating docking player with 18dp radius (`NisMiniPlayerShape`), 10dp thumbnail clip, 48dp controls, and bottom accent line in `NisBrandGradient`.
  - Dialogs & Overlays (`AppUpdateDialog`, `ChangelogDialog`, `LanguageOnboardingDialog`, `GoogleAuthGateDialog`, `GoogleOAuthDialog`, `AfdsAuthGateDialog`, `InAppTrailerDialog`): Restyled with `NisDialogShape` (24dp), `NisSurfaceElevated` container, `NisBorderSubtle`, and `NisScrimStrong` dimmed backdrops.
  - Skeleton & Progress Indicators (`NisSkeletonLoader`, `NisLoadingIndicator`, `NetflixLoadingScreen`): Shimmer gradient progression across surface tiers (`surface` -> `elevated` -> `high`), static gradient fallback on reduced motion, and sweep gradient in `NisBrandGradient`.
  - `NisEmptyState` & `NisErrorState`: 48dp iconography in 72dp container circles, typography from type scale, and solid primary buttons.
  - Directional Icon Mirroring (`NisIcons`): Enabled `autoMirror = true` on `Back`, `SkipNext`, and `SkipPrevious` for correct RTL layout support.
  - `NisDesignGalleryPreview`: Interactive gallery preview composables displaying all buttons, chips, badges, search bar, skeletons, and states at standard (1.0x) and accessibility-scaled (1.3x) font sizes.
  - `ComponentDesignGuardTest`: Automated JUnit suite guarding component token usage, RTL mirroring, motion durations, and ensuring zero unauthorized raw hex colors in design system components.

## [1.14.0] - 2026-11-11

### Added
- **Cinematic Obsidian 2.0 Theme Tokens (`Color.kt`)**:
  - Rebuilt color system with a 4-tier surface hierarchy: `NisBackground` (`#0B0D12`), `NisSurface` (`#131823`), `NisSurfaceElevated` (`#1A2130`), and `NisSurfaceHigh` (`#232C3F`).
  - Added new brand tokens: `NisAccentPrimaryPressed` (`#6377F0`), `NisAccentPrimaryContainer` (`#232C5E`), `NisAccentViolet` (`#A978FF`), and standard scrim tokens (`NisScrimSoft`, `NisScrimMedium`, `NisScrimStrong`, `NisPlayerScrim`).
  - Defined 3-stop `NisBrandGradient` (`cyan -> periwinkle -> violet`) and `NisBrandGradientSoft`.
  - Wired complete Material 3 `darkColorScheme` and extended token container `NisExtraColors` via `LocalNisExtraColors` and `NisTheme.colors`.
- **Static Inter Typography Scale (`Type.kt`)**:
  - Bundled static Inter font weights (Regular 400, Medium 500, SemiBold 600, Bold 700) in `res/font/` for deterministic offline rendering across Android 7.0+ (minSdk 24).
  - Explicitly defined all Material 3 typography styles with centered, untrimmed line-height metrics (`NisLineHeightStyle`) to eliminate ascender/descender clipping on complex South Asian scripts (Tamil, Devanagari/Hindi, Telugu).
  - Added `NisTextExtras` (`hero`, `micro`, `tagline`, `mono`) via `LocalNisTextExtras`.
- **Harmonic Shapes, Spacing & Motion (`Shape.kt`, `Spacing.kt`, `Motion.kt`)**:
  - Wired `NisShapes` into `MaterialTheme` with a 5-step scale (xs 6dp, sm 10dp, md 14dp, lg 18dp, xl 24dp).
  - Unified card and detail header shared-element morphing via `NisMediaCardShape`.
  - Added responsive 8-point spacing scale (4dp to 48dp), phone (16dp) / tablet (24dp) adaptive screen padding, and provided `LocalSpacing`.
  - Defined `NisMotion` tokens (120ms/220ms/360ms durations, emphasized/standard easings, press spring).
- **Design System Documentation & Quality Assurance**:
  - Added `docs/DESIGN_SYSTEM.md` comprehensive architecture and component authoring reference.
  - Added SIL Open Font License documentation under `licenses/INTER_OFL_LICENSE.txt`.
  - Added automated `ContrastTest` asserting WCAG AA (>= 4.5:1) compliance for all text/surface pairs and `ThemeTokensTest` verifying token consistency.

### Changed
- Elevated `NisTextMuted` from `#6B7280` to `#8B94AA` to eliminate WCAG AA contrast failures across all dark surfaces.
- Updated `NisAccentPrimary` to `#7A8FFF`, `NisAccentPrimaryLight` to `#9DAEFF`, and `NisAccentCyan` to `#3CCBFF`.
- Deprecated misnamed `NisAccentGreen` and `NisAccentGreenPressed` aliases with `@Deprecated(..., ReplaceWith(...))`.

### Fixed
- **Background Color Contradiction**:
  - Corrected `nistv_background` in `res/values/colors.xml` from `#07090D` to `#0B0D12`, synchronizing it with `themes.xml`, `SplashScreen.kt`, and `Color.kt`.

## [1.13.0] - 2026-11-10

### Added
- **In-App Official Trailer Player**:
  - Implemented `InAppTrailerDialog` with a privacy-enhanced HTML5 embed player (`youtube-nocookie.com`), allowing users to watch movie and series trailers directly inside NisTV without being redirected to external browser or apps.
  - Added modal UI with dark obsidian scrim, official trailer badge, movie title, trailer subtitle, loading indicator, and explicit ✕ close control with clean WebView lifecycle resource destruction.

### Changed
- **Content Details Hero Backdrop Enhancement**:
  - Significantly increased the landscape backdrop banner image height in `MediaDetailsHeader.kt` from 240dp to 340dp (+41.6% increase) and header container to 400dp, providing a much richer, cinema-grade presentation that showcases high-resolution backdrop art without getting occluded by the foreground poster card.

### Fixed
- **Mini-Player Maximize Intro Bypass**:
  - In `PlayerScreen.kt`, added context-aware detection for active channel playback (`isAlreadyActiveChannel`). When maximizing the currently watching stream from the docked mini-player, the 5-second branding video is now completely bypassed, allowing immediate uninterrupted viewing of the active live stream.

## [1.12.2] - 2026-11-09

### Fixed
- **Splash Sonic Mnemonic Audio Playback**:
  - Replaced fragile `MediaPlayer.create(..., 0)` session ID call with Media3 `ExoPlayer` audio pipeline and resilient `MediaPlayer` fallback, preventing native initialization exceptions.
  - Updated `SplashTimeline.shouldPlayStartupSound` to play splash audio across all device ringer modes (normal, vibrate, silent) on the media audio stream (`USAGE_MEDIA`), ensuring the brand mnemonic plays reliably when opening the application.
  - Linked audio cleanup to application `Lifecycle.Event.ON_STOP` rather than immediate Composable disposal, allowing the 1.9s sound to finish its natural reverb tail when transitioning from Splash to Home screen without clipping.

## [1.12.1] - 2026-11-09

### Fixed
- **Incomplete / Paused Download State on App Reopen**:
  - Fixed logic in `DownloadsRepairManager.kt` where any existing partial file on disk (`f.length() > 0`) was erroneously converting active, queued, or paused downloads to `COMPLETED` status on app startup, causing truncated files to display as 100% complete and play corrupted.
  - Active and paused downloads (`PAUSED`, `DOWNLOADING`, `QUEUED`) are now strictly protected and never converted to `COMPLETED` during repair.
  - Automatic self-healing: existing rows erroneously marked `COMPLETED` where `file.length() < fileSizeBytes` are automatically repaired back to `PAUSED` status with true downloaded byte counts so users can resume downloading without data loss.
- **Background Content Playing During Branding Video**:
  - In `OfflinePlayerScreen.kt`, local video playback is now strictly paused and muted (`exoPlayer.volume = 0f; exoPlayer.pause()`) while the 5-second branding video is playing. Content only starts playing after the branding intro completes.
  - In `PlayerScreen.kt` and `PlayerController.kt`, live TV streaming buffers silently in the background (`playWhenReady = false; volume = 0f`) during branding intro and begins playback only after the 5-second intro completes (or on immediate zap dismissal).

### Changed
- **Branding Video Player**:
  - Removed the "Skip Intro" pill and timer button from `PlayerBrandingIntro.kt` so the 5-second cinematic branding video plays in full without overlay distractions.

### Removed
- **Startup Brand Sound Preference Toggle**:
  - Removed the "Startup Brand Sound" switch row and divider from `ProfileScreen.kt`.
  - Configured splash sonic mnemonic audio to be always on upon opening the app.

## [1.12.0] - 2026-11-09

### Added
- **5-Second Cinematic NisTV Branding Intro (Offline & Online Video Players)**:
  - Bundled high-definition 1080p 16:9 branding video (`app/src/main/res/raw/nistv_branding.mp4`) generated directly from the visual geometry and mathematical choreography of the NisTV animated splash screen:
    - Deep obsidian `#0B0D12` void canvas with top subtle navy vertical ambient gradient `#0F1523`.
    - Eased squircle base frame fade and scale with radial gradient (`#221A44` -> `#131028` -> `#07090E`).
    - Progressive perimeter draw-on of glowing squircle border (`#00F2FE` -> `#6366F1` -> `#FFFFFF`).
    - Sculpted vertical stream pillars rising with smooth bottom clipping and ambient drop shadows.
    - Dynamic cyber neon diagonal ribbon wipe (`#00F2FE` -> `#0EA5E9` -> `#6366F1` -> `#A855F7`) with glowing leading head and glass ridge sheen.
    - Embedded luminous play core pop (▶) with spring damping dynamics and soft radial burst.
    - Live broadcast ruby red beacon (`#FF4D5E`) flaring with 3 finite sonar ping rings.
    - Specular light sheen sweep diagonally across the ribbon ridge.
    - Post-build breathing pulse followed by smooth cinematic exit fade.
    - Synchronized sonic brand identity mnemonic audio track from `nistv_splash.mp3` with ambient tail.
  - **Offline Player Integration (`OfflinePlayerScreen.kt`)**:
    - Pre-roll branding intro plays seamlessly upon entering the offline video player.
    - Discrete, sleek "Skip Intro" pill in the bottom right corner showing remaining countdown (`5s` -> `1s`).
    - Smooth auto-advance to target downloaded media at saved resume position (`lastPlaybackPositionMs`) after 5 seconds or upon skip.
  - **Online Live TV Player Integration (`PlayerScreen.kt`)**:
    - Pre-roll branding intro plays on initial channel launch, masking live stream manifest loading and buffering latency with zero UI stutter.
    - Discrete "Skip Intro" pill in bottom right corner.
    - Rapid channel zapping protection: swipes, channel jumps, and drawer switches immediately dismiss the intro so channel zapping remains instantaneous.
  - **Unit Testing**:
    - Comprehensive unit tests in `PlayerBrandingIntroTest.kt` verifying component loading, raw asset validity, countdown timer calculations, and zapping dismiss mechanics.

## [1.11.0] - 2026-11-09

### Added
- **Multi-View Dual Streaming (U5)**:
  - Watch two live TV channels simultaneously with responsive layout transitions (vertically stacked 16:9 tiles in portrait, side-by-side tiles in landscape).
  - Tap any tile to toggle audio focus with visual amber border highlight; unselected stream is automatically muted.
  - Long-press contextual action sheet for Fullscreen, Replace Channel, and Close Slot.
  - Double-tap opens any active multi-view channel into full single-player mode.
  - Optimized resource usage: primary stream capped at 720p (1280x720) and secondary stream capped at 480p (854x480) via ExoPlayer `TrackSelectionParameters`.
  - Device protection guards: low-memory detection (`ActivityManager.isLowRamDevice()`) with helpful notice, cellular data warning, background lifecycle pause, and complete resource release on departure.
  - Unified authentication and demo grace period enforcement across both active streams.
  - Pure unit test coverage in `MultiViewTest.kt` verifying aspect ratio calculations and slot audio state machine.
- **Stream Health Intelligence & Self-Healing Channels (U6)**:
  - Real-time stream telemetry engine measuring first rendered frame startup latency, playback successes, and fatal failover errors per stream URL hash key.
  - New Room database table `stream_health` introduced via safe additive migration `MIGRATION_9_10` (DB version 10).
  - Pure scoring algorithm `StreamHealthScorer` with 30-day exponential recency decay:
    - Dead channel rule: flags streams with 5+ consecutive failures over 3 or more days.
    - Automatic stream ranking: `ChannelRepository` dynamically orders source streams by reliability score so highest quality links connect first.
    - Reliability indicator dots on `NisChannelCard` (Emerald = Reliable, Amber = Degraded, Red = Dead).
  - New Profile settings: "Stream Reliability Dot" (default ON) and "Hide Dead Channels" (default OFF).
  - Unit test coverage in `StreamHealthTest.kt` and `DatabaseMigrationV10Test.kt`.
- **Data Saver Suite (U7)**:
  - **Listen Mode**: One-tap audio-only mode in live player (`setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true)`) that halts video decoding while keeping background audio playback and notification transport controls active.
  - Visual audio overlay featuring prominent headphones badge, savings explanation, and one-tap "Resume Video" button.
  - **Auto-Listen on Screen Off**: BroadcastReceiver listening for `ACTION_SCREEN_OFF` to automatically switch to audio-only streaming when active on metered cellular networks.
  - **Cellular Quality Cap**: Off / 480p / 720p resolution throttling via `setMaxVideoSize` when on mobile data.
  - **Data Usage Telemetry**: `DataUsageManager` recording streaming bytes via ExoPlayer `TransferListener` and download bytes into new table `data_usage_daily` (`MIGRATION_9_10`).
  - Monthly cellular bandwidth budget limits with 80% and 100% threshold alert notifications.
  - Pure helper calculations and unit tests in `DataSaverTest.kt`.
- **Channel Zapper (U8)**:
  - Intuitive horizontal swipe gestures in `PlayerGestureDetector` (swipe left for next channel, swipe right for previous channel) with zero interference on vertical volume/brightness drags.
  - In-memory `ZapContext` maintaining origin navigation playlist, active index, and context title without polluting Compose navigation arguments.
  - Dynamic `ZapOverlay` HUD displaying channel position, logo, clean name, and origin context with 2-second auto-dismiss.
  - Double-tap on HUD triggers instant toggle back to previous channel.
  - Numeric Channel Jump dialog enabling direct 1-4 digit channel jumping.
  - Automatic parental controls filtering: locked categories and NSFW channels are skipped during zapping and jump requests.
  - Comprehensive unit test suite in `ChannelZapperTest.kt`.

## [1.10.0] - 2026-11-09

### Added
- **Episode Radar (Tracked Series & New Episode Alerts)**:
  - Automatic series tracking for downloaded TV shows and series added to Watch Later.
  - Per-series notification toggle on Media Details and Offline Details screens, with upcoming episode air date badge ("Upcoming: SxxExx - {Date}").
  - Master "New Episode Alerts" toggle under Profile Settings (`isEpisodeRadarEnabled`).
  - Background checker `EpisodeRadarManager` scheduled via WorkManager (`EpisodeRadarWorker`, 12-hour periodic interval) querying TMDB API for `next_episode_to_air` and `last_episode_to_air`.
  - Grouped notifications on new episode broadcast with deep-link navigation directly to series details.
  - New Room table `tracked_series` with additive `MIGRATION_8_9` (schema version 9).
  - Pure unit tests in `EpisodeRadarTest.kt` verifying canonical episode key formatting, regex parsing, and chronological progression.
- **Smart Downloads Autopilot**:
  - Automated next-episode background downloads for TV series with configurable "keep ahead" window (1, 2, 3, or 5 unwatched episodes).
  - Network and battery safety guards: Wi-Fi only mode and Charging only constraints.
  - Storage safety floor enforcement: defaults to 2 GB free space preservation before scheduling downloads.
  - Daily download quota tracking: respects user daily data limits (`todayDownloadedBytes`).
  - Auto-delete watched episodes: configurable retention policy (1, 3, 7, or 14 days after completion) with 24-hour advance notification before cleanup.
  - Pure planner `AutopilotPlanner` with comprehensive unit test suite `AutopilotPlannerTest.kt`.
  - Immediate autopilot evaluation on 95%+ episode completion in `OfflinePlayerScreen`, alongside periodic background sync via `AutopilotWorker`.
  - New Room table `autopilot_rules` in Room schema version 9.
- **Dual Subtitles & Subtitle Delay (Offline Player)**:
  - Secondary subtitle track selection ("Dual Mode - Top") alongside primary bottom subtitle.
  - Precision subtitle timing delay controls (-10s to +10s in 50ms increments) for both primary and secondary tracks.
  - Custom Jetpack Compose subtitle overlay `DualSubtitleOverlay` with HarfBuzz Indic text shaping and rich style tag stripping.
  - High-performance, memory-efficient streaming cue parser `SubtitleCueParser` supporting SRT, WebVTT, and ASS/SSA formats with UTF-8 BOM tolerance.
  - Added `delayMs` and `role` columns to `subtitle_tracks` entity in Room schema version 9.
  - Dedicated unit tests in `SubtitleCueParserTest.kt` covering timing math, tag removal, Indic script rendering, and delay offsets.
- **"Tonight" Picks & "Surprise Me" (Home)**:
  - "✨ Tonight" dynamic chip in Home screen category filter pills opening the interactive `TonightPicksSheet`.
  - Contextual time-of-day affinity weighting via `TonightPicksHelper`:
    - Late night (10 PM - 4 AM): heavily favors shorter TV episodes and sub-90 minute movies.
    - Weekends (Friday evening through Sunday): favors cinematic feature-length movies (110+ minutes).
  - Offline-aware candidate filtering: automatically restricts recommendations to downloaded media when offline.
  - "🎲 Surprise Me" button with weighted random selection from current personalized picks.
  - Emits `VIEW` interaction to `IntelligentRecommendationEngine` to keep taste profiles fresh.
  - Pure unit tests in `TonightPicksTest.kt` verifying filtering, weighting, and deterministic seed generation.
- **Database & Architecture**:
  - Safe, additive Room database migration `MIGRATION_8_9` from version 8 to 9, verified by `DatabaseMigrationV9Test.kt`.

## [1.9.0] - 2026-11-09

### Added
- **Security & Token Hardening (SecureTokenStore & Permission Audit)**:
  - Removed unused legacy permissions `READ_CONTACTS` and `GET_ACCOUNTS` from `AndroidManifest.xml`.
  - Implemented `SecureTokenStore` with hardware-backed Android KeyStore AES-256 GCM encryption at rest (`AndroidKeystoreCipher`) and fallback JVM-compatible AES-GCM cipher (`AesGcmCipher`).
  - Automated migration of legacy plaintext auth tokens into secure storage on first run.
  - Complete zeroization/clearance of stored credentials on sign-out.
- **Parental Controls & PIN Protection**:
  - 4-6 digit numeric PIN protection with salted `PBKDF2WithHmacSHA256` hashing (10,000 iterations) and timing-safe equality checks.
  - Brute-force protection: 5 failed attempts trigger an automated 5-minute lockout with persistent cooldown countdown.
  - "Forgot PIN" recovery via device screen lock / biometric credential authentication (`KeyguardManager.createConfirmDeviceCredentialIntent`).
  - Granular category locking and strict NSFW channel filtering hidden across Live TV, Search, Home, and Recommendation queries until unlocked.
  - 15-minute temporary session unlock with automatic re-locking on app backgrounding (`onStop`).
  - Strict child safety in TMDB discovery: enforced `include_adult=false` on all media queries.
  - Favorites screen retains locked channels with an interactive lock badge, prompting for PIN entry before playback.
- **Backup & Restore (Portability)**:
  - Export user data (favorites, watch later items, custom playlists, preferences, and optional recent searches) to a portable JSON file via `ACTION_CREATE_DOCUMENT`.
  - Strict privacy protections: auth tokens, Google account identifiers, TMDB API keys, and PIN hashes are NEVER included in backup exports.
  - SHA-256 payload integrity checksums and schema versioning (`1.9.0`).
  - Import via `ACTION_OPEN_DOCUMENT` with interactive preview dialog supporting Merge (default additive) or Replace (full overwrite) modes.
  - Atomic Room database transactions (`withTransaction`) guaranteeing complete rollback if any corruption occurs during restore.
- **History & Privacy Controls**:
  - Dedicated "History & Privacy" settings screen (`HistoryAndPrivacyScreen`) under Profile.
  - Grouped watch history view bucketed into Today, Yesterday, and Earlier sections, alongside recently watched downloads.
  - Single-item history deletion and "Clear All Watch History" with confirmation dialog.
  - "Pause Watch History" switch to halt recording channel watch events and media recommendation interactions.
  - "Reset Recommendations" action to clear the `media_interactions` table and reset personalized taste profiles.
  - 100% on-device privacy guarantee statement with transparent data locality explanation.
- **Launcher App Shortcuts & Startup Sound Control**:
  - Static launcher shortcuts (`shortcuts.xml`): Live TV, Search, and Downloads.
  - Dynamic launcher shortcut ("Resume: {last channel}") published via `ShortcutManagerCompat` after channel playback.
  - Startup sound toggle in Profile settings (default enabled) with pure audio focus predicate (`SplashTimeline.shouldPlayStartupSound`) respecting device silent/vibrate ringer modes, active phone calls, and background music playback with `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK` ducking.
- **Testing & Verification**:
  - Added new unit test suites: `ParentalPinTest`, `BackupRestoreTest`, `SecureTokenStoreTest`, `StartupSoundTest`, and `HistoryPrivacyTest`.

## [1.8.0] - 2026-11-08

### Added
- **Custom Playlists (M3U / M3U8)**:
  - Import external and private live TV playlists via direct URL or local file picker (`.m3u` / `.m3u8`).
  - Automated 24-hour background synchronization powered by WorkManager (`PlaylistRefreshWorker`).
  - High-performance streaming parser (`M3uParser`) with strict 50 MB / 100,000 entry safeguards, `#EXTINF` attribute parsing (`tvg-id`, `tvg-name`, `tvg-logo`, `group-title`, `tvg-language`), CRLF tolerance, quote/comma in title separation tolerance, and collision-proof custom ID hashing (`custom_{playlistId}_{hash}`).
  - Sensitive URL token and auth credential masking in UI presentations and logs.
  - Dedicated "My Playlists" management screen (`PlaylistsScreen`) to refresh, rename, enable/disable toggle, and delete playlists with cascading cleanup of channels, streams, and favorites.
  - Integrated Live TV experience: "My Playlists" filter pill with per-playlist sub-filtering. Custom channels seamlessly support cards, favorites, failover, watch history, and category fallback EPG.
  - Strict sync isolation ensuring `CatalogueSyncWorker` and seed imports never prune or overwrite custom playlist channels.
- **TV Guide Grid & Program Reminders**:
  - Virtualized TV Guide grid (`GuideScreen`) accessible via TV Guide icon in the Live TV top bar.
  - Sticky 30-minute time slot header covering a ~3-hour scrollable window with synchronized horizontal scrolling across all channel rows.
  - Real-time "NOW" vertical indicator line showing current broadcast position.
  - Category filter chips matching Live TV genre selections.
  - Category-based fallback programs clearly badged as "Estimated"; reminders restricted to verified broadcast schedule entries.
  - Program reminders scheduled with exact alarms via `AlarmManager` (`SCHEDULE_EXACT_ALARM` with fallback on Android 12+), persistent in `program_reminders` table.
  - 5-minute pre-broadcast notification (`ProgramReminderReceiver`) with "Watch Now" action; checks DND and suppresses alerts if user is already watching that channel.
  - Pure function `buildEpgTimeline` resolving boundary clamping, program overlaps, and empty gap filling with unit test coverage.
- **Stream Diagnostics & Live Player Upgrades**:
  - Real-time Stream Diagnostics HUD overlay (`StreamDiagnosticsOverlay`) displaying video codec, resolution, bitrate, audio sample rate/channels, dropped video frames via `AnalyticsListener`, buffer health in seconds, stream source index/total, and host latency.
  - Instant Track Selector bottom sheet (`TrackSelectorBottomSheet`) for audio and subtitle switching via Media3 `TrackSelectionOverride`.
  - Quick-switch previous channel button remembering the last played channel during session.
- **Room Database Schema v8**:
  - Safe, non-destructive migration from v7 to v8 (`MIGRATION_7_8`).
  - Added `playlists` table (`id`, `name`, `sourceType`, `url`, `lastSyncedAt`, `channelCount`, `autoRefresh`, `enabled`).
  - Added `program_reminders` table (`id`, `channelId`, `programTitle`, `startEpochMs`, `notified`).
  - Added `playlistId` column to `channels` table with index `index_channels_playlistId`.
  - New test suites: `DatabaseMigrationV8Test`, `M3uParserTest`, `PlaylistSyncIsolationTest`, and `EpgTimelineTest`.

## [1.7.1] - 2026-11-07

### Fixed
- Fixed missing "Enter Fullscreen" button in the offline player when in portrait / non-fullscreen mode:
  - Added a dedicated, persistent Fullscreen button (`NisIcons.Fullscreen`) in the bottom-right corner of the video overlay alongside the playback time scrubber and "OFFLINE" badge, matching standard media player conventions.
  - Reorganized the portrait video overlay top bar: fixed the Back button to the left and the Fullscreen button to the far right, wrapping all secondary actions (Sleep Timer, Playback Speed, Audio Tracks, Captions, Aspect Ratio, Screencast, PiP, External Player) into a smooth horizontally scrollable row to prevent the Fullscreen button from being pushed off-screen on narrow displays.
  - Added a Quick Action Row directly beneath the video canvas in the portrait media header with one-tap access to "Fullscreen", "Subtitles", "Speed", "Timer", "PiP", and "External".
  - Replaced duplicate `Fullscreen` icon on the External Player action with `NisIcons.OpenInNew` across both portrait and landscape player layouts to eliminate icon ambiguity.

## [1.7.0] - 2026-11-07

### Added
- **Continue Watching (Home)**:
  - New "Continue Watching" row placed prominently above recommendation sections on the Home screen, displaying in-progress downloads with playback progress between 5% and 95%.
  - Series collapsing intelligence: displays a single card per series, showing either the active episode or the next downloaded episode across seasons.
  - Seamless portrait poster cards (115dp x 165dp) featuring offline-safe poster art, title, episode badges (e.g. "S2 E4"), progress bar, and remaining time.
  - Direct navigation to offline player on tap, with long-press or overflow menu option to "Remove from Continue Watching".
  - "Recent Channels" quick-access row displaying the last 10 distinct channels from watch history (newest first) with existing auth-gating preserved.
- **Offline Player Enhancements**:
  - Speed sheet controller supporting 0.5x, 0.75x, 1.0x, 1.25x, 1.5x, and 2.0x playback speeds (session-scoped).
  - Double-tap left/right seek gestures (-/+10s) with responsive animated ripple labels (`SeekRippleOverlay`), preserving single-tap controls toggle.
  - Vertical swipe gestures for brightness (left half) and volume (right half) with real-time HUD level indicators; original window screen brightness is cleanly restored upon exit.
  - Sleep timer with 15, 30, 45, 60-minute presets and "End of episode" mode, managed by `SleepTimerManager` with smooth 5-second volume fade-out and playback pause on expiry. Includes an active countdown chip in player controls.
  - Autoplay next episode: 10-second countdown overlay (`AutoplayNextOverlay`) with "Play Now" and "Cancel" actions when an episode finishes. Configurable via "Autoplay next episode" toggle in Profile settings (default ON).
  - Persistent aspect ratio mode remembered across videos via `UserPreferences`.
- **External Subtitles Support**:
  - Automatic detection of sidecar subtitle files (.srt, .vtt, .ass, .ssa) adjacent to video files or located in a `Subs/` subfolder, with auto-parsed language tags.
  - System document picker (`ACTION_OPEN_DOCUMENT`) to import external subtitle files, copied into app-private storage (`filesDir/subtitles/{downloadId}/`).
  - Robust multi-charset decoding (`SubtitleCharsetDetector`) supporting UTF-8, UTF-8 with BOM, UTF-16 LE/BE, and Windows-1252 to ensure flawless rendering of non-ASCII scripts (e.g., Tamil and Devanagari).
  - Subtitle management dialog supporting track selection (Off, embedded, and external sidecar/imported tracks).
- **Room Database Schema v7**:
  - Safe, non-destructive migration from v6 to v7 (`MIGRATION_6_7`).
  - Added `durationMs`, `lastWatchedAt`, `watchedCompleted`, and `hideFromContinueWatching` columns to `downloads` table.
  - Added new `subtitle_tracks` table with foreign key cascade deletion linked to `downloads`.
  - Comprehensive unit test suites: `DatabaseMigrationV7Test`, `ContinueWatchingTest`, `PlayerControlsTest`, and `SubtitleEncodingTest`.

## [1.6.2] - 2026-11-06

### Fixed
- Fixed an issue where users with existing AFDS accounts were unexpectedly redirected to the sign-up screen when attempting to log in:
  - Removed all automatic forced transitions to the Sign-Up tab upon login failure or `AfdsAccountNotFoundException`. Users now remain on the Sign-In screen with an informative inline error and an explicit clickable link (`Don't have an account yet? Create one here →`) if their account was truly not found.
  - Added seamless OTP fallback for "Continue with Google": accounts created via Email OTP return `404` to Google OAuth endpoints (`/auth/google`); NisTV now automatically falls back to dispatching a login OTP to that Google email address so users can enter their 6-digit code without being redirected to registration.
  - Added Cloudflare reverse proxy failover (`https://afds.hashhackers.com/proxyapi`) and dual-candidate email dispatch (raw email and normalized Gmail) across `requestLoginOtp`, `requestLoginOtpEmail`, `verifyLoginOtp`, `loginWithGoogle`, and `getProfile`.
  - Fixed Telegram OTP delivery fallback: if Telegram bot delivery fails or user has not linked their Telegram ID, the app cleanly falls back to email OTP without triggering signup.

## [1.6.1] - 2026-11-06

### Fixed
- Fixed an issue in AFDS sign-up where submitting with the default `"WELCOME"` invite code triggered an `"Invalid invite code."` error:
  - Enabled `encodeDefaults = true` in `AfdsAuthService`'s JSON serializer and removed default field assignment from `SignupRequestBody` so that `inviteCode` is always explicitly encoded into JSON payloads.
  - Normalized invite code input in `AfdsAuthGateDialog` and `AfdsAuthService` with `.trim().uppercase().ifBlank { "WELCOME" }` to prevent casing or whitespace mismatches.
  - Added unit test `signupRequestBody_serializesInviteCodeProperly` verifying explicit serialization of `inviteCode` even when matching the default value.

## [1.6.0] - 2026-11-06

### Added
- Native in-app AFDS Sign-Up flow in `AfdsAuthGateDialog`: users without an AFDS account can now register directly inside NisTV with zero external browser redirects.
- Automatic unregistered account detection: when a user attempts to sign in with an email not registered with AFDS, the dialog automatically transitions to the Sign-Up form with the email pre-filled and an informative prompt.
- Real-time email validation: added inline format checking that prevents `+` sign aliases and displays normalized Gmail previews (dot stripping and lowercase normalization).
- Backend Sign-Up integration in `AfdsAuthService`: added `requestSignupVerification` (`POST /api/request-verification`), `verifySignup` (`POST /api/verify`), and `AfdsAccountNotFoundException` with transparent Cloudflare reverse proxy failover (`https://afds.hashhackers.com/proxyapi`).
- Seamless onboarding handoff: upon successful sign-up verification, the app immediately dispatches a login code and transitions to the login OTP step, completing sign-in without requiring the user to re-enter their email.
- Unit test suite: added `AfdsAuthServiceTest` verifying email normalization logic, serialization contracts, and exception hierarchies.

## [1.5.2] - 2026-11-04

### Added
- Added two-state "Download Started" view to `AppUpdateDialog`: when updating the app, users now see a confirmation card with step-by-step installation instructions, animated icon, and an "Open Downloads" action instead of an abrupt dialog disappearance or blank screen.
- Added dedicated static `/download.html` and query-parameter (`?download=started` / `/download` / `#download`) routing on the website to auto-trigger APK downloads while presenting an interactive guide, preventing blank browser tabs on direct file links.
- Updated in-app changelog viewer modal (`ChangelogDialog.kt`) and website release notes with full version history for v1.5.2, v1.5.1, v1.5.0, and v1.4.0.

### Fixed
- Fixed blank screen issue when checking and starting app updates: integrated Android system `DownloadManager` in `AppUpdateChecker` to display real-time download progress in the system notification shade.

## [1.5.1] - 2026-11-04

### Fixed
- Fixed splash screen brand audio playback: routed audio stream to `USAGE_MEDIA` (`STREAM_MUSIC`) with full stereo volume `setVolume(1.0f, 1.0f)` instead of touch sound assistance sonification (`STREAM_SYSTEM`), preventing the audio from being muted by system settings or silent/vibrate mode.
- Replaced Vorbis audio asset with universally decoded MP3 format (`nistv_splash.mp3`) with fallback `AssetFileDescriptor` initialization.
- Removed gating on system reduced-motion settings so audio consistently triggers upon splash entrance.

### Removed
- Removed user preference toggle `isSplashSoundEnabled` from `UserPreferences`, `ProfileViewModel`, and `ProfileScreen` so splash sound identity plays directly without user configuration overhead.

## [1.5.0] - 2026-11-04

### Added
- Synchronized brand mnemonic audio (`nistv_splash.ogg`) for the NisTV splash screen logo entrance with rich spatial sound design (sub-bass swell, cyber sweep, play core chime kick, and ruby beacon flare).
- Added `KEY_SPLASH_SOUND_ENABLED` in `UserPreferences`, exposed via `ProfileViewModel` and accessible as a "Splash Screen Brand Sound" user toggle switch in `ProfileScreen`.
- Added accessibility reduced-motion suppression: sound is automatically muted if system animations are disabled (`animator_duration_scale == 0`).
- Added robust lifecycle management with `DisposableEffect` in `SplashScreen` stopping and releasing `MediaPlayer` resources upon screen exit or disposal.

### Changed
- Increased the main landscape backdrop banner height in `MediaDetailsHeader` from 200dp to 240dp (total header height 260dp to 300dp) for improved cinematic visual presentation.
- Balanced vertical poster card overlap against the taller backdrop (165dp poster overlapping 105dp onto the backdrop and extending 60dp below).

## [1.4.0] - 2026-11-03

### Changed
- Refined NisTV splash screen and animated logo choreography into a unified deterministic timeline (`BUILD_END_MS` = 1900 ms, `MIN_DISPLAY_MS` = 1900 ms, `HARD_TIMEOUT_MS` = 4000 ms, `EXIT_MS` = 280 ms).
- Replaced unconditional 2.8 s delay with a readiness-based exit orchestrator listening to a single `AppContainer.bootReady` signal, dropping warm start splash time to ~2.1 s.
- Updated system window theme handoff via `androidx.core:core-splashscreen:1.0.1` (`Theme.NisTV.Starting` with `#0B0D12` background and adaptive foreground mark), eliminating the double splash and color jump.
- Removed cluttered simultaneous loops: eliminated rotating radial aurora (no-op on radial gradients), infinite shimmer, ribbon energy comet loop, and continuous ripple waves.
- Staggered typography entrance for wordmark "NisTV" (900..1350 ms) and tagline (1100..1550 ms) via alpha and translationY with zero text relayout.

### Added
- Created `SplashTimeline.kt` as the single source of truth for all splash timing segments and custom easings (`EaseOutQuint`, `EaseInOutCubic`).
- Created `NisLogoGeometry.kt` providing pre-calculated paths, static brushes, and PathMeasure built once per size inside `drawWithCache`.
- Added support for system reduced-motion setting (`animator_duration_scale = 0`): displays instant final logo, suppresses breathing, and exits with a 150 ms fade.
- Added conditional loading hint with `CircularProgressIndicator` that fades in only if boot is delayed >400 ms after the build completion.
- Added debug-only `@Preview` `NisLogoTimelineScrubberPreview` with a slider scrubbing 0..1900 ms for visual animation tuning.
- Added comprehensive unit tests in `SplashTimelineTest` verifying timeline progress boundaries, monotonicity, easing endpoints, remaining wait math, and reduced-motion states.

### Fixed
- Fixed per-frame allocation churn and recompositions in `NisLogo`: parameters changed to `() -> Float` lambdas read strictly in the draw phase, eliminating all `Path()` and `Brush` allocations during draw.
- Fixed duplicate database seeding by removing redundant `channelRepository.ensureSeeded()` from `SplashScreen` and consolidating onto `AppContainer.bootReady`.

### Performance
- Zero heap allocations in `NisLogo` draw loop (`onDrawBehind`).
- Zero recompositions of `NisLogo` during the splash animation sequence.

## [1.3.2] - 2026-11-03

### Fixed
- Fixed fullscreen player rotation locking: replaced hardcoded single-direction `SCREEN_ORIENTATION_LANDSCAPE` with `PlayerOrientationController` across both Live TV (`PlayerScreen`) and Offline Player (`OfflinePlayerScreen`)
- Enabled YouTube-style 180° rotation in fullscreen: seamlessly flips between standard Landscape and Reverse Landscape based on the hardware accelerometer, even when system-wide auto-rotate is locked
- Added auto-rotate exit: physically turning the device upright automatically exits fullscreen back to portrait when system auto-rotate is enabled
- Added anti-snapback protection: tapping Exit Fullscreen while still holding the device horizontally temporarily holds portrait until physically rotated upright
- Preserved in-player control locks (`isLocked`) and Picture-in-Picture mode against unwanted rotation triggers
- Added unit tests in `PlayerOrientationTest` verifying orientation degree resolution, 180° flip handling, jitter hysteresis, and tablet support

## [1.3.1] - 2026-11-03

### Fixed
- Fixed media image placement between Home and Details: Home media rails now showcase vertical 2:3 TMDB posters (`NisPosterMediaCard`) while the Details screen features a full landscape backdrop banner (`MediaDetailsHeader`)
- Positioned the vertical poster card on the Details page in the left-side corner under/overlapping the main landscape backdrop image with shared bounds (`SharedRole.CONTAINER` and `SharedRole.IMAGE`)
- Positioned title (`SharedRole.TITLE`), media type, rating pill, and release year to the right of the vertical poster card
- Pre-cached both poster and backdrop image URLs in Coil memory cache before navigation for zero-flicker frame-1 rendering

## [1.3.0] - 2026-11-03

### Added
- Netflix-style shared element container transform transition for VOD media cards morphing into the Details header (`SharedTransitionLayout`, `LocalSharedTransitionScope`, `Modifier.nisSharedBounds`)
- 16:9 TMDB backdrop card morphing smoothly into portrait poster header (`NisBackdropMediaCard` -> `MediaDetailsHeader`) with scale-cropped bounds transform and zero aspect ratio distortion
- Seamless title text morphing between media cards and Details screen header without size snaps
- Frame-one synchronous header rendering via in-memory `MediaPreviewStore` (thread-safe LRU cache of 200 entries) and Coil 3 memory cache keys, eliminating white/blank placeholder flashes
- Predictive back gesture support via `android:enableOnBackInvokedCallback="true"` and interruptible spring physics (`NisBoundsTransform`)
- Subtle delayed enter animation (`nisContentEnterExit`) and skeleton loader (`MediaDetailsBodySkeleton`) for non-shared Details body elements
- Smooth bottom navigation bar exit/entry slide animation when navigating to and from the Details screen
- Shared element transition support across Home, Discover, Search, Watch Later/Favorites, and Details "Similar Titles" rail
- Unit test suite (`SharedKeysTest`, `MediaPreviewStoreTest`, `MediaDetailsRouteTest`) verifying cache eviction, key generation uniqueness, and backward-compatible route formatting

## [1.2.0] - 2026-11-02

### Added
- On-demand "Check for Update" action button in the Settings screen with real-time feedback
- Full release notes changelog viewer modal (`ChangelogDialog`) in the Settings screen
- Version release history and highlight tracking in `APP_CHANGELOG`

### Changed
- Settings screen layout now includes quick actions for app updates and release notes

## [1.1.0] - 2026-11-02

### Added
- Screencast and Wireless Display support (Chromecast, Miracast, Samsung Smart View, Fire TV) via `CastManager`
- `ScreencastDialog` for live stream and offline media casting to TV / DLNA apps (VLC, Web Video Caster, BubbleUPnP)
- In-player Screencast action button in both portrait and landscape controls for Live TV and Offline Downloads
- Dynamic in-app update checker (`AppUpdateChecker`) fetching release info from `https://nistv.xi11a.eu.org/version.json`
- `AppUpdateDialog` presenting changelogs, APK sizes, and one-tap download and installation
- Dynamic `version.json` endpoint configuration on website portal

### Changed
- Updated official website portal to reflect v1.1.0 release, showcasing Screencasting and dynamic updates
- Integrated `nistv.xi11a.eu.org` hosting domain configuration across the web and mobile ecosystem

### Fixed
- Offline player cast URI resolution fallback for downloaded media files

## [1.0.0] - 2026-11-01

### Added
- Initial versioning baseline

### Changed

### Fixed

### Removed

### Security
