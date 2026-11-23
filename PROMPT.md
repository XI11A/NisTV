NisTV — Complete Native Android IPTV Application

1. ROLE AND MISSION

Act as a senior Android engineer, Kotlin architect, UI/UX designer, and streaming-media specialist.

Your task is to build NisTV, a polished, production-minded, native Android IPTV application with a premium streaming experience.

This is a real implementation task, not a prototype, mockup, or documentation-only exercise. Inspect the development environment, implement the application, build it, diagnose errors, and progressively deliver a working APK.

Do not stop after creating the project structure or writing a plan. Continue through the milestones below, building and verifying the application after each milestone.

Primary objectives

1. Deliver a fully native Android application.
2. Create a premium, dark, OLED-friendly streaming interface.
3. Use the public IPTV catalogue provided by iptv-org.
4. Support live television playback through Android Media3.
5. Implement an offline-first architecture with a local database.
6. Support channel search, categories, languages, favourites, profiles, and watch history.
7. Implement robust stream failover and playback error handling.
8. Optimize performance for real Android devices, including ARM-based devices.
9. Produce a debug APK that can be installed and tested.
10. Maintain clean architecture and readable, maintainable code.

Do not claim that a feature works, that a build succeeded, or that an APK was installed unless you have actually verified it.

---

2. MANDATORY PROJECT DIRECTORY

The project directory is:

"~/NisTV-Revamped"

This directory requirement overrides every other directory reference in this specification.

Before making changes:

1. Inspect the current working directory.
2. Check whether "~/NisTV-Revamped" already exists.
3. Inspect existing files before modifying anything.
4. Preserve useful existing implementation and configuration.
5. Create the directory if it does not exist.
6. Run all project-related commands from "~/NisTV-Revamped", unless a tool explicitly requires a different working directory.

Never create, modify, initialize, or build this project in "~/NisTV".

Do not accidentally create nested directories such as:

- "~/NisTV-Revamped/NisTV-Revamped/"
- "~/NisTV-Revamped/app/NisTV-Revamped/"

The Android project root must be exactly:

"~/NisTV-Revamped"

The expected final APK location is:

"~/NisTV-Revamped/app/build/outputs/apk/debug/app-debug.apk"

If an existing project is present, inspect it first and adapt it instead of unnecessarily overwriting working code.

---

3. DEVELOPMENT ENVIRONMENT AND COMPATIBILITY

The application is being developed in an Android-oriented environment that may use Termux on an ARM64 device.

Inspect the actual environment before selecting commands or changing configuration.

Check:

- Operating system and architecture
- Java version
- Gradle version, if available
- Android SDK location
- Android platform installations
- Android build-tools installations
- Android SDK command-line tools
- Gradle wrapper availability
- Available disk space
- Network access
- Existing project configuration

Do not assume that a desktop Linux or Windows environment is available.

3.1 Preferred technology versions

Use the following compatible versions unless the existing environment or a verified dependency constraint requires an adjustment.

Component| Version
Language| Kotlin
Java| 17
Android Gradle Plugin| 8.7.3
Gradle| 8.9
Kotlin| 2.0.21
KSP| 2.0.21-1.0.28
Compose BOM| 2024.12.01
Navigation Compose| 2.8.5
AndroidX Media3| 1.5.1
Coil| 3.0.4
Room| 2.6.1
DataStore| 1.1.1
WorkManager| 2.10.0
OkHttp| 4.12.0
kotlinx.serialization| 1.7.3
Credential Manager| 1.3.0
Google ID| 1.1.1

These are baseline versions, not permission to introduce incompatible dependencies. Verify plugin compatibility and use a consistent version catalog.

If a dependency cannot be downloaded or a version is unavailable in the actual environment, investigate the cause and document the adjustment.

Do not silently substitute incompatible versions.

3.2 Android configuration

Recommended initial configuration:

- "minSdk": 24
- "compileSdk": 34
- "targetSdk": 34
- Primary architecture: "arm64-v8a"
- Secondary architecture: "armeabi-v7a", where practical

Use a single Android application module:

":app"

Use a single main Activity.

Use Jetpack Compose for application UI.

Do not create unnecessary modules or introduce a multi-module architecture unless the existing project genuinely requires it.

3.3 Termux-specific requirements

If the environment is Termux, determine whether the installed Java, Android SDK, Gradle wrapper, and Android build tools are actually usable.

If the environment requires an AAPT2 override and the executable exists, use:

"/data/data/com.termux/files/usr/bin/aapt2"

Only configure this override when it is appropriate for the detected environment.

Potential Gradle properties:

org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8
org.gradle.parallel=false
org.gradle.caching=true
android.useAndroidX=true
kotlin.code.style=official

Adjust memory settings to match the device's actual resources.

Do not assume that the Android SDK exists simply because Java is installed.

If essential SDK components are missing, explain precisely what is missing and whether it can be installed in the current environment. Do not fabricate build results.

---

4. NON-NEGOTIABLE ARCHITECTURE RULES

Build a native Android application.

Required

- Kotlin
- Jetpack Compose
- Material 3 foundations, customized to match the NisTV design system
- AndroidX Navigation Compose
- AndroidX Media3 ExoPlayer
- MediaSessionService for background playback
- Room
- DataStore
- OkHttp
- Coil
- WorkManager, where background synchronization is appropriate
- Kotlin coroutines and Flow
- Manual dependency injection through a lightweight "AppContainer"

Forbidden

- WebView-based application UI
- React Native
- Flutter
- HTML/CSS-based application screens
- A website wrapped inside an Android application
- Emoji as interface icons
- Material Icons Extended as the default icon solution
- Hilt or Dagger for this project
- Unnecessary dependencies
- Fake analytics
- Fake popularity statistics
- Hardcoded fake playback statistics
- Fake EPG schedules
- Fake user accounts presented as authenticated accounts
- Fake cloud synchronization
- Placeholder buttons that look functional but do nothing

Use custom vector drawable icons or carefully designed Compose vector icons.

A small number of simple, locally bundled SVG or vector assets may be used where appropriate, but avoid introducing a large asset pipeline without a clear benefit.

The application must remain usable without an internet connection after its initial installation and seed-data initialization.

---

5. PRODUCT VISION

NisTV should feel like a premium live-TV application rather than a basic IPTV playlist player.

The interface should combine:

- OLED-friendly dark surfaces
- Elegant typography
- Large, high-quality channel artwork
- Smooth horizontal content rails
- Carefully controlled animations
- Responsive layouts
- A refined live-player overlay
- A fast search experience
- Clear loading and empty states
- Consistent visual hierarchy

The experience should be inspired by the quality and polish of modern streaming platforms without copying their branding or proprietary assets.

Product principles

1. Playback is the primary feature.
2. The home screen must be useful immediately.
3. Channels must be discoverable without navigating through multiple menus.
4. Loading states must not make the application feel frozen.
5. Errors must offer meaningful recovery actions.
6. Cached data should remain available offline.
7. No content metadata should be fabricated.
8. The application must clearly distinguish catalogue availability from actual stream availability.
9. Navigation and playback must work correctly across configuration changes.
10. Visual polish must never compromise accessibility or stability.

---

6. DESIGN SYSTEM

Create a unified design system before implementing individual screens.

6.1 Colour palette

Use a midnight OLED theme.

Suggested palette:

Token| Value
Background| "#07090D"
Main surface| "#10141C"
Elevated surface| "#171D27"
Primary text| "#F4F6FA"
Secondary text| "#9AA4B2"
Muted text| "#667181"
Accent green| "#39D98A"
Accent green pressed| "#27B875"
Divider| "#242B36"
Error| "#FF626B"
Warning| "#F4BE58"

Centralize colours in a dedicated theme package.

Do not scatter colour literals across every screen.

Use restrained gradients for featured content. Avoid excessive gradients, glowing borders, and unnecessary glassmorphism.

The background should remain predominantly dark to reduce visual noise and suit OLED displays.

6.2 Typography

Use a consistent typographic scale.

Suggested hierarchy:

- Featured heading: 28–34sp
- Screen title: 24–30sp
- Section heading: 18–22sp
- Channel name: 14–16sp
- Body: 14–16sp
- Supporting metadata: 12–14sp
- Compact badges: 10–12sp

Use a clean system sans-serif or a properly licensed bundled font.

Use consistent font weights and line heights.

Avoid clipping and unnecessary text truncation.

6.3 Layout and spacing

Use an 8dp-based spacing system.

Suggested values:

- Small: 4dp
- Compact: 8dp
- Medium: 12dp
- Standard: 16dp
- Large: 24dp
- Extra large: 32dp
- Section spacing: 32–44dp

Support different screen sizes and display densities.

Do not assume that every device has the same width.

6.4 Shapes

Use consistent rounded corners:

- Small controls: 8dp
- Buttons and compact cards: 12dp
- Channel cards: 14–18dp
- Featured surfaces: 20–24dp
- Full-screen modal sheets: 24dp top corners

Avoid making every element excessively rounded.

6.5 Animation

Use motion to communicate state changes.

Examples:

- Fade and slide screen transitions
- Animated bottom navigation selection
- Subtle channel-card focus feedback
- Image crossfades
- Smooth rail scrolling
- Player OSD fade-out
- Loading shimmer
- Modal sheet transitions
- Search-result transitions

Requirements:

- Avoid animations that block interaction.
- Avoid launching multiple redundant animations during recomposition.
- Respect system animation and reduced-motion preferences where practical.
- Avoid infinite animation loops on large parts of the screen.
- Never allow an animation failure to block playback or navigation.

6.6 Icons

Create a consistent icon set for:

- Home
- Live TV
- Search
- My List
- Profile
- Settings
- Play
- Pause
- Back
- Close
- Fullscreen
- Volume
- Mute
- Previous
- Next
- Reload
- Cast, only if actually implemented
- Picture-in-Picture
- Audio track
- Subtitle track
- Quality
- Report a channel
- Information

Use vector assets or Compose vector paths.

Do not use emoji as replacements for application icons.

Every icon-only interactive control must have an appropriate accessibility description.

---

7. APPLICATION NAVIGATION

Create a predictable navigation architecture.

Recommended primary destinations:

1. Home
2. Live TV
3. Search
4. My List
5. Profile

Settings can be accessible from the Profile screen.

Use Navigation Compose and a single Activity.

Preserve the expected navigation state when moving between primary destinations.

Avoid creating multiple Activities unnecessarily.

Handle Android Back navigation correctly.

The player should support returning to the originating screen without losing the user's context.

---

8. REQUIRED SCREENS

8.1 Startup and initialization

Implement a startup flow that:

1. Initializes the application container.
2. Opens the Room database.
3. Loads preferences.
4. Checks whether seed data is available.
5. Loads the minimum data required for the first usable screen.
6. Starts background synchronization when appropriate.
7. Navigates to the correct initial destination.

Do not block the main thread while reading large JSON files or performing database operations.

Show a polished loading screen only when necessary.

If the database already contains usable data, display the application promptly while background synchronization continues.

8.2 Authentication gate

Create an optional authentication gate.

Do not require authentication simply to browse public channels.

Where authentication is implemented, distinguish clearly between:

- Local-only profiles
- Google sign-in
- Authenticated account data

Do not claim that a user is authenticated unless authentication has actually succeeded.

If Google sign-in is not configured, keep the feature optional and present a clear explanation rather than a fake sign-in flow.

8.3 Profile selection

Support local profiles.

Each profile may contain:

- Display name
- Avatar identifier
- Preferred language
- Favourite channels
- Watch history
- Recently watched channels
- Optional parental-control settings

Persist profile data locally.

Allow users to create, select, rename, and remove local profiles.

Keep profile switching deterministic.

Avoid claiming that local profiles synchronize across devices.

8.4 Home screen

Build a polished, scrollable home screen.

Suggested structure:

1. Top app bar
2. Optional search shortcut
3. Featured live channel
4. Continue Watching, only when watch-history data exists
5. Recently Watched, only when history exists
6. Popular Categories, based on real catalogue categories
7. Channels by Language
8. Channels by Genre
9. Recommended Live Channels, selected through explicit editorial curation or deterministic rules
10. Additional channel rails where enough valid data exists

Do not display empty sections.

Do not invent popularity metrics or claim that a channel is trending without evidence.

Do not create fake viewing counts, viewer numbers, or recommendation scores that imply real-world activity.

A curated asset named "featured_in.json" may define a small number of featured channel IDs.

If it is absent, select featured channels using a deterministic rule based on the real catalogue. Clearly separate this from popularity-based recommendations.

Use channel logos and metadata from the source catalogue.

Use graceful placeholders when artwork is missing.

Tapping a channel should open the player.

The Home screen should remain usable with cached data when offline.

8.5 Live TV and channel browser

Create a dedicated channel browser.

Required features:

- Scrollable channel list or grid
- Channel logo
- Channel name
- Category
- Language
- Live status where supported
- Search/filter access
- Category filtering
- Language filtering
- Favourite action
- Channel information
- Clear indication of unavailable streams when such status is known

Avoid displaying unverified playback availability as guaranteed.

Provide empty states for categories that contain no eligible channels.

8.6 Search

Implement search across channel names and available metadata.

Search should support:

- Channel name
- Alternative channel names, if available
- Language
- Category
- Network
- Relevant metadata

Use a debounce interval for text input.

Use Room and its full-text search capability where appropriate.

Avoid reading the entire catalogue repeatedly on every keystroke.

Persist recent searches locally if the user enables or uses that feature.

Allow users to clear recent searches.

Display helpful empty states.

Search must work offline using the local catalogue.

8.7 My List

Implement a favourites screen.

Features:

- Add a channel to favourites.
- Remove a channel from favourites.
- Display favourites in a predictable order.
- Open a favourite channel.
- Preserve favourites across application restarts.
- Provide an empty state with a route to Live TV.

Favourites must be stored locally and associated with the appropriate profile.

8.8 Profile and settings

Implement settings for:

- Active profile
- Preferred language
- Appearance
- Playback preferences
- Background playback preference
- Picture-in-Picture preference
- Catalogue refresh
- Cache management
- Recent-search management
- Parental controls, if implemented
- Application information
- Data attribution
- Channel reporting

Do not add nonfunctional toggles.

If a setting requires platform support, explain or disable it appropriately.

---

9. IPTV CATALOGUE INTEGRATION

Use the public catalogue from the iptv-org project.

Official repository:

"https://github.com/iptv-org/iptv"

API documentation:

"https://github.com/iptv-org/api"

JSON catalogue base:

"https://iptv-org.github.io/api/"

Relevant resources:

- "channels.json"
- "feeds.json"
- "streams.json"
- "logos.json"
- "categories.json"
- "languages.json"
- "blocklist.json"
- "guides.json"

Expected URLs follow the JSON catalogue base, for example:

"https://iptv-org.github.io/api/channels.json"

Use the repository's published structured data.

Do not scrape GitHub HTML pages to extract channel data.

Inspect the actual JSON schemas and current field names before implementing the parser. Do not assume that every optional field is present.

Use Kotlin serialization with tolerant handling of missing or unknown fields.

Network failures, malformed records, and optional-field omissions must not crash the application.

9.1 Catalogue resources

Download the relevant catalogue resources and join their information correctly.

Channels

Use channel metadata such as:

- Channel ID
- Display name
- Alternative names
- Country
- Languages
- Categories
- Network
- Website
- NSFW indicator
- Other available metadata

Use only fields actually provided by the source.

Feeds

Use feed data where available to associate feeds with channels or provide additional metadata.

Streams

Associate streams with their channel IDs.

Preserve relevant source metadata, including:

- Stream URL
- Referrer
- User-Agent
- Quality
- Labels
- Other supported fields

Logos

Associate logos with their channel IDs.

Where multiple logo candidates exist, choose using a deterministic ranking.

Prefer:

1. Entries marked in use, when the field exists
2. Suitable raster formats over SVG where that simplifies reliable rendering
3. Larger usable images where dimensions are available
4. Stable fallback ordering

If no suitable logo exists, display a local placeholder.

Do not make the whole channel row fail because an image is missing.

Languages and categories

Join IDs to human-readable names.

Do not expose internal catalogue IDs as display labels when a human-readable value is available.

Map supported source categories into a small, consistent application taxonomy:

- Entertainment
- News
- Music
- Kids
- Religious
- Sports
- Movies
- Other

Preserve the original category information in the database if useful.

Do not misclassify channels based on their names alone when reliable category metadata is available.

9.2 Channel filtering

By default, focus the initial catalogue on India.

Use a configurable allowed-country set with a default value equivalent to:

"["IN"]"

Design the implementation so users or maintainers can extend the supported country set later.

Filter out channels explicitly marked NSFW.

Respect the catalogue's blocklist.

Do not treat a missing optional boolean field as equivalent to an explicit positive value without considering the source schema.

Apply the filtering rules consistently to both generated seed data and network synchronization.

9.3 Stream ordering

Maintain multiple streams per channel.

Rank sources using a deterministic policy.

Prefer:

1. Known, usable streams
2. Sources with known quality metadata when the quality preference favours them
3. Sources without known geo-blocking restrictions
4. Sources with fewer recorded failures
5. Stable source ordering as a tie-breaker

Do not claim that a stream is currently working simply because it exists in the catalogue.

Do not invent quality metadata.

Only show resolution badges when the quality can be established from available metadata or verified playback information.

Where a source explicitly includes relevant labels, show suitable notices such as:

- Geo-restricted
- Not available 24/7

Only show these notices when supported by source metadata.

Do not infer geo-restriction solely from a failed connection.

9.4 Optional M3U fallback

Use M3U parsing only if the structured JSON catalogue cannot be joined reliably or if an explicitly supported fallback is needed.

Potential fallback playlists:

"https://iptv-org.github.io/iptv/index.m3u"

"https://iptv-org.github.io/iptv/countries/in.m3u"

"https://iptv-org.github.io/iptv/languages/tam.m3u"

Do not use M3U as the primary source if the JSON pipeline is functioning correctly.

Keep parsing resilient to malformed entries.

Never assume that an M3U URL guarantees a working stream.

---

10. OFFLINE-FIRST DATA ARCHITECTURE

Use Room as the local source of truth.

Use DataStore for small application preferences and synchronization metadata.

Network requests should update the local database rather than directly driving every screen.

Use repositories to separate network, database, and presentation concerns.

Recommended structure:

app/
  src/
    main/
      AndroidManifest.xml
      java/
        <application-package>/
          NisTvApplication.kt
          MainActivity.kt

          core/
            common/
            database/
            network/
            preferences/
            designsystem/
            navigation/
            util/

          data/
            local/
            remote/
            repository/
            mapper/
            sync/

          domain/
            model/
            repository/
            usecase/

          playback/
            PlaybackService.kt
            PlayerController.kt
            StreamResolver.kt
            PlaybackState.kt

          feature/
            startup/
            auth/
            profile/
            home/
            livetv/
            search/
            favorites/
            player/
            settings/

      res/
        drawable/
        mipmap/
        values/

      assets/
        seed/
        featured_in.json

Adapt this structure to the existing project if necessary, but keep responsibilities clear.

Avoid creating dozens of empty packages or classes before they are needed.

10.1 Room schema

Create Room entities and relationships for at least the following concepts.

Channel

Suggested fields:

- "id"
- "name"
- "altNames"
- "country"
- "network"
- "website"
- "languageIds"
- "categoryIds"
- "logoUrl"
- "isNsfw"
- "labels"
- "updatedAt"

Use appropriate types and converters.

Do not store complex structures as arbitrary strings if a structured representation is more suitable.

StreamSource

Suggested fields:

- "id"
- "channelId"
- "url"
- "referrer"
- "userAgent"
- "quality"
- "labels"
- "priority"
- "failCount"
- "lastFailureAt"

A stream source belongs to a channel.

Support multiple sources per channel.

Profile

Suggested fields:

- "id"
- "name"
- "avatarId"
- "preferredLanguage"
- "createdAt"
- "updatedAt"

Favorite

Suggested fields:

- "profileId"
- "channelId"
- "createdAt"

Enforce appropriate uniqueness.

WatchHistory

Suggested fields:

- "profileId"
- "channelId"
- "lastWatchedAt"
- "watchDurationMs"

Use actual recorded playback data.

Do not fabricate durations or viewing history.

RecentSearch

Suggested fields:

- "id"
- "profileId"
- "query"
- "createdAt"

Programme

Use this only if guide data is implemented.

Suggested fields:

- Channel identifier
- Start time
- End time
- Title
- Description
- Source metadata

Create indexes for commonly queried columns.

Use foreign keys where appropriate.

Define deletion behaviour carefully so that catalogue refreshes do not accidentally erase unrelated profile data.

10.2 Full-text search

Use Room FTS4 or another supported full-text search mechanism for channel search.

Index the fields that materially improve discovery.

Keep the search index synchronized with channel updates.

Test insert, update, and delete behaviour.

If a complicated FTS relationship introduces build or runtime problems, implement a reliable simpler search first and document the trade-off.

10.3 Database transactions

Use transactions for catalogue imports and updates.

Never leave the database half-updated because one network resource failed.

Do not clear the complete catalogue before verifying the replacement data.

Keep favourites and profile data independent of catalogue synchronization.

---

11. SEED DATA AND FIRST LAUNCH

The application must be useful immediately after installation.

Create a seed-data generation process that uses the same filtering and joining logic as the live catalogue pipeline.

The seed data should contain a reasonable selection of eligible Indian channels and their available metadata.

Aim for a few hundred good channel entries where source data supports that target.

Do not fabricate channel records merely to reach a target count.

If the source provides fewer eligible entries, use the actual available records and report the resulting count.

Generate seed data during development rather than downloading the entire catalogue during every first launch.

Store the generated seed asset under an appropriate location, such as:

"app/src/main/assets/seed/"

Document how the seed data is generated.

The application should:

1. Open the database.
2. Detect whether the initial catalogue has already been imported.
3. Import the seed data once when needed.
4. Avoid repeatedly importing the same seed records.
5. Show cached data immediately.
6. Perform network synchronization separately.

Do not overwrite an existing, successfully populated database with the seed dataset on every launch.

Seed import must be transactional.

If seed parsing fails, provide a clear error path rather than silently presenting fabricated data.

---

12. NETWORK AND CATALOGUE SYNCHRONIZATION

Implement a dedicated catalogue synchronization component.

Use OkHttp.

Use Kotlin coroutines.

Run network and database operations off the main thread.

12.1 Conditional HTTP requests

For each resource, persist synchronization metadata, including:

- ETag
- Last-Modified
- Last successful synchronization time
- Last error, where useful
- Relevant version or source metadata

Send conditional requests using "If-None-Match" and "If-Modified-Since" when appropriate.

When the server returns HTTP 304 Not Modified, reuse the existing cached resource or existing database representation.

Do not re-import unchanged resources unnecessarily.

Respect server responses and HTTP caching rules.

Handle timeouts and connection errors.

Do not continuously retry a failing server in a tight loop.

12.2 Large JSON files

"streams.json" may be large.

Do not load the entire file into a giant in-memory string and then build a second full in-memory object graph.

Use a streaming or bounded-memory parsing strategy where practical.

Possible approaches include:

- Downloading to a temporary file and parsing incrementally
- Using a streaming JSON reader
- Processing records in bounded batches
- Staging parsed records before transactional database application

Choose an approach compatible with the selected serialization library.

Ensure the implementation is actually incremental rather than merely wrapping a fully buffered string in a reader.

Handle cancellation.

Delete temporary files after successful processing or safe failure.

Avoid unnecessary duplicate copies of large datasets.

12.3 Safe synchronization process

A full catalogue refresh should:

1. Fetch required resources.
2. Check response status.
3. Verify resource structure.
4. Parse records.
5. Apply filtering.
6. Join related metadata.
7. Prepare validated updates.
8. Update Room transactionally.
9. Update synchronization metadata after successful application.
10. Report the result to the UI.

If a required download fails, preserve the previous usable catalogue.

If parsing fails, preserve the previous usable catalogue.

If an individual optional record is malformed, skip it safely and record diagnostics where appropriate.

Do not erase the database because a remote endpoint temporarily fails.

12.4 Removing stale catalogue entries

Do not delete existing channels merely because a partial download omitted them.

Only consider removing stale channels after a verified successful full synchronization.

Use a conservative removal strategy, such as requiring the channel to be absent from three consecutive successful full catalogue synchronizations.

Ensure that the stale-removal process is scoped to catalogue-owned data.

Preserve user data and referential integrity.

12.5 Background refresh

Use WorkManager for appropriate deferrable catalogue refresh tasks.

Respect Android background execution constraints.

Do not promise exact periodic execution intervals when Android may defer work.

Provide a manual refresh action in Settings.

Show honest states:

- Never synchronized
- Synchronizing
- Last successful refresh
- Refresh failed
- Cached catalogue available

Do not display fabricated progress percentages.

---

13. MEDIA3 PLAYBACK ENGINE

Playback is a core requirement.

Use AndroidX Media3 ExoPlayer.

Implement playback using a dedicated service based on "MediaSessionService" where background playback is supported.

Keep player lifecycle management separate from composables.

Do not create a new ExoPlayer instance every time the player screen recomposes.

Use a reusable playback controller or manager.

Ensure the player is released appropriately when the application no longer needs it.

13.1 Playback responsibilities

Implement:

- Live stream playback
- Loading state
- Buffering state
- Playing state
- Paused state, where supported
- Error state
- Retry action
- Switch-source action
- Channel switching
- Player controls
- Fullscreen mode
- Aspect-ratio controls
- Audio-track selection where available
- Subtitle selection where available
- Playback lifecycle handling
- Background playback where enabled
- Picture-in-Picture where supported

Do not show track-selection controls as active when the stream provides no selectable tracks.

Do not claim that every live stream supports pausing, seeking, or timeshift.

Disable or hide unsupported operations.

13.2 Stream-specific headers

Some IPTV streams require HTTP headers.

Respect stream metadata supplied by the catalogue.

Support per-source:

- "Referer"
- "User-Agent"

Configure Media3's data-source layer to apply the selected source's headers to the relevant requests.

Use an appropriate "DataSource.Factory", such as an OkHttp-backed factory where suitable.

Avoid globally applying one channel's headers to unrelated streams.

Do not forward sensitive or unnecessary headers to unrelated hosts.

Do not bypass access controls, DRM, authentication, or geographical restrictions.

13.3 Source selection

When a channel has multiple sources:

1. Load the ordered source candidates.
2. Attempt playback with the preferred source.
3. Detect a playback failure.
4. Record the failure.
5. Attempt the next eligible source.
6. Avoid repeatedly selecting sources that have recently failed.
7. Update source ordering or temporary eligibility using actual failure history.
8. Inform the user when a source switch occurs.
9. Stop when all eligible sources have been attempted.

Display honest feedback such as:

“Trying another source (2 of 4)”

Only show the real attempt number and real candidate count.

Provide Retry and Try Another Source actions.

Do not retry forever.

A suggested retry schedule for transient errors is 1, 2, and 4 seconds, with a maximum of three retries for the same source before moving to the next source. Apply this only to errors where retrying is reasonable.

Do not repeatedly retry clearly unsupported formats or permanent HTTP errors.

Differentiate between:

- Network timeout
- DNS or connection failure
- HTTP error
- Unsupported media format
- Decoder failure
- Source ended
- Source unavailable

Avoid presenting every error as a generic network problem.

13.4 Playback error recovery

When playback fails:

- Show a clear error message.
- Preserve the channel context.
- Offer retry.
- Offer another source if available.
- Keep the user from being trapped in an infinite loading state.
- Allow returning to the channel list.
- Update actual source failure information.

Never mark a source as permanently dead after a single transient timeout.

13.5 Player controls

Design a custom Compose player interface around the Media3 video surface.

Include:

- Back button
- Channel name
- Channel logo, where available
- Live indicator, where appropriate
- Play/pause when supported
- Previous and next channel actions
- Favourite action
- Source selection
- Fullscreen toggle
- Aspect-ratio selection
- Audio and subtitle controls when available
- Settings or quality controls when supported
- Error recovery controls

Use Media3's actual playback state.

Do not create fake buffering or playback progress.

For a live stream without seeking support, do not display a misleading seek bar.

13.6 Gesture controls

Implement gestures only when they can be made reliable.

Potential interactions:

- Single tap: show or hide the player OSD.
- Double tap: play/pause or another explicitly defined action, only when supported.
- Horizontal swipe: channel switching, if it does not conflict with other controls.
- Vertical swipe: volume or brightness, if platform permissions and behaviour allow it.

Avoid conflicts between gestures and accessibility.

Ensure gesture zones do not make essential controls difficult to use.

Respect system brightness and audio APIs.

13.7 Picture-in-Picture

Implement Picture-in-Picture only if the device API level and Android manifest configuration support it.

Handle:

- Activity lifecycle
- Entering PiP
- Exiting PiP
- Player continuity
- Appropriate player controls
- Back navigation

Do not advertise PiP on unsupported configurations.

13.8 Background playback

Support background playback through the MediaSession service where appropriate.

Use the correct Android media playback foreground-service configuration and permissions for the target SDK.

Provide a user preference where useful.

Ensure that playback does not unexpectedly continue when the user has explicitly stopped it.

---

14. ELECTRONIC PROGRAMME GUIDE (OPTIONAL)

Use the guide catalogue only when reliable data is available.

Relevant catalogue resource:

"https://iptv-org.github.io/api/guides.json"

The guide catalogue can identify guide sources, but it does not itself guarantee that a complete programme schedule is available.

If implementing EPG:

1. Associate channels with the available guide identifiers.
2. Retrieve the relevant XMLTV data from supported sources.
3. Parse it lazily or incrementally.
4. Store the programme records locally.
5. Respect source availability and update frequency.
6. Avoid downloading unnecessarily large guide files.
7. Avoid blocking startup while importing schedules.

Display actual programme information only when it exists.

When guide data is unavailable, show a neutral label such as:

“Live Broadcast”

Do not fabricate:

- Programme names
- Start times
- End times
- Descriptions
- Upcoming schedules

EPG is lower priority than reliable channel discovery and playback.

If implementing it would compromise the core application or build stability, defer it and document the limitation.

---

15. CHANNEL ATTRIBUTION AND REPORTING

The application uses publicly published third-party catalogue data.

Provide a clear attribution notice on the Profile or About screen:

“Channel data from iptv-org. NisTV does not host or control streams.”

Do not imply that NisTV owns, operates, or guarantees third-party streams.

Where a reporting action is provided, it may direct users to the official iptv-org issue tracker:

"https://github.com/iptv-org/iptv/issues"

Explain that reports are submitted to the external project, not directly to NisTV, unless a separate reporting backend is implemented.

Do not automatically submit reports without the user's explicit action.

Do not claim to verify licensing or availability of every listed channel.

Use the catalogue in accordance with its applicable licence and contribution requirements.

---

16. SECURITY AND PRIVACY

Follow Android security best practices.

Requirements:

- Request only necessary permissions.
- Use HTTPS for supported network resources.
- Avoid logging sensitive information.
- Do not store credentials in plain text.
- Do not put secrets in source code.
- Do not upload user data without consent.
- Do not introduce analytics or tracking SDKs unnecessarily.
- Keep local profile information on-device unless the user explicitly enables a supported remote service.
- Validate external URLs before using them.
- Avoid exposing arbitrary local files or internal application data through exported components.
- Configure Android components with appropriate export settings.
- Handle network security configuration correctly.
- Do not disable TLS certificate verification.
- Do not bypass DRM, paywalls, authentication, or geographical restrictions.

Do not collect or transmit watch history to an external server unless a real, disclosed feature requires it and the user has consented.

---

17. PERFORMANCE REQUIREMENTS

Optimize for real Android hardware.

Targets are goals to measure, not results to claim without testing.

Metric| Target
Warm startup| Under 1.2 seconds where practical
First usable screen on a good network| Under 2.5 seconds where practical
Playback memory| Under approximately 350 MB where practical
Debug APK size| Aim for under 25 MB, subject to dependencies and bundled assets
Main-thread database work| None
Large catalogue parsing| Background, bounded-memory approach
Offline startup| Must work with cached data
Navigation responsiveness| No avoidable blocking
Playback continuity| Preserve the player across ordinary recompositions

Required optimizations

- Lazy Compose lists
- Stable item keys
- Efficient Room queries
- Indexed database columns
- Image caching
- Bounded JSON processing
- Coroutine cancellation
- Avoiding repeated database imports
- Avoiding redundant recompositions
- Avoiding unnecessary player recreation
- Avoiding large image decoding on the main thread
- Avoiding excessive object allocation
- Avoiding repeated catalogue downloads

Use Baseline Profiles only if they can be generated and integrated reliably.

Do not claim performance targets have been achieved without measurements.

---

18. ACCESSIBILITY

Provide a usable experience for a wide range of users.

Requirements:

- Appropriate content descriptions
- Minimum practical touch target of 48dp
- Sufficient text contrast
- Scalable text
- Keyboard navigation where applicable
- TalkBack-compatible controls
- Meaningful focus order
- Clear error messages
- Non-colour-only status indicators
- Motion that can be reduced
- Accessible player controls

Ensure decorative icons do not create redundant screen-reader announcements.

Verify that channel names, player actions, and error states remain understandable with accessibility services enabled.

---

19. TESTING STRATEGY

Write tests as the relevant components are implemented.

19.1 Unit tests

Test:

- Catalogue filtering
- Country selection
- NSFW filtering
- Blocklist handling
- Language mapping
- Category mapping
- Logo selection
- Stream ranking
- Stream failure ranking
- Search logic
- Seed import behaviour
- Stale-channel removal rules
- Synchronization failure handling
- Parsing malformed or incomplete records

19.2 Database tests

Test:

- Channel insert and update
- Stream-source relationships
- Favourite persistence
- Profile isolation
- Watch-history persistence
- Search indexing
- Transaction rollback
- Stale-channel handling
- Database migration behaviour

19.3 UI tests

Where the environment supports them, test:

- Initial screen
- Navigation
- Channel browsing
- Search
- Favourite add/remove
- Profile switching
- Player entry and exit
- Loading states
- Error states

19.4 Playback tests

Use a known, authorized test stream or a locally controlled test source when possible.

Do not rely exclusively on arbitrary public IPTV streams for automated tests because they can be offline, geo-restricted, rate-limited, or changed without notice.

Test:

- Player initialization
- Loading
- Playback failure
- Retry
- Source failover
- Player release
- Rotation or configuration changes
- Background lifecycle where supported

If no Android emulator or physical device is available, report that runtime playback testing could not be completed.

A successful compilation is not proof that playback works.

---

20. MILESTONE-BASED IMPLEMENTATION

Implement the project in the following order.

After each milestone, run the appropriate build or tests, diagnose failures, and fix them before proceeding.

Do not implement every feature in one enormous, unverified change.

M0 — Environment and project foundation

Tasks:

1. Inspect "~/NisTV-Revamped".
2. Inspect Java, Gradle, SDK, and build tools.
3. Establish the project configuration.
4. Configure plugin and dependency versions.
5. Create the single ":app" module if required.
6. Configure Kotlin and Compose.
7. Configure the Android manifest.
8. Create the application class and main Activity.
9. Add the basic theme.
10. Build the empty application.

Expected result:

- Correct project directory
- Valid Gradle configuration
- Working Android application skeleton
- Successful debug build, if the environment supports it

Do not proceed while the build configuration is broken.

M1 — Design system and navigation

Tasks:

1. Implement the colour palette.
2. Implement typography and spacing tokens.
3. Implement shared buttons and surfaces.
4. Create vector icons.
5. Create the main navigation structure.
6. Implement screen scaffolds.
7. Add loading, empty, and error components.
8. Implement basic screen transitions.
9. Verify accessibility semantics.

Expected result:

- Consistent dark visual design
- Navigation between primary destinations
- No placeholder navigation that crashes

Build and verify.

M2 — Database and catalogue pipeline

Tasks:

1. Implement Room entities.
2. Implement DAOs and indexes.
3. Implement database relationships.
4. Implement full-text search where appropriate.
5. Implement catalogue JSON models.
6. Inspect actual source schemas.
7. Implement filtering and joins.
8. Implement logo selection.
9. Implement stream ranking.
10. Create the seed-generation process.
11. Import seed data once.
12. Display the local channel catalogue.
13. Add resilient image loading.

Expected result:

- Channels available offline
- Database survives application restarts
- Searchable channel catalogue
- Reliable seed import

Build and test.

M3 — Home screen

Tasks:

1. Implement the featured channel area.
2. Implement channel cards.
3. Implement category rails.
4. Implement language rails.
5. Implement recently watched data where available.
6. Implement favourite actions.
7. Implement useful empty states.
8. Connect all displayed content to the real database.
9. Add appropriate loading states.

Expected result:

- Fully populated Home screen based on actual catalogue data
- Responsive scrolling
- No fabricated catalogue metrics

Build and test.

M4 — Media3 player

Tasks:

1. Configure Media3 dependencies.
2. Implement the player controller.
3. Implement "MediaSessionService".
4. Implement the video surface.
5. Connect channel selection to playback.
6. Implement playback states.
7. Implement stream-specific HTTP headers.
8. Implement Retry.
9. Implement Try Another Source.
10. Handle player cleanup.
11. Handle configuration changes.

Expected result:

- A functioning player architecture
- Real playback attempts
- Recoverable errors
- Correct player lifecycle

Build and test.

M5 — Advanced player experience

Tasks:

1. Implement the player OSD.
2. Implement supported transport controls.
3. Implement fullscreen.
4. Implement aspect-ratio controls.
5. Implement channel switching.
6. Implement audio and subtitle selection when available.
7. Implement source-selection UI.
8. Implement failure feedback.
9. Add gestures only where reliable.
10. Verify player accessibility.

Expected result:

- Polished player interface
- Honest controls based on actual stream capabilities

Build and test.

M6 — Search, favourites, profiles, and settings

Tasks:

1. Implement search UI.
2. Connect search to Room.
3. Implement recent searches.
4. Implement favourites.
5. Implement local profile creation and switching.
6. Implement profile-specific preferences.
7. Implement settings.
8. Implement cache management.
9. Implement About and attribution screens.

Expected result:

- Persistent local user experience
- Functional navigation and settings

Build and test.

M7 — Synchronization and optional EPG

Tasks:

1. Implement conditional HTTP requests.
2. Persist ETag and Last-Modified metadata.
3. Implement safe catalogue refresh.
4. Implement bounded-memory processing for large resources.
5. Implement WorkManager refresh.
6. Implement manual refresh.
7. Implement synchronization status.
8. Implement safe stale-channel removal.
9. Add EPG only if the core application remains stable.

Expected result:

- Safe updates
- Preserved offline data
- Clear synchronization status

Build and test.

M8 — Background playback and resilience

Tasks:

1. Verify MediaSession behaviour.
2. Implement background playback configuration.
3. Implement Picture-in-Picture where supported.
4. Improve stream failover.
5. Add failure counters.
6. Improve retry classification.
7. Test player lifecycle transitions.
8. Verify notification and foreground-service behaviour.

Expected result:

- More reliable playback across application lifecycle changes

Build and test.

M9 — Final polish and delivery

Tasks:

1. Remove unused code.
2. Fix warnings where practical.
3. Verify application permissions.
4. Verify manifest declarations.
5. Review accessibility.
6. Review error handling.
7. Review performance.
8. Run unit tests.
9. Run supported integration and UI tests.
10. Build the final debug APK.
11. Verify that the APK exists.
12. Report the actual file size.
13. Provide the exact APK location.
14. Provide installation instructions appropriate to the environment.

Do not claim runtime verification if only compilation was possible.

---

21. REQUIRED BUILD AND DELIVERY PROCESS

Build using the Gradle wrapper from the project root.

Expected project root:

"~/NisTV-Revamped"

Build command:

cd ~/NisTV-Revamped
./gradlew :app:assembleDebug --no-daemon

If Gradle is unavailable, diagnose the issue rather than assuming the build succeeded.

Do not run conflicting builds concurrently.

When the build fails:

1. Read the complete relevant error output.
2. Identify the first meaningful error.
3. Determine whether the cause is dependency resolution, SDK configuration, Kotlin compilation, resources, manifest configuration, or code.
4. Fix the root cause.
5. Rebuild.
6. Repeat until the build succeeds or a genuine environmental blocker remains.

Do not hide errors by disabling essential checks.

Do not randomly downgrade dependencies to make the error disappear.

APK location

Expected output:

"~/NisTV-Revamped/app/build/outputs/apk/debug/app-debug.apk"

Verify that this file actually exists after the build.

If appropriate in Termux, open the APK using:

termux-open ~/NisTV-Revamped/app/build/outputs/apk/debug/app-debug.apk

Only use that command if "termux-open" is available and suitable for the environment.

If the device cannot install the APK directly, provide the actual output location and explain the next step.

Do not claim that the application has been installed or launched unless you verified it.

---

22. FINAL ACCEPTANCE CRITERIA

The application is ready for initial delivery when the following requirements are satisfied or their limitations are explicitly documented.

Project

- [ ] Project root is exactly "~/NisTV-Revamped".
- [ ] No project files were accidentally created in "~/NisTV".
- [ ] Gradle configuration is consistent.
- [ ] The application compiles, if the environment permits.
- [ ] The debug APK exists.

UI

- [ ] Dark OLED-friendly theme is implemented.
- [ ] Primary navigation works.
- [ ] Home screen uses actual catalogue data.
- [ ] Channel browser works.
- [ ] Search works offline.
- [ ] Favourites persist.
- [ ] Profile functionality is honest and functional.
- [ ] Empty and error states are implemented.
- [ ] Icons and controls have accessibility descriptions.

Data

- [ ] Seed data is generated from real catalogue information.
- [ ] Initial catalogue import works.
- [ ] Room stores catalogue data.
- [ ] Catalogue joins are correct.
- [ ] Missing optional metadata does not crash the application.
- [ ] Failed synchronization does not wipe existing data.
- [ ] Large JSON processing is bounded in memory.
- [ ] Stale removal is conservative.
- [ ] No fabricated channel metadata is displayed.

Playback

- [ ] Media3 is configured.
- [ ] Player lifecycle is managed correctly.
- [ ] Stream-specific headers are supported.
- [ ] Multiple sources can be attempted.
- [ ] Retry is bounded.
- [ ] Source failover is implemented.
- [ ] Unsupported controls are not falsely presented as available.
- [ ] Errors are actionable.
- [ ] Runtime playback testing is documented honestly.

Delivery

- [ ] Build result is reported accurately.
- [ ] APK path is verified.
- [ ] APK size is measured.
- [ ] Remaining limitations are listed.
- [ ] No successful tests are claimed without evidence.

---

23. EXECUTION RULES FOR THE CODING AGENT

Follow these rules throughout implementation.

1. Start by inspecting "~/NisTV-Revamped".
2. Do not ask unnecessary questions when a reasonable implementation decision can be made safely.
3. Do not overwrite useful existing code without inspecting it.
4. Do not build the project in "~/NisTV".
5. Do not stop after producing a plan.
6. Do not create fake implementations for complex features.
7. Do not leave core buttons nonfunctional.
8. Do not fabricate channels, streams, programme schedules, popularity metrics, or test results.
9. Do not place large catalogue operations on the main thread.
10. Do not load the complete "streams.json" file into a giant string.
11. Do not clear working database contents when synchronization fails.
12. Do not recreate ExoPlayer during ordinary Compose recomposition.
13. Do not suppress errors without understanding them.
14. Do not make unnecessary architecture changes during later milestones.
15. Do not claim that a build, test, installation, or playback attempt succeeded without actual verification.
16. Prefer small, verifiable changes over large untested edits.
17. Keep the code readable and maintainable.
18. Build after every milestone.
19. Fix compilation failures before proceeding.
20. Report environmental blockers precisely.

If a feature cannot be completed because of missing SDK components, unavailable network access, incompatible dependencies, missing credentials, or lack of a test device, continue with independent work where possible and clearly identify the blocker.

Do not misrepresent an incomplete feature as complete.

---

24. START NOW

Begin immediately.

First actions

1. Inspect "~/NisTV-Revamped".
2. Inspect the current Java, Gradle, Android SDK, and build-tool environment.
3. Review any existing project files and preserve useful work.
4. Determine whether the environment can compile an Android application.
5. Establish the project configuration.
6. Implement M0: Environment and project foundation.
7. Build the application.
8. Fix any build errors that can be resolved.
9. Continue to M1 and subsequent milestones without waiting for unnecessary confirmation.
10. Keep a concise implementation log describing changes, build results, and remaining blockers.

The final goal is a polished, native Android IPTV application—not merely a UI mockup.

Project directory: "~/NisTV-Revamped"

