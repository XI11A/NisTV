package com.nis.nistv.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nis.nistv.core.designsystem.icon.NisIcons
import com.nis.nistv.core.designsystem.theme.*

data class AppReleaseNote(
    val version: String,
    val date: String,
    val tag: String,
    val highlights: List<String>
)

val APP_CHANGELOG = listOf(
    AppReleaseNote(
        version = "v2.0.1",
        date = "November 2026",
        tag = "Latest",
        highlights = listOf(
            "Download Random Pause & Resume Loop Fix: Fixed O(N^2) byte-usage inflation in MultiThreadedDownloader so single downloads no longer trip the daily download limit and re-pause on resume",
            "Pause/Resume Synchronization: Serialized per-download pause, resume, and retry transitions in DownloadManager and cleared stale pause reasons on resume",
            "Paused Reason Visibility: Downloads screen now displays pause reason notices (such as daily limit or Wi-Fi wait) on paused items"
        )
    ),
    AppReleaseNote(
        version = "v2.0.0",
        date = "November 2026",
        tag = "Major",
        highlights = listOf(
            "Next-Gen Modern UI Overhaul: Floating glass island navigation dock with dynamic scroll-hiding, ambient lighting hero carousels, and segmented story progress indicators",
            "Modern Segmented Download Engine: Built on pure Kotlin Coroutines, OkHttp, and Java NIO FileChannel positional writes (pwrite), eliminating FUSE filesystem locks and 99% hangs",
            "Multi-Profile ('Who's Watching?'): Up to 5 user profiles with 12 local gradient avatars, Kids profile filter, Profile Lock PIN protection, and per-profile continue watching and taste recommendations",
            "Room Database Migration 10 -> 11: Seamless non-destructive profile data isolation for watch later, playback progress, taste profiles, and reminders"
        )
    ),
    AppReleaseNote(
        version = "v1.21.0",
        date = "November 2026",
        tag = "Minor",
        highlights = listOf(
            "Multi-Profile Architecture: Support for 5 independent profiles with isolated watch history, favorites, continue watching, and bookmarks",
            "Download Pause & Resume Fix: Resilient byte-accurate pause/resume with state persistence, preventing downloads from restarting from 0 bytes"
        )
    ),
    AppReleaseNote(
        version = "v1.20.0",
        date = "November 2026",
        tag = "Stable",
        highlights = listOf(
            "Modern Segmented Download Engine: High-performance parallel NIO chunk downloader with buffered I/O, eliminating filesystem lock contention and 99% completion hangs",
            "UI Freeze & Recomposition Fix: Throttled Room database writes and NotificationManager IPC calls to at most 1/sec, ensuring silky-smooth UI response during high-speed downloads",
            "Live Multi-Thread Status: Maintained per-thread chunk progress bars and active thread status in real-time on the Downloads screen"
        )
    ),
    AppReleaseNote(
        version = "v1.19.2",
        date = "November 2026",
        tag = "Patch",
        highlights = listOf(
            "Search Bar Typing Fix: Resolved async state race condition where rapid typing inverted letter sequences (e.g. 'Fire' becoming 'iref')",
            "Selection & Focus Preservation: Preserved cursor index and IME composition bounds during high-speed search queries"
        )
    ),
    AppReleaseNote(
        version = "v1.19.1",
        date = "November 2026",
        tag = "Patch",
        highlights = listOf(
            "Download OkHttpClient Isolation: Dedicated dispatcher and connection pool preventing download workers from blocking UI image loads and TMDB queries",
            "Fast-Ramp Progress Ticker: Fast 250ms initial progress ramp settling to 1000ms for immediate feedback upon connection",
            "Strict HTTP 206 Validation: Validated partial content responses across range workers to guard against server-side full content fallbacks"
        )
    ),
    AppReleaseNote(
        version = "v1.19.0",
        date = "November 2026",
        tag = "Minor",
        highlights = listOf(
            "Per-Thread Progress Visualization: Added real-time per-chunk status indicators and progress bars in Downloads screen",
            "Active Chunk Header & Badges: Real-time thread count indicator and individual thread completion badges",
            "Light Theme Details Polish: Adapted movie and episode details cards, synopsis text, and episode lists for light mode"
        )
    ),
    AppReleaseNote(
        version = "v1.18.0",
        date = "November 2026",
        tag = "Minor",
        highlights = listOf(
            "Parallel Chunk Downloader: High-speed multi-threaded media chunk downloading with Range probing and resume support",
            "Thread Concurrency Settings: User-configurable download concurrency (1, 2, 4, 6, 8 threads) in Profile Data & Storage",
            "Light Theme Video Players: Comprehensive light theme support across player portrait views, drawers, and modal sheets"
        )
    ),
    AppReleaseNote(
        version = "v1.17.1",
        date = "November 2026",
        tag = "Patch",
        highlights = listOf(
            "Global Screen Light Theme: Migrated all 15+ screens, cards, and modal dialogs to dynamic NisTheme color tokens",
            "Subtle Borders & Adaptive Surfaces: Clean background and elevated surfaces with adaptive contrast for daylight viewing"
        )
    ),
    AppReleaseNote(
        version = "v1.17.0",
        date = "November 2026",
        tag = "Minor",
        highlights = listOf(
            "Cinematic Light Theme: Added pristine high-contrast light theme palette (F6F7FB background, clean card surfaces, and accessible text/accent tokens)",
            "Dynamic Theme Setting: Select between System, Dark, and Light mode in Profile > Playback & Display with instant insets update and zero activity recreation",
            "Guaranteed Immersive Dark Surfaces: Media players, splash screen, hero scrims, and in-app trailer dialogs reliably stay dark in both light and dark modes"
        )
    ),
    AppReleaseNote(
        version = "v1.16.3",
        date = "November 2026",
        tag = "Patch",
        highlights = listOf(
            "Motion & Feedback Polish: Unified tactile spring press feedback across media cards and buttons with draw-phase scale evaluation",
            "Smooth Component Transitions: Animated bottom navigation indicator pill with alpha fade, pulsing live badge optimization, and smooth category chip color changes",
            "Animated Progress & Reduced Motion: Added NisLinearProgressBar with deferred draw-phase execution and system reduced-motion accessibility fallbacks across all screens"
        )
    ),
    AppReleaseNote(
        version = "v1.16.2",
        date = "November 2026",
        tag = "Patch",
        highlights = listOf(
            "Hero Carousel Visual Proportions: Expanded billboard height to 500dp to provide a spacious 140dp visible artwork window and prevent floating category chips from obscuring character faces",
            "Vibrant Artwork Scrim: Added 100% transparent gradient window (0.22f to 0.55f), eliminating muddy dark washes over backdrops while preserving contrast for top bars and text",
            "Contextual Slide Badges: Personalized watch history hook on slide 0 with dynamic contextual badges ('Critics' Choice', 'Top Movie Pick', etc.) on subsequent slides, plus centered overview text"
        )
    ),
    AppReleaseNote(
        version = "v1.16.1",
        date = "November 2026",
        tag = "Patch",
        highlights = listOf(
            "Hero Carousel Stability: Fixed unbounded cross-axis layout collapse inside LazyColumn by bounding height to 440dp, ensuring backdrop image and action buttons render with perfect proportions",
            "Smooth Gesture & Auto-Advance Paging: Auto-advance pauses during active touch gestures and cleanly resets the 5.5s timer after settling, avoiding jarring mid-swipe jumps",
            "Stable Recommendation Lifecycle: Carousel position is preserved during background recommendation refreshes with stable list keys, eliminating flickering and scroll resets"
        )
    ),
    AppReleaseNote(
        version = "v1.16.0",
        date = "November 2026",
        tag = "Minor",
        highlights = listOf(
            "Screen Polish & Tablet Responsiveness: Centered and capped content width to 840dp on tablets/foldables, upgraded Discover to an adaptive grid, and aligned vertical rhythms to 24dp",
            "Dynamic Type Resilience: Replaced fixed text heights with flexible minimum bounds across hero banners and media details for seamless 1.3x-2.0x font scaling without clipping",
            "Themed Monochrome Launcher Icon: Added Android 13+ monochrome silhouette layer for seamless Material You theming and guaranteed 48dp minimum touch targets across all screens"
        )
    ),
    AppReleaseNote(
        version = "v1.15.1",
        date = "November 2026",
        tag = "Patch",
        highlights = listOf(
            "Cinematic Obsidian 2.0 Screen Token Migration: Migrated all hardcoded colors, typography sizes, and corner shapes across all 15+ screens to design system tokens",
            "Universal Shape System: Standardized all UI surfaces, cards, and modal sheets to semantic shape tokens (NisMediaCardShape, NisDialogShape, NisPillShape)",
            "Automated Design Guard: Added DesignGuardTest enforcing zero raw hex colors, zero ad-hoc shapes, and zero deprecated tokens across the entire codebase"
        )
    ),
    AppReleaseNote(
        version = "v1.15.0",
        date = "November 2026",
        tag = "Minor",
        highlights = listOf(
            "Cinematic Obsidian 2.0 Component Restyle: Upgraded buttons, chips, badges, media cards, channel cards, and search bar with tactile spring press feedback and 48dp minimum touch targets",
            "Harmonic Surface & Dialog Styling: Unified alerts, modal sheets, and onboarding flows with 24dp dialog shapes, subtle borders, and consistent scrim backdrops",
            "Enhanced Accessibility & Motion Safety: Added reduced-motion support across shimmer loaders and pulsating badges, plus automatic RTL mirroring on directional icons"
        )
    ),
    AppReleaseNote(
        version = "v1.14.0",
        date = "November 2026",
        tag = "Minor",
        highlights = listOf(
            "Cinematic Obsidian 2.0 Tokens: Rebuilt theme foundations with unified surface hierarchy, high-contrast WCAG AA text tokens, and vibrant brand accents",
            "Static Inter Typography: Bundled official Inter static font family (400-700) with non-clipping multi-script safety for Tamil, Hindi, and Telugu",
            "Harmonic Shapes & Motion: Standardized 5-step corner radius scale, responsive 16/24dp screen paddings, and spring-driven tactile motion tokens"
        )
    ),
    AppReleaseNote(
        version = "v1.13.0",
        date = "November 2026",
        tag = "Minor",
        highlights = listOf(
            "In-App Trailer Playback: Official trailers now play seamlessly inside NisTV without redirecting to external browsers or apps",
            "Enhanced Hero Backdrop: Increased content details landscape image height by +41% (from 240dp to 340dp) for a cinematic, immersive view",
            "Seamless Mini-Player Maximize: Expanding the currently watching stream from the docked mini-player now immediately resumes playback without replaying the 5s branding intro"
        )
    ),
    AppReleaseNote(
        version = "v1.12.2",
        date = "November 2026",
        tag = "Patch",
        highlights = listOf(
            "Splash Audio Playback Fix: Modernized splash brand mnemonic playback with Media3 ExoPlayer and resilient MediaPlayer fallback, fixing invalid session ID errors",
            "Always-On Audio Reliability: Enabled splash audio playback across all ringer modes (normal, vibrate, silent) on the media stream to ensure the brand sound always plays",
            "Natural Reverb Tail: Transition from splash to home screen no longer abruptly cuts off splash audio, allowing the 1.9s sound to complete naturally"
        )
    ),
    AppReleaseNote(
        version = "v1.12.1",
        date = "November 2026",
        tag = "Patch",
        highlights = listOf(
            "Download Recovery Fix: Incomplete/paused downloads are no longer wrongly marked completed on app restart, and partial files are automatically healed back to paused status to allow resumption without corruption",
            "Full 5s Branding Video: Removed skip button overlay from the branding video for an uninterrupted cinematic entrance",
            "Synchronized Player Playback: Downloaded media and live streams are now strictly muted and paused during the branding video, beginning playback only after the 5-second intro completes",
            "Always-On Splash Audio: Removed the startup brand sound toggle from settings and made the splash sonic brand mnemonic always active"
        )
    ),
    AppReleaseNote(
        version = "v1.12.0",
        date = "November 2026",
        tag = "Feature",
        highlights = listOf(
            "Cinematic Branding Intro: 5-second NisTV pre-roll video bundled directly in offline and online players, choreographed to match the animated splash screen with zero network overhead",
            "Discrete Skip Intro Pill: Sleek translucent overlay with real-time countdown (5s -> 1s) and 1-tap/D-pad skip to instantly advance to content",
            "Seamless Offline Resumption: Automatically advances to downloaded media at saved playback resume position with zero timeline drift",
            "Zapping-Protected Live TV: Plays on initial channel launch while buffering live IPTV streams; automatically dismissed during channel zapping for instant channel switches"
        )
    ),
    AppReleaseNote(
        version = "v1.11.0",
        date = "November 2026",
        tag = "Live TV Upgrade",
        highlights = listOf(
            "Multi-View Dual Streaming: Stream two live TV channels simultaneously with stacked portrait or side-by-side landscape tiles, 1-tap audio switching, double-tap fullscreen, and low-RAM protection",
            "Stream Health Intelligence: Automatic stream reliability tracking with 30-day decay, self-healing stream ranking, dead-channel filtering, and color-coded status dots on channel cards",
            "Data Saver Suite: 1-tap Listen Mode (audio-only) in live player, auto-listen on cellular screen-off, cellular video quality caps (480p/720p), and real-time daily data usage tracking with threshold warnings",
            "Channel Zapper: Rapid horizontal swipe channel switching in live player with ZapOverlay HUD, 1-tap last channel toggle, and 1-4 digit numeric Channel Jump dialog skipping parental-locked channels"
        )
    ),
    AppReleaseNote(
        version = "v1.10.0",
        date = "November 2026",
        tag = "Feature",
        highlights = listOf(
            "Episode Radar: Automatic new-episode alerts for tracked TV shows with 12h background checks, grouped notifications, upcoming air dates, and per-series toggles",
            "Smart Downloads Autopilot: Automated background downloading for next 1-5 episodes with Wi-Fi only, charging only, 2 GB storage floor, and auto-delete watched retention policy",
            "Dual Subtitles & Delay: Precision subtitle offset controls (-10s to +10s in 50ms steps) and concurrent top & bottom dual subtitles with Indic font support",
            "Tonight Picks & Surprise Me: Contextual time-of-day Home recommendations favoring shorter episodes late at night and feature movies on weekends, with a 1-tap Surprise Me picker"
        )
    ),
    AppReleaseNote(
        version = "v1.9.0",
        date = "November 2026",
        tag = "Security",
        highlights = listOf(
            "Privacy & Security: Removed unused legacy permissions (READ_CONTACTS, GET_ACCOUNTS) and secured auth tokens using hardware-backed Android KeyStore AES-256 GCM encryption at rest",
            "Parental Controls: 4-6 digit salted PBKDF2 PIN, 5-attempt brute-force lockout, biometric/device lock recovery, locked categories, and automatic session re-locking on backgrounding",
            "Backup & Restore: Export/import user data (favorites, watch later, custom playlists, settings) to portable JSON with SHA-256 verification and merge/replace options",
            "History & Privacy: Dedicated screen with grouped watch history (Today/Yesterday/Earlier), pause history switch, taste profile reset, and 100% on-device guarantee",
            "Launcher Shortcuts: Quick launcher actions for Live TV, Search, Downloads, and dynamic 'Resume: {last channel}'",
            "Startup Sound Control: Profile setting to enable/disable startup sound with intelligent suppression in silent/vibrate mode, during active phone calls, or active music playback"
        )
    ),
    AppReleaseNote(
        version = "v1.8.0",
        date = "November 2026",
        tag = "Feature",
        highlights = listOf(
            "Custom Playlists (M3U/M3U8): Add by URL or local file, WorkManager 24h auto-refresh, token masking, and 'My Playlists' filter pill in Live TV",
            "TV Guide Grid: Sticky 30-minute time slots covering ~3h with synchronized horizontal scroll across all channels and real-time NOW indicator line",
            "Program Reminders: Schedule exact alarms for upcoming verified shows via AlarmManager with 5-minute pre-broadcast notifications and Watch Now deep links",
            "Stream Diagnostics: Monospace HUD overlay with resolution, codec, bitrate, dropped frames, buffer health, source index, and host latency",
            "Instant Track Selector: Bottom sheet for dynamic Audio and Subtitle track switching via Media3 TrackSelectionOverride",
            "Previous Channel Quick-Switch: One-tap button remembering the previous channel played during the session",
            "Database Schema v8: Added playlists and program_reminders tables with non-destructive migration"
        )
    ),
    AppReleaseNote(
        version = "v1.7.1",
        date = "November 2026",
        tag = "Bugfix",
        highlights = listOf(
            "Fixed missing enter fullscreen button in offline player: added prominent fullscreen toggle to bottom-right of video overlay next to time and OFFLINE badge",
            "Fixed top bar clipping on portrait screens with horizontal scroll container and pinned fullscreen button at the top-right",
            "Added Quick Action bar under portrait video canvas with one-tap access to Fullscreen, Subtitles, Speed, Timer, PiP, and External Player",
            "Added new NisIcons.OpenInNew icon for external player action to eliminate icon duplication"
        )
    ),
    AppReleaseNote(
        version = "v1.7.0",
        date = "November 2026",
        tag = "Feature",
        highlights = listOf(
            "Continue Watching on Home: prominent row for in-progress downloads with smart series collapsing and progress tracking",
            "Recent Channels: quick-access carousel for the last 10 visited channels",
            "Offline player gestures: double-tap left/right 10s seek and vertical swipe brightness/volume HUDs",
            "Playback speed sheet (0.5x - 2.0x) and persistent aspect ratio memory",
            "Sleep timer with volume fade-out and active countdown chip in controls",
            "Autoplay next episode countdown overlay with user preference control in Profile",
            "External subtitles support: auto-detection of sidecars, system file picker import, multi-charset decoder (UTF-8, UTF-16, Windows-1252), and subtitle track chooser",
            "Room schema v7 migration with subtitle tracks cascade deletion"
        )
    ),
    AppReleaseNote(
        version = "v1.6.2",
        date = "November 2026",
        tag = "Patch",
        highlights = listOf(
            "Prevented unwanted redirect to signup for existing AFDS accounts: users remain on Sign-In with an inline prompt instead of being forcibly redirected",
            "Added seamless OTP fallback for Google login: accounts created with Email OTP automatically receive a login code when selecting 'Continue with Google'",
            "Integrated Cloudflare reverse proxy failover and dual-candidate email dispatch across all login and profile endpoints",
            "Smooth Telegram-to-email OTP delivery fallback when Telegram bot is unlinked"
        )
    ),
    AppReleaseNote(
        version = "v1.6.1",
        date = "November 2026",
        tag = "Patch",
        highlights = listOf(
            "Fixed AFDS registration invite code bug: enabled mandatory serialization for 'inviteCode' in signup requests to resolve 'Invalid invite code.' error on default WELCOME code",
            "Normalized invite code inputs with automatic trim and uppercase formatting",
            "Enhanced JSON payload serializer with encodeDefaults = true to prevent omitted parameters"
        )
    ),
    AppReleaseNote(
        version = "v1.6.0",
        date = "November 2026",
        tag = "Feature",
        highlights = listOf(
            "Native in-app AFDS Sign-Up flow: users can now register directly inside NisTV with zero external browser redirects",
            "Automatic unregistered account detection: attempting to sign in with an unregistered email automatically transitions to the Sign-Up form with email prefilled",
            "Real-time email validation blocking '+' aliases and displaying normalized Gmail previews",
            "Two-step verification (User Details -> 6-digit OTP) backed by HashHackers AFDS API and Cloudflare proxy failover",
            "Seamless post-signup onboarding: automatically dispatches login code upon account creation so users can sign in immediately"
        )
    ),
    AppReleaseNote(
        version = "v1.5.2",
        date = "November 2026",
        tag = "Stable",
        highlights = listOf(
            "Download Started view in update dialog: users see clear instructions and download status instead of a blank screen",
            "Direct DownloadManager integration displaying live progress in the system notification shade",
            "Dedicated web download landing page with auto-download and installation guide",
            "Updated in-app changelog modal and website release notes with full version history"
        )
    ),
    AppReleaseNote(
        version = "v1.5.1",
        date = "November 2026",
        tag = "Stable",
        highlights = listOf(
            "Synchronized splash brand audio with high-fidelity spatial MP3 sound design",
            "Direct routing to media stream (STREAM_MUSIC) at full stereo volume for consistent playback",
            "Universal MP3 sonic asset decoded reliably across all Android hardware and emulators",
            "Instant playback trigger on splash entrance with lifecycle cleanup on screen exit",
            "Streamlined experience with unified brand audio identity"
        )
    ),
    AppReleaseNote(
        version = "v1.5.0",
        date = "November 2026",
        tag = "Stable",
        highlights = listOf(
            "Sonic brand identity: synchronized audio mnemonic for the animated NisTV logo entrance",
            "Increased Details page landscape backdrop height to 240dp (total header height 300dp)",
            "Balanced vertical poster placement and metadata alignment against the taller backdrop",
            "Enhanced cinematic visual hierarchy for movies and TV series details"
        )
    ),
    AppReleaseNote(
        version = "v1.4.0",
        date = "November 2026",
        tag = "Stable",
        highlights = listOf(
            "Choreographed deterministic splash timeline (1900 ms) with background settle, squircle trace, and play core pop",
            "Readiness-based boot exit orchestrator via AppContainer.bootReady, reducing warm-start splash to ~2.1 s",
            "Seamless system splash handoff via AndroidX core-splashscreen (Theme.NisTV.Starting) eliminating color jump",
            "Staggered typography entrance for wordmark and tagline with zero text relayout",
            "Pre-computed logo geometry and zero-allocation draw phase via drawWithCache",
            "Eliminated cluttering loops: removed rotating aurora, infinite shimmer, ribbon comet, and ping loops",
            "Full support for accessibility reduced-motion settings (animator_duration_scale == 0)",
            "Conditional loading feedback indicator if boot initialization exceeds expected build duration"
        )
    ),
    AppReleaseNote(
        version = "v1.3.2",
        date = "November 2026",
        tag = "Stable",
        highlights = listOf(
            "YouTube-style 180° player rotation in fullscreen across both Live TV and Offline playback",
            "Hardware accelerometer detection seamlessly flips between Landscape and Reverse Landscape even when system auto-rotate is off",
            "Auto-rotate exit: physically rotating the device upright exits fullscreen to portrait when auto-rotate is enabled",
            "Anti-snapback protection: tapping Exit Fullscreen while horizontal keeps portrait until upright",
            "In-player lock controls protection: orientation changes ignored while controls are locked"
        )
    ),
    AppReleaseNote(
        version = "v1.3.1",
        date = "November 2026",
        tag = "Stable",
        highlights = listOf(
            "Netflix-style shared element container transform transitions for VOD media cards",
            "Fluid shared bounds morphing from Home, Discover, Search, and Favorites into Details",
            "Instant frame-1 header rendering via in-memory MediaPreviewStore with zero blank flashes",
            "Refined image hierarchy: vertical posters on Home rails with cinematic landscape backdrop and corner poster on Details",
            "Predictive back gesture support with interruptible spring physics",
            "Animated bottom navigation bar slide in/out on Details transition"
        )
    ),
    AppReleaseNote(
        version = "v1.2.0",
        date = "November 2026",
        tag = "Previous",
        highlights = listOf(
            "On-demand in-settings 'Check for Update' button with real-time status feedback",
            "Full release notes changelog viewer modal in app settings",
            "Version release history and highlight tracking",
            "Enhanced wireless display pairing and network resilience"
        )
    ),
    AppReleaseNote(
        version = "v1.1.0",
        date = "November 2026",
        tag = "Stable",
        highlights = listOf(
            "Screencast & Wireless Display casting (Chromecast, Miracast, Samsung Smart View, Fire TV)",
            "Remote stream URL handoff to DLNA apps (VLC, Web Video Caster, BubbleUPnP)",
            "Dynamic in-app update checker from nistv.xi11a.eu.org",
            "In-settings manual update checker & full changelog viewer",
            "Offline player remote streaming and display projection support",
            "ExoPlayer stream failover cascade stability improvements"
        )
    ),
    AppReleaseNote(
        version = "v1.0.0",
        date = "November 2026",
        tag = "Baseline",
        highlights = listOf(
            "Initial production release of NisTV Revamped",
            "1,000+ Live IPTV channels with EPG guide",
            "TMDB Movies & TV Series VOD integration",
            "Resilient chunked multi-threaded offline download manager",
            "100% on-device recommendation vector affinity engine",
            "Picture-in-Picture (PiP) and background audio playback",
            "First-launch regional Indian and global language onboarding"
        )
    )
)

@Composable
fun ChangelogDialog(
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NisScrimStrong)
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(340.dp)
                .clip(NisDialogShape)
                .background(NisTheme.colors.surfaceElevated)
                .border(1.dp, NisTheme.colors.borderSubtle, NisDialogShape)
                .padding(20.dp)
                .clickable(enabled = false) {}
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(NisThumbnailShape)
                            .background(NisAccentCyan.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = NisIcons.Info,
                            contentDescription = null,
                            tint = NisAccentCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Release Notes",
                            color = NisTheme.colors.textPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "What's new in NisTV",
                            color = NisTheme.colors.textMuted,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(NisTheme.colors.surfaceHigh)
                        .nisPressable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = NisIcons.Close,
                        contentDescription = "Close",
                        tint = NisTheme.colors.textMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Changelog List Box
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .clip(NisMediaCardShape)
                    .background(NisTheme.colors.surfaceHigh)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(APP_CHANGELOG) { note ->
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = note.version,
                                color = NisTheme.colors.textPrimary,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            Box(
                                modifier = Modifier
                                    .clip(NisBadgeShape)
                                    .background(
                                        if (note.tag == "Latest") NisAccentCyan.copy(alpha = 0.16f) else NisTheme.colors.surfaceElevated
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = note.tag,
                                    color = if (note.tag == "Latest") NisAccentCyan else NisTheme.colors.textMuted,
                                    style = NisTheme.textExtras.micro,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Text(
                                text = note.date,
                                color = NisTheme.colors.textMuted,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            note.highlights.forEach { item ->
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 5.dp)
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(if (note.tag == "Latest") NisAccentCyan else NisTheme.colors.textMuted)
                                    )
                                    Text(
                                        text = item,
                                        color = NisTheme.colors.textPrimary,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Close Button
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = NisMediaCardShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NisAccentPrimary,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Close",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
