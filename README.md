# NisTV

A modern, native Android IPTV and Video-on-Demand (VOD) streaming application built from the ground up with Kotlin and Jetpack Compose.

---

## Overview

**NisTV** provides a fluid, unified streaming experience tailored for both mobile devices and Android TV. It combines open live IPTV broadcast streams with rich media metadata, offline chunked downloading, on-device recommendations, and custom profile isolation.

### Key Features

- **Live IPTV Streaming**: Seamless broadcast playback powered by **Android Media3 (ExoPlayer)** with automatic stream failover and multi-source redundancy.
- **VOD & Metadata Exploration**: Comprehensive Movie and Series catalogue powered by The Movie Database (TMDB) API, with genre filtering, trending lists, and cast details.
- **Electronic Program Guide (EPG)**: Virtualized TV Guide grid displaying live timelines, channel schedules, and reminder alarms.
- **Resilient Multi-Threaded Downloader**: Chunked parallel downloading engine utilizing OkHttp and NIO FileChannel with pause/resume capabilities and automatic link regeneration.
- **Cinematic Obsidian UI**: Dark OLED-optimized design system, custom motion physics, WCAG AA contrast compliance, and full keyboard/D-pad navigational accessibility.
- **Multi-Profile Management**: Dedicated watch histories, continued-watching states, bookmarks, and preferences per profile.
- **Advanced Player Controls**: Swipe gestures for brightness and volume, double-tap to seek, external subtitle parser (UTF-8, UTF-16, Windows-1252), and sleep timer automation.
- **Picture-in-Picture & Background Media**: Native Android O+ PiP and background audio playback backed by Android MediaSession.
- **Integrated Web Portal**: Responsive landing page and distribution hub located in `website/` built with React 18, Vite, and Tailwind CSS.

---

## Tech Stack

| Layer | Technologies |
| :--- | :--- |
| **Language & Platform** | Kotlin 2.0, Android SDK (Min SDK 24, Target SDK 34, Compile SDK 35) |
| **UI Framework** | Jetpack Compose, Material 3, Compose Navigation |
| **Media Engine** | AndroidX Media3 (ExoPlayer, HLS, DASH, RTSP, SmoothStreaming, Session) |
| **Persistence** | SQLite with Room Database (v1–v8 migrations) |
| **Preferences** | Jetpack DataStore Preferences, Android KeyStore AES-256 GCM encryption |
| **Networking & Async** | OkHttp 4.12, Kotlinx Coroutines, Kotlinx Serialization, Coil 3 |
| **Background Tasks** | AndroidX WorkManager |
| **Web Portal** | React 18, TypeScript, Vite, Tailwind CSS, Lucide Icons |

---

## Getting Started

### Prerequisites

- **Java Development Kit**: JDK 17 or higher
- **Android SDK**: Build tools and platform SDK 35 installed
- **Node.js** (Optional, for web portal): Node.js 18+ and npm

### Configuration

The app uses The Movie Database (TMDB) for movie and series metadata.

1. Obtain a free API key from [The Movie Database (TMDB)](https://www.themoviedb.org/settings/api).
2. Configure the key via any of the following methods:
   - Add it to `local.properties`:
     ```properties
     TMDB_API_KEY=your_actual_tmdb_api_key_here
     ```
   - Or export it as an environment variable:
     ```bash
     export TMDB_API_KEY=your_actual_tmdb_api_key_here
     ```
   - Or configure it directly within the in-app Settings screen at runtime.

### Building the Android App

Clone the repository and run the Gradle tasks:

```bash
# Assemble the debug APK
./gradlew assembleDebug

# Run all unit test suites
./gradlew testDebugUnitTest
```

The compiled APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### Running the Web Portal

```bash
cd website
npm install
npm run dev
```

---

## Project Structure

```text
├── app/
│   ├── src/main/java/com/nis/nistv/
│   │   ├── core/            # Design system, theme, security, auth, preferences
│   │   ├── data/            # Room database, repositories, download manager, network
│   │   ├── feature/         # Compose UI screens (Home, Live TV, Player, Search, etc.)
│   │   └── NisTvApp.kt      # Application entry point & dependency wiring
│   └── src/test/            # Comprehensive unit and architectural guard test suites
├── assets/                  # App vector icons, branding graphics, and splash assets
├── docs/                    # Design system specifications and architecture guides
├── website/                 # React + Vite web portal and landing page
├── version.properties       # Semantic versioning configuration
└── AGENTS.md                # Engineering standards and agent workflow rules
```

---

## Contributing

Contributions are welcome! If you would like to contribute:

1. Fork the repository.
2. Create a feature branch (`git checkout -b feature/my-new-feature`).
3. Commit your changes with clear, descriptive commit messages.
4. Ensure all unit tests pass (`./gradlew testDebugUnitTest`).
5. Open a Pull Request detailing your changes.

---

## Disclaimer

NisTV is an open-source software project designed for educational and personal media management purposes. NisTV does not host, broadcast, or distribute any media content or copyrighted streams. Live television streams are sourced from publicly accessible indexes (such as iptv-org) or user-supplied M3U playlists. All trademarks and media metadata remain the property of their respective owners.
