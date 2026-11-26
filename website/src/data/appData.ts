import { ReleaseInfo, FeatureItem, FaqItem, ChangelogItem, SpecRequirement } from '../types';

export const CURRENT_RELEASE: ReleaseInfo = {
  version: '2.0.1',
  versionCode: 20001,
  releaseDate: 'November 2026',
  fileSize: '15.2 MB',
  fileSizeBytes: 15974042,
  apkFileName: 'nistv-v2.0.1.apk',
  downloadUrl: './downloads/nistv-v2.0.1.apk',
  sha256: 'a300ac4c0b4443df7aa48dadb3006bce9e2567b5769207a8c802bbb5742acd89',
  md5: '5b110093eb25aa7fcecb4571c38ed083',
  minAndroid: 'Android 7.0 (Nougat, API 24)',
  targetAndroid: 'Android 14.0 (API 34)',
  architectures: ['arm64-v8a', 'armeabi-v7a', 'x86_64', 'x86'],
  packageName: 'com.nis.nistv',
};

export const HIGHLIGHT_STATS = [
  { value: '1,000+', label: 'Live IPTV Channels', detail: 'National, regional & sports' },
  { value: '5 Profiles', label: "Who's Watching?", detail: 'Kids Mode & Profile Lock' },
  { value: 'Multi-View', label: 'Dual Streaming', detail: 'Watch 2 channels at once' },
  { value: 'Screencast', label: 'Cast to TV / DLNA', detail: 'Chromecast, Miracast & VLC' },
  { value: 'Data Saver', label: 'Listen Mode', detail: 'Audio-only & cellular caps' },
  { value: '100%', label: 'On-Device Privacy', detail: 'Zero trackers, zero analytics' },
];

export const CORE_FEATURES: FeatureItem[] = [
  {
    id: 'multi-profiles',
    title: "Multi-Profile ('Who's Watching?') & Kids Mode",
    description: "Personalized entertainment for the whole family. Up to 5 individual profiles with custom avatars, dedicated watch history, continue watching progress, taste profiles, Kids mode, and PIN lock.",
    icon: 'Users',
    badge: 'NEW in v2.0.0',
    highlights: [
      "Up to 5 profiles with personalized local avatars",
      "Dedicated Kids profile restricting Live TV and VOD to family-friendly content",
      "Profile Lock with PBKDF2 PIN security to safeguard personal accounts",
      "Per-profile Watch Later, Continue Watching, and Taste recommendations"
    ]
  },
  {
    id: 'multi-view',
    title: 'Multi-View Dual Streaming & Zapper',
    description: 'Watch two live TV channels simultaneously with dynamic split-screen layouts, instant audio switching, horizontal swipe zapping, and 1-4 digit numeric channel jumping.',
    icon: 'MonitorPlay',
    badge: 'NEW in v1.12.0',
    highlights: [
      'Dual live streaming with portrait and landscape layouts',
      'One-tap audio toggle and low-RAM protection',
      'Channel Zapper with swipe navigation & ZapOverlay HUD',
      'Stream Health indicator dots with auto self-healing failover'
    ]
  },
  {
    id: 'screencast',
    title: 'Screencast & Wireless TV Playback',
    description: 'Beam your favorite channels, movies, and offline downloads directly to your big screen via Chromecast, Miracast, Samsung Smart View, or DLNA apps (VLC, Web Video Caster).',
    icon: 'Cast',
    badge: 'Full Cast Support',
    highlights: [
      'One-tap Google Cast & Miracast screen mirroring',
      'Direct stream URL handoff to DLNA/VLC players',
      'Dual-screen presentation display detection',
      'Wi-Fi network auto-pairing guidance'
    ]
  },
  {
    id: 'live-tv',
    title: 'Live IPTV with Stream Health Intelligence',
    description: 'Watch hundreds of regional and global channels. Stream Health intelligence tracks uptime and automatically cascades through backup server endpoints without buffering.',
    icon: 'Tv',
    badge: 'Self-Healing Streams',
    highlights: [
      'Multi-source fallback cascades with latency ranking',
      'Full Electronic Program Guide (EPG) grid & reminders',
      'Categorized by language & genre with M3U custom playlists',
      'Instant channel switching with swipe zapper'
    ]
  },
  {
    id: 'vod-hub',
    title: 'TMDB Movies & Series Catalog',
    description: 'Browse thousands of trending movies and TV series with rich cast metadata, trailer previews, season selectors, episode tracking, and contextual "Tonight" picks.',
    icon: 'Film',
    badge: 'TMDB Indexed',
    highlights: [
      'Detailed cast, crew & genre tags',
      'High-res poster & backdrop gallery',
      'Contextual "Tonight" picks and Surprise Me recommendations',
      'Discover tailored to your preferred languages'
    ]
  },
  {
    id: 'offline-downloads',
    title: 'Smart Downloads Autopilot & Dual Subs',
    description: 'Download full movies and episodes in parallel chunks. Smart Downloads Autopilot automatically fetches upcoming episodes, manages retention, and renders dual multilingual subtitles with delay controls.',
    icon: 'Download',
    badge: 'Autopilot + Dual Subs',
    highlights: [
      'Smart Autopilot background episode queueing',
      'Dual subtitle tracks (top/bottom) with +/-10s delay controls',
      'Chunked parallel thread downloads with auto-recovery',
      'Sleep timer with volume fade and autoplay next episode'
    ]
  },
  {
    id: 'privacy-recommendations',
    title: '100% On-Device Privacy & Security',
    description: 'Smart recommendations powered by on-device vector affinity. Features 4-6 digit Parental PIN protection, Android KeyStore AES-256 token encryption, and full JSON backup/restore.',
    icon: 'ShieldCheck',
    badge: 'Zero Cloud Logging',
    highlights: [
      'Parental PIN controls & category locks',
      'Hardware-backed AES-256 KeyStore encryption',
      'Zero telemetry or external tracking SDKs',
      'Portable JSON Backup & Restore'
    ]
  },
  {
    id: 'exoplayer-engine',
    title: 'Cinematic Branding & Pro Media Player',
    description: '5-second cinematic branding intro, YouTube-style 180° rotation, listen mode (audio-only), sleep timer, and gesture volume/brightness controls.',
    icon: 'PlayCircle',
    badge: 'ExoPlayer 1.5',
    highlights: [
      '5-second cinematic splash-geometry branding intro',
      'Listen Mode audio-only streaming saving up to 90% mobile data',
      'Auto-PiP on Android 12+ & YouTube-style 180° rotation',
      'Gesture volume, brightness, and double-tap 10s seek'
    ]
  }
];

export const LANGUAGES_SUPPORTED = [
  { code: 'hi', name: 'Hindi', native: 'हिन्दी', channels: '140+' },
  { code: 'ta', name: 'Tamil', native: 'தமிழ்', channels: '95+' },
  { code: 'te', name: 'Telugu', native: 'తెలుగు', channels: '110+' },
  { code: 'ml', name: 'Malayalam', native: 'മലയാളം', channels: '70+' },
  { code: 'kn', name: 'Kannada', native: 'ಕನ್ನಡ', channels: '65+' },
  { code: 'bn', name: 'Bengali', native: 'বাংলা', channels: '80+' },
  { code: 'mr', name: 'Marathi', native: 'मराठी', channels: '75+' },
  { code: 'pa', name: 'Punjabi', native: 'ਪੰਜਾਬੀ', channels: '50+' },
  { code: 'gu', name: 'Gujarati', native: 'ગુજરાતી', channels: '45+' },
  { code: 'en', name: 'English & Global', native: 'Global', channels: '300+' },
];

export const SYSTEM_SPECS: SpecRequirement[] = [
  {
    feature: 'Android Version',
    minimum: 'Android 7.0 (Nougat, API 24)',
    recommended: 'Android 10.0+ (API 29 to 14/15)',
    note: 'Supports Android 7 to Android 15'
  },
  {
    feature: 'RAM Memory',
    minimum: '2 GB RAM',
    recommended: '4 GB RAM or higher',
    note: 'Enables ultra-smooth 4K decoding'
  },
  {
    feature: 'Available Storage',
    minimum: '100 MB free space (app)',
    recommended: '5 GB+ for offline movie downloads',
    note: 'Configurable download storage location'
  },
  {
    feature: 'Screencast / TV Casting',
    minimum: 'Miracast / Wi-Fi Display or DLNA',
    recommended: 'Google Cast / Android TV / Fire TV',
    note: 'Both devices on same local Wi-Fi'
  },
  {
    feature: 'Processor (CPU)',
    minimum: 'ARMv7 32-bit or x86',
    recommended: 'ARM64 64-bit octa-core',
    note: 'Hardware accelerated H.264/H.265 (HEVC)'
  },
  {
    feature: 'Internet Connectivity',
    minimum: '3 Mbps for SD/HD streams',
    recommended: '15 Mbps+ for Full HD / 4K UHD',
    note: 'Wi-Fi or 4G/5G mobile data supported'
  }
];

export const CHANGELOG_DATA: ChangelogItem[] = [
  {
    version: 'v2.0.1',
    date: 'November 2026',
    tag: 'Latest',
    added: [],
    improved: [
      'Paused Reason Visibility: Downloads screen now displays pause reason notices (daily limit or Wi-Fi wait) on paused items'
    ],
    fixed: [
      'Download Random Pause & Resume Loop: Fixed O(N^2) byte-usage inflation in MultiThreadedDownloader so single downloads no longer trip the daily download limit and re-pause on resume',
      'Pause/Resume Synchronization: Serialized per-download pause, resume, and retry transitions in DownloadManager and cleared stale pause reasons on resume'
    ]
  },
  {
    version: 'v2.0.0',
    date: 'November 2026',
    tag: 'Stable',
    added: [
      'Next-Gen Modern UI Overhaul: Floating glass island navigation dock with dynamic scroll-hiding, glowing periwinkle pill indicators, and ambient lighting hero carousel',
      'Modern Segmented Download Engine: Powered by Kotlin Coroutines, OkHttp, and Java NIO FileChannel positional writes (pwrite), eliminating FUSE seek locks and 99% completion hangs',
      "Multi-Profile ('Who's Watching?') System: Up to 5 user profiles with 12 local gradient avatars, Kids profile filter, Profile Lock PIN protection, and per-profile continue watching and taste recommendations"
    ],
    improved: [
      'Room Database Migration 10 -> 11: Multi-profile isolation for watch later, playback progress, taste profiles, and reminders',
      'Tactile Spring Motion: Refined NisPressable micro-interactions across media cards, channel tiles, and navigation items'
    ],
    fixed: [
      '99% Download Hang: Guaranteed exact 100% completion in DownloadEntity.progressPercent and MultiThreadedDownloader disk sync',
      'Download Pause/Resume: Seamless byte-accurate pause/resume with .parts.json chunk state persistence across app restarts and URL renewals'
    ]
  },
  {
    version: 'v1.21.0',
    date: 'November 2026',
    tag: 'Stable',
    added: [
      "Multi-Profile System: Support for up to 5 individual user profiles ('Who's watching?') with custom avatars, dedicated watch history, favorites, and continue watching",
      'Kids Profile Mode: Restricts Live TV and VOD to family and kids-friendly content with parental PIN requirement to leave',
      'Profile Lock: Protect personal profiles with existing PBKDF2 PIN security to prevent unauthorized access'
    ],
    improved: [
      'Room Migration 10 -> 11: Multi-profile isolation for watch later, recommendations taste profile, reminders, and continue watching playback progress',
      'Instant Profile Switcher: Seamless in-app switching sheet without Activity recreation or audio interruption'
    ],
    fixed: [
      'Download Pause & Resume Breakpoint Reset: Fixed issue where pausing and resuming downloads restarted from byte 0 by eliminating eager URL renewals and migrating OkDownload breakpoints'
    ]
  },
  {
    version: 'v1.20.0',
    date: 'November 2026',
    tag: 'Stable',
    added: [
      'OkDownload High-Performance Multi-Stream Engine: Native buffered chunk downloading eliminating Android FUSE emulated storage lock contention and speed drops',
      'Real-Time Multi-Thread Chunk Monitor: Live visualization of individual chunk threads and completion status in Downloads'
    ],
    improved: [
      'Asynchronous Downloads Pipeline: Decoupled UI state flow from disk queries; storage statistics, file scans, and TV series grouping execute on background dispatchers',
      'Dedicated Network Connection Pool: OkDownload uses isolated HTTP dispatcher preventing parallel download blocks from starving app image loading or TMDB metadata'
    ],
    fixed: [
      'Critical Main Thread ANR / UI Hang: Throttled Room SQLite checkpoints and DataStore disk writes during active downloads, eliminating UI freezes and frame drops'
    ]
  },
  {
    version: 'v1.19.2',
    date: 'November 2026',
    tag: 'Stable',
    added: [],
    improved: [
      'Selection & Focus Preservation: Preserved cursor index and IME composition bounds during high-speed search queries'
    ],
    fixed: [
      'Search Bar Typing Fix: Resolved async state race condition where rapid typing inverted letter sequences (e.g. typing "Fire" producing "iref")'
    ]
  },
  {
    version: 'v1.19.1',
    date: 'November 2026',
    tag: 'Stable',
    added: [
      'Fast-Ramp Progress Ticker: 250ms initial progress ramp settling to 1000ms for immediate visual feedback upon connection'
    ],
    improved: [
      'Strict HTTP 206 Validation: Validated partial content responses across range workers to guard against server-side full content fallbacks'
    ],
    fixed: [
      'Download OkHttpClient Isolation: Dedicated dispatcher and connection pool preventing download workers from blocking UI image loads'
    ]
  },
  {
    version: 'v1.19.0',
    date: 'November 2026',
    tag: 'Stable',
    added: [
      'Per-Thread Progress Visualization: Added real-time per-chunk status indicators and progress bars in Downloads screen',
      'Active Chunk Header & Badges: Real-time thread count indicator and individual thread completion badges'
    ],
    improved: [
      'Light Theme Details Polish: Adapted movie and episode details cards, synopsis text, and episode lists for light mode'
    ],
    fixed: []
  },
  {
    version: 'v1.18.0',
    date: 'November 2026',
    tag: 'Stable',
    added: [
      'Parallel Chunk Downloader: High-speed multi-threaded media chunk downloading with Range probing and resume support',
      'Thread Concurrency Settings: User-configurable download concurrency (1, 2, 4, 6, 8 threads) in Profile Data & Storage'
    ],
    improved: [
      'Light Theme Video Players: Comprehensive light theme support across player portrait views, drawers, and modal sheets'
    ],
    fixed: []
  },
  {
    version: 'v1.17.1',
    date: 'November 2026',
    tag: 'Stable',
    added: [],
    improved: [
      'Global Screen Light Theme: Migrated all 15+ screens, cards, and modal dialogs to dynamic NisTheme color tokens',
      'Subtle Borders & Adaptive Surfaces: Clean background and elevated surfaces with adaptive contrast for daylight viewing'
    ],
    fixed: []
  },
  {
    version: 'v1.17.0',
    date: 'November 2026',
    tag: 'Stable',
    added: [
      'Cinematic Light Theme: Added pristine high-contrast light theme palette (F6F7FB background, clean card surfaces, and accessible text/accent tokens)',
      'Dynamic Theme Setting: Select between System, Dark, and Light mode in Profile > Playback & Display with instant insets update and zero activity recreation'
    ],
    improved: [
      'Guaranteed Immersive Dark Surfaces: Media players, splash screen, hero scrims, and in-app trailer dialogs reliably stay dark in both light and dark modes'
    ],
    fixed: []
  },
  {
    version: 'v1.16.3',
    date: 'November 2026',
    tag: 'Stable',
    added: [],
    improved: [
      'Motion & Feedback Polish: Unified tactile spring press feedback across media cards and buttons with draw-phase scale evaluation',
      'Smooth Component Transitions: Animated bottom navigation indicator pill with alpha fade, pulsing live badge optimization, and smooth category chip color changes',
      'Animated Progress & Reduced Motion: Added NisLinearProgressBar with deferred draw-phase execution and system reduced-motion accessibility fallbacks'
    ],
    fixed: []
  },
  {
    version: 'v1.16.2',
    date: 'November 2026',
    tag: 'Stable',
    added: [],
    improved: [
      'Hero Carousel Visual Proportions: Expanded billboard height to 500dp to provide a spacious visible artwork window',
      'Vibrant Artwork Scrim: Added 100% transparent gradient window eliminating muddy dark washes over backdrops',
      'Contextual Slide Badges: Personalized watch history hook on slide 0 with dynamic contextual badges on subsequent slides'
    ],
    fixed: []
  },
  {
    version: 'v1.16.1',
    date: 'November 2026',
    tag: 'Stable',
    added: [],
    improved: [
      'Smooth Gesture & Auto-Advance Paging: Auto-advance pauses during active touch gestures and cleanly resets the timer after settling',
      'Stable Recommendation Lifecycle: Carousel position is preserved during background recommendation refreshes with stable list keys'
    ],
    fixed: [
      'Hero Carousel Stability: Fixed unbounded cross-axis layout collapse inside LazyColumn by bounding height'
    ]
  },
  {
    version: 'v1.16.0',
    date: 'November 2026',
    tag: 'Stable',
    added: [
      'Themed Monochrome Launcher Icon: Added Android 13+ monochrome silhouette layer for seamless Material You theming'
    ],
    improved: [
      'Screen Polish & Tablet Responsiveness: Centered and capped content width to 840dp on tablets/foldables, upgraded Discover to an adaptive grid',
      'Dynamic Type Resilience: Replaced fixed text heights with flexible minimum bounds across hero banners and media details'
    ],
    fixed: []
  },
  {
    version: 'v1.15.1',
    date: 'November 2026',
    tag: 'Stable',
    added: [
      'Automated Design Guard: Added DesignGuardTest enforcing zero raw hex colors, zero ad-hoc shapes, and zero deprecated tokens'
    ],
    improved: [
      'Cinematic Obsidian 2.0 Screen Token Migration: Migrated all hardcoded colors, typography sizes, and corner shapes across all 15+ screens to design system tokens',
      'Universal Shape System: Standardized all UI surfaces, cards, and modal sheets to semantic shape tokens'
    ],
    fixed: []
  },
  {
    version: 'v1.15.0',
    date: 'November 2026',
    tag: 'Stable',
    added: [
      'Cinematic Obsidian Design System 2.0: Deep obsidian surfaces, radiant crimson accents, and refined typography scales'
    ],
    improved: [],
    fixed: []
  },
  {
    version: 'v1.14.0',
    date: 'November 2026',
    tag: 'Stable',
    added: [
      'Offline Media Auto-Repair & Scanner: Automated background repair and reconciliation for interrupted or incomplete media downloads'
    ],
    improved: [],
    fixed: []
  },
  {
    version: 'v1.13.0',
    date: 'November 2026',
    tag: 'Stable',
    added: [
      'In-App Official Trailer Player: watch official TMDB movie and series trailers directly inside NisTV with zero external app or browser redirects',
      'Cinema-grade HTML5 trailer modal with dark obsidian scrim, official trailer badge, movie title, subtitle, and explicit close button'
    ],
    improved: [
      'Content Details Hero Backdrop: increased landscape backdrop height to 340dp (total header 400dp) for a rich, expansive cinematic presentation'
    ],
    fixed: [
      'Mini-Player Maximize Intro Bypass: expanding currently watching content from the docked mini-player now immediately resumes live playback without replaying the 5-second branding video'
    ]
  },
  {
    version: 'v1.12.2',
    date: 'November 2026',
    tag: 'Stable',
    added: [
      'Resilient Media3 ExoPlayer splash brand mnemonic audio pipeline with graceful Android MediaPlayer fallback'
    ],
    improved: [
      'Always-on splash audio reliability: brand sound plays cleanly across all device ringer modes (normal, vibrate, silent) on the media stream',
      'Smooth transition physics: transitioning from splash to home screen no longer terminates the audio track prematurely, letting the 1.9s sound finish naturally'
    ],
    fixed: [
      'Resolved invalid audio session ID exception in splash sound initialization on modern Android releases',
      'Fixed audio cut-off caused by early composable disposal during fast startup and reduced motion'
    ]
  },
  {
    version: 'v1.12.1',
    date: 'November 2026',
    tag: 'Stable',
    added: [
      'Automatic download self-healing: corrupted or truncated download entries previously marked as completed are automatically restored to paused status with true byte counts to permit resume'
    ],
    improved: [
      'Synchronized media playback: downloaded content and live IPTV streams now strictly wait and buffer silently in the background while the branding video plays, starting playback only after the 5-second intro finishes',
      'Uninterrupted branding video: removed the skip button and countdown timer from the branding intro for a pristine cinematic start',
      'Always-on splash audio: sonic brand identity mnemonic audio is now always enabled across app launches'
    ],
    fixed: [
      'Fixed critical issue where paused or in-progress downloads were mistakenly marked as completed after fully closing and reopening the app, causing playback of corrupted partial files'
    ]
  },
  {
    version: 'v1.12.0',
    date: 'November 2026',
    tag: 'Stable',
    added: [
      '5-Second Cinematic Branding Video: bespoke 1080p intro based on the splash geometry (luminous squircle border trace, rising stream pillars, cyber neon diagonal ribbon wipe, luminous play core, and ruby red broadcast beacon) played on video player startup',
      'Offline Player Branding Integration: pre-roll branding intro smoothly plays upon opening offline video with countdown and discrete "Skip Intro" button, automatically resuming target media at saved position',
      'Live TV Player Branding Integration: intro plays on initial channel launch, masking stream manifest loading and buffering with zero UI stutter',
      'Instant Zap Dismissal: channel zapping gestures, channel jumps, and navigation drawer immediately dismiss the intro'
    ],
    improved: [
      'Synchronized sonic brand identity mnemonic audio playback during video player startup',
      'Seamless transition physics into target media without screen flicker or audio clipping'
    ],
    fixed: [
      'Eliminated video player start jitter during initial stream network handshake'
    ]
  },
  {
    version: 'v1.11.0',
    date: 'November 2026',
    tag: 'Stable',
    added: [
      'Multi-View Dual Streaming: watch two live TV channels simultaneously with responsive split-screen layouts (stacked in portrait, side-by-side in landscape)',
      'One-tap audio focus toggle between streams with visual amber border highlight; unselected stream is automatically muted',
      'Stream Health Intelligence: real-time stream telemetry, 30-day exponential decay scoring, and automatic self-healing stream failover ranking',
      'Reliability indicator dots on channel cards (Emerald = Reliable, Amber = Degraded, Red = Dead)',
      'Data Saver Suite: Listen Mode for audio-only live TV streaming, halting video decoding and saving up to 90% mobile data',
      'Auto-Listen on Screen Off: automatically switches to audio-only playback when screen turns off on cellular networks',
      'Cellular streaming quality caps (Off, 480p, 720p) and daily/monthly cellular bandwidth budget alerts',
      'Channel Zapper: rapid horizontal swipe switching (swipe left for next, swipe right for previous channel)',
      'Dynamic ZapOverlay HUD displaying channel position, logo, and title with 2-second auto-dismiss',
      'Numeric Channel Jump dialog supporting direct 1-4 digit channel hopping'
    ],
    improved: [
      'Multi-stream resource optimization: primary stream capped at 720p and secondary capped at 480p with low-RAM device safety guards',
      'Parental controls enforcement: locked categories and NSFW channels are automatically skipped during zapping and jump requests'
    ],
    fixed: [
      'Prevented audio focus conflict and background decoder leaks during multi-window transitions'
    ]
  },
  {
    version: 'v1.10.0',
    date: 'November 2026',
    tag: 'Stable',
    added: [
      'Episode Radar: automated background tracking for downloaded series and Watch Later shows with upcoming air date badges',
      'Smart Downloads Autopilot: automatic background next-episode downloading with Wi-Fi/charging safety constraints and watched episode cleanup',
      'Dual Subtitles & Subtitle Delay: secondary top subtitle track support alongside primary bottom subtitles with -10s to +10s timing delay controls',
      'Tonight Picks & Surprise Me: dynamic contextual time-of-day recommendation engine with weighted random selection'
    ],
    improved: [
      'Subtitles parser now supports SRT, WebVTT, and ASS/SSA with complex Indic script HarfBuzz shaping',
      'Offline-aware candidate filtering restricting recommendations to downloaded media when offline'
    ],
    fixed: [
      'Resolved subtitle timing desynchronization across variable framerate video containers'
    ]
  },
  {
    version: 'v1.9.0',
    date: 'November 2026',
    tag: 'Stable',
    added: [
      'Parental Controls & PIN Protection: PBKDF2 hashed PIN, 5-attempt brute-force lockout, and category locking',
      'Full Backup & Restore: export and import user favorites, watch later items, and preferences via portable JSON',
      'Dedicated History & Privacy controls with single-item deletion and pause watch history toggle',
      'Hardware-backed Android KeyStore AES-256 GCM token encryption (SecureTokenStore)',
      'Android Launcher App Shortcuts for Live TV, Search, Downloads, and Last Channel Resume'
    ],
    improved: [
      'Hardened security posture: removed legacy unused Android permissions',
      '100% on-device data locality guarantee with transparent privacy controls'
    ],
    fixed: [
      'Fixed credential leakage risk by encrypting authentication tokens at rest'
    ]
  },
  {
    version: 'v1.8.0',
    date: 'November 2026',
    tag: 'Stable',
    added: [
      'Custom Playlists: import external M3U/M3U8 playlists via URL or file picker with 24h background auto-sync',
      'TV Guide Grid (EPG): virtualized 3-hour broadcast grid with real-time "NOW" indicator and program reminders',
      'Stream Diagnostics HUD: real-time FPS, bitrate, resolution, dropped frames, and network latency overlay',
      'Quick Track Selector bottom sheet for instantaneous audio and subtitle switching'
    ],
    improved: [
      'High-performance streaming M3U parser supporting large channel playlists up to 100,000 entries',
      'Strict sync isolation ensuring catalogue updates never prune custom playlist channels'
    ],
    fixed: [
      'Resolved channel collision issues by hashing custom playlist stream identifiers'
    ]
  },
  {
    version: 'v1.7.0',
    date: 'November 2026',
    tag: 'Stable',
    added: [
      'Continue Watching row on Home screen for in-progress downloads with episode collapsing',
      'Offline Player upgrades: double-tap 10s seek ripples, swipe gestures for brightness/volume, sleep timer, and autoplay next episode',
      'External Subtitle File import (.srt, .vtt, .ass) with multi-charset auto-detection'
    ],
    improved: [
      'Media preview caching for zero-flicker transitions between Home and Details screens',
      'Battery and CPU efficiency during high-bitrate offline video decoding'
    ],
    fixed: [
      'Fixed missing enter-fullscreen button in portrait video player mode'
    ]
  },
  {
    version: 'v1.6.2',
    date: 'November 2026',
    tag: 'Stable',
    added: [
      'Seamless OTP fallback for "Continue with Google": accounts created via Email OTP automatically receive a login OTP to that email instead of redirecting to sign-up',
      'Cloudflare reverse proxy failover integration across all login, OTP verification, and profile endpoints'
    ],
    improved: [
      'Discrete error handling: users remain on Sign-In tab upon login failures with inline guidance rather than abrupt forced redirection',
      'Robust dual-candidate email dispatch (raw email and normalized Gmail) for OTP requests',
      'Automatic fallback from Telegram OTP to email OTP when Telegram bot delivery fails'
    ],
    fixed: [
      'Prevented unwanted automatic redirect to signup for existing AFDS accounts'
    ]
  },
  {
    version: 'v1.6.1',
    date: 'November 2026',
    tag: 'Stable',
    added: [
      'Payload serialization regression test suite for AFDS registration'
    ],
    improved: [
      'Normalized invite code input with automatic trimming and uppercase conversion',
      'Configured JSON serializer with encodeDefaults = true to safeguard against parameter omission'
    ],
    fixed: [
      'Resolved AFDS registration "Invalid invite code." failure: enabled mandatory serialization for inviteCode to prevent the default WELCOME invite code from being omitted in signup API payloads'
    ]
  },
  {
    version: 'v1.6.0',
    date: 'November 2026',
    tag: 'Stable',
    added: [
      'Native in-app AFDS Sign-Up flow: users can now register directly inside NisTV with zero external browser redirects',
      'Automatic unregistered account detection that seamlessly routes unregistered sign-in attempts to the Sign-Up view',
      'Real-time email validation blocking \'+\' aliases and displaying normalized Gmail previews',
      'Two-step verification (User Details -> 6-digit OTP) backed by HashHackers AFDS API and Cloudflare proxy failover',
      'Seamless post-signup onboarding: automatically dispatches login code upon account creation'
    ],
    improved: [
      'Frictionless AFDS onboarding: eliminates manual browser signup steps',
      'Resilient network layer with automatic failover between direct backend and reverse proxy'
    ],
    fixed: [
      'Resolved dead-end login failures for new or unregistered email addresses'
    ]
  },
  {
    version: 'v1.5.2',
    date: 'November 2026',
    tag: 'Stable',
    added: [
      'Download Started view in update dialog: users see clear instructions and download status instead of a blank screen',
      'Direct DownloadManager integration displaying live progress in the system notification shade',
      'Dedicated web download landing page (/download.html) with auto-download and installation guide',
      'Comprehensive in-app and website changelog history detailing all recent version updates'
    ],
    improved: [
      'Seamless in-app update experience preventing blank web browser tabs',
      'Automatic hash and query detection on website (/download, #download, ?download=started)'
    ],
    fixed: [
      'Eliminated blank screen when checking and starting app updates'
    ]
  },
  {
    version: 'v1.5.1',
    date: 'November 2026',
    tag: 'Stable',
    added: [
      'Synchronized splash brand audio with high-fidelity spatial MP3 sound design',
      'Direct routing to media stream (STREAM_MUSIC) at full stereo volume for consistent playback',
      'Download Started view in update dialog: users see clear instructions and download status instead of a blank screen',
      'Web download landing page auto-opening download modal with installation instructions'
    ],
    improved: [
      'Universal MP3 sonic asset decoded reliably across all Android hardware decoders',
      'App update flow uses system DownloadManager with real-time notification shade progress'
    ],
    fixed: [
      'Fixed splash screen audio not playing due to system sonification touch sound stream routing and reduced-motion suppression',
      'Fixed blank page on checking and starting update download'
    ]
  },
  {
    version: 'v1.5.0',
    date: 'November 2026',
    tag: 'Stable',
    added: [
      'Sonic brand identity: synchronized audio mnemonic for the animated NisTV logo entrance',
      'Increased Details page landscape backdrop height to 240dp (total header height 300dp)',
      'Balanced vertical poster placement and metadata alignment against the taller backdrop'
    ],
    improved: [
      'Enhanced cinematic visual hierarchy for movies and TV series details headers'
    ],
    fixed: [
      'Adjusted poster-to-backdrop overlap ratio (105dp overlap, 60dp bottom extension)'
    ]
  },
  {
    version: 'v1.4.0',
    date: 'November 2026',
    tag: 'Stable',
    added: [
      'Choreographed deterministic splash timeline (1900 ms) with background settle, squircle trace, and play core pop',
      'Readiness-based boot exit orchestrator via AppContainer.bootReady, reducing warm-start splash to ~2.1 s',
      'Seamless system splash handoff via AndroidX core-splashscreen (Theme.NisTV.Starting) eliminating color jump',
      'Staggered typography entrance for wordmark and tagline with zero text relayout',
      'Pre-computed logo geometry and zero-allocation draw phase via drawWithCache',
      'Full support for accessibility reduced-motion settings (animator_duration_scale == 0)',
      'Conditional loading feedback indicator if boot initialization exceeds expected build duration'
    ],
    improved: [
      'Zero heap allocations and zero recompositions in NisLogo draw loop',
      'Removed cluttering loops: eliminated rotating aurora, infinite shimmer, ribbon comet, and ping loops'
    ],
    fixed: [
      'Fixed duplicate database seeding on launch by centralizing initialization'
    ]
  },
  {
    version: 'v1.3.2',
    date: 'November 2026',
    tag: 'Stable',
    added: [
      'YouTube-style 180° player rotation in fullscreen across both Live TV and Offline playback',
      'Continuous hardware accelerometer tracking dynamically flipping between Landscape and Reverse Landscape',
      'Auto-rotate exit: physically turning the device upright automatically exits fullscreen to portrait when system auto-rotate is enabled',
      'Anti-snapback protection: tapping Exit Fullscreen while horizontal keeps portrait until physically upright',
      'In-player control lock protection: orientation changes safely ignored while controls are locked'
    ],
    improved: [
      'Player orientation controller architecture (PlayerOrientationController) with sensor hysteresis to eliminate angle jitter',
      'Full cross-device compatibility across phones and natural-landscape tablets',
      'Unit test coverage with PlayerOrientationTest verifying orientation degree resolution and 180° flip handling'
    ],
    fixed: [
      'Fixed player not rotating 180° when flipping the device upside down in fullscreen mode'
    ]
  },
  {
    version: 'v1.3.1',
    date: 'November 2026',
    tag: 'Stable',
    added: [
      'Netflix-style shared element container transform transitions for VOD media cards morphing into the Details header',
      'Fluid shared bounds morphing from Home, Discover, Search, and Favorites directly into the Details view',
      'Synchronous frame-one header display from in-memory MediaPreviewStore with pre-cached image memory keys',
      'Refined image hierarchy: vertical 2:3 posters on Home rails with cinematic landscape backdrop and corner poster on Details',
      'Predictive back gesture support via android:enableOnBackInvokedCallback="true" with interruptible spring physics',
      'Smooth bottom navigation bar exit/entry slide animation when navigating to and from the Details screen'
    ],
    improved: [
      'Pre-warming of backdrop and poster image cache keys in Coil for zero-flicker transitions',
      'Adaptive Layout: Details header positions vertical poster card under the landscape backdrop with aligned title and metadata',
      'Enhanced test coverage: 17 unit test suites verifying route building, LRU cache operations, and shared keys uniqueness'
    ],
    fixed: [
      'Fixed media image placement between Home and Details: Home media rails now showcase vertical 2:3 TMDB posters, while Details features a full landscape backdrop banner with the vertical poster placed in the left-side corner',
      'Fixed title and rating duplication between Details header and body sections'
    ]
  },
  {
    version: 'v1.2.0',
    date: 'November 2026',
    tag: 'Stable',
    added: [
      'Added in-settings on-demand "Check for Update" action with real-time status and animated loading feedback',
      'Added comprehensive "Changelog" viewer modal in app settings displaying complete version history and release notes',
      'Integrated auto-updating prompt with instant direct download and installation mechanism',
      'Enhanced remote display resolution and screencast pairing workflows'
    ],
    improved: [
      'Settings & Profile screen UX with clean update indicator badge',
      'Network failure resilience when querying release metadata from nistv.xi11a.eu.org',
      'ExoPlayer stream failover cascade recovery times'
    ],
    fixed: [
      'Resolved display connection status edge-cases when disconnecting wireless displays',
      'Fixed offline player stream resolution on external DLNA playback handoff'
    ]
  },
  {
    version: 'v1.1.0',
    date: 'November 2026',
    tag: 'Stable',
    added: [
      'Added Screencast & Wireless Display casting support (Chromecast, Miracast, Samsung Smart View, Fire TV)',
      'Added in-player Screencast action button in both portrait and landscape controls for Live TV and Offline Downloads',
      'Added remote stream casting to external players and smart TVs (Web Video Caster, BubbleUPnP, VLC)',
      'Dynamic in-app update checker fetching release info from nistv.xi11a.eu.org/version.json',
      'AppUpdateDialog presenting changelogs, APK sizes, and one-tap download and installation'
    ],
    improved: [
      'ExoPlayer stream failover cascade stability and recovery speed',
      'Optimized memory usage during high-bitrate Full HD streams',
      'External display presentation detection and state handling'
    ],
    fixed: [
      'Fixed offline player media URI resolution for local downloaded files',
      'Enhanced wireless stream handoff stability'
    ]
  },
  {
    version: 'v1.0.0',
    date: 'November 2026',
    tag: 'Stable',
    added: [
      'Initial production release of NisTV Revamped architecture',
      'Unified Live IPTV streaming engine with over 1,000+ channels and EPG support',
      'TMDB VOD metadata integration with poster galleries, cast lists, and trailers',
      'Resilient multi-threaded chunked download manager with auto-recovery',
      'Privacy-preserving on-device recommendation engine using vector decay',
      'Language onboarding dialog supporting 10+ regional Indian and global languages',
      'Picture-in-Picture (PiP) and MediaSession background audio playback'
    ],
    improved: [
      'Stream failure fallback cascades for near-instant failover',
      'Chunked download network speed and battery consumption efficiency',
      'Dark obsidian UI contrast and responsive touch targets (48x48px)'
    ],
    fixed: [
      'Resolved stream stall issues on unstable cellular data connections'
    ]
  }
];

export const FAQ_DATA: FaqItem[] = [
  {
    category: 'playback',
    question: 'How do I cast NisTV to my TV, Chromecast, or Fire Stick?',
    answer: 'Tap the Cast icon on the player screen (available in both portrait and landscape modes). You can choose "System Screen Cast" to mirror to Chromecast, Google TV, Samsung Smart View, or Miracast displays. Or select "Cast Stream to TV / App" to send the direct video feed to DLNA apps like Web Video Caster, BubbleUPnP, or VLC!'
  },
  {
    category: 'download',
    question: 'How does the in-app update checker work?',
    answer: 'Whenever you open NisTV Revamped, it automatically checks https://nistv.xi11a.eu.org/version.json for new releases. If an update is detected, a dialog pops up showing the new features and lets you download and install the update in one tap. You can also manually check for updates and review the full release changelog at any time in App Settings.'
  },
  {
    category: 'download',
    question: 'Is NisTV Revamped completely free and safe to download?',
    answer: 'Yes! NisTV Revamped is 100% free and open-source under the MIT license. It contains zero advertisements, zero tracking telemetry, and requires no premium subscriptions or account creation. The APK is signed and can be verified against the SHA-256 checksum published above.'
  },
  {
    category: 'installation',
    question: 'Why does Android show "File might be harmful" or "Unknown app"?',
    answer: 'This is standard Android security warning shown for any APK downloaded outside the Google Play Store (sideloading). Since NisTV is open-source and distributed directly without proprietary store lock-in, Android displays this notice. You can safely tap "Download anyway" and enable "Allow from this source" in Settings to install.'
  },
  {
    category: 'installation',
    question: 'How do I install NisTV on Android TV or Fire TV Stick?',
    answer: 'On your Android TV or Fire Stick: 1) Install the free "Downloader" app by AFTVnews from the TV app store. 2) Enter https://nistv.xi11a.eu.org/downloads/nistv-v1.20.0.apk (or https://nistv.xi11a.eu.org/downloads/nistv-latest.apk). 3) Download the APK and press Install when prompted. The interface automatically adapts for TV remote D-Pad navigation!'
  },
  {
    category: 'playback',
    question: 'How does the Auto-Failover stream cascading work?',
    answer: 'When you tap a Live TV channel, NisTV connects to the primary high-definition stream. If a server experiences latency or returns an HTTP error (404/503), NisTV immediately cascades to alternate backup mirror streams within milliseconds, preventing buffering loops.'
  },
  {
    category: 'download',
    question: 'Can I download movies and episodes to watch completely offline?',
    answer: 'Yes! Tap the download icon on any movie or series episode. The resilient download manager downloads in parallel chunks. If your internet connection drops or switches between Wi-Fi and 4G/5G, downloading automatically resumes from the exact byte where it paused.'
  },
  {
    category: 'privacy',
    question: 'Does NisTV collect my watch history or personal data?',
    answer: 'No. NisTV Revamped has a strict zero-telemetry policy. Your watch preferences, history, bookmarks, and recommendations run entirely on your local device CPU/storage using an embedded SQLite database. No logs are ever uploaded to remote servers.'
  }
];
