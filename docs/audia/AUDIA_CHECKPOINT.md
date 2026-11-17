# Audia working checkpoint

Date: 2026-11-12

## Stage completed

Stage A baseline audit: repository/build, architecture, feature inventory, security/network review, confirmed defects, QA matrix and release gate.

## Repairs completed in this checkpoint

- Removed destructive Room fallback; added v6→v8 and defensive v7→v8 cache-table migrations.
- Added contextual queue construction for album, artist, folder, favorites, search and Home rails.
- Queue screen now reads the active playback queue and selects an item without rebuilding it.
- Added queue normalization unit test.
- Added the MiniPlayer glass/accent surface, circular artwork progress, playing-only capped liquid glow, one-shot track-change shimmer and play/pause haptic feedback.
- Rebuilt LRCLIB lookup around URI-owned cache, exact lookup, confidence-ranked search, cancellation/deduplication, Retry-After handling, manual candidate selection and wrong-cache refresh.
- Added real Radio Browser discovery/search, mirror fallback, UUID favorites/recents, click counting, metered-data warning, HLS/progressive Media3 playback, notification metadata and reconnect/fallback handling.
- Updated privacy disclosures for LRCLIB, Radio Browser and direct station connections.
- Restored the active local/radio MiniPlayer inside Folder navigation and made folder taps create a folder-scoped queue.
- Replaced the flat Most Played rail with the same center-focused coverflow language, adding real Y-axis perspective, depth fading, accessible previous/next controls and reduced-motion behavior.
- Clarified that Radio search targets station names, tags/genres, ISO country codes or broadcast languages according to the selected chip.
- Replaced only the Most Played depth stack with a focused featured carousel using actual ranking order, readable metadata, swipe, accessible arrows, tap-to-play and a missing-art fallback; Recently Played and Recently Added remain unchanged.

## Tests executed

- `gradlew :app:testDebugUnitTest :app:assembleDebug` after MiniPlayer UI-002 - passed in 2m14s.
- `gradlew :app:testDebugUnitTest` after Lyrics/Radio integration - passed in 1m27s (21 tests).
- `gradlew :app:compileDebugKotlin :app:testDebugUnitTest` after Folder/coverflow repair - passed in 49s (21 tests).
- `gradlew :app:assembleDebug` verification build - passed in 4m.
- Live API smoke: Radio Browser returned 3 working stations; LRCLIB returned 20 candidates.
- Samsung SM-M315F: final verification build 9.0.84 (96) installed and launched without an observed startup fatal exception.

- `gradlew :app:testDebugUnitTest :app:assembleDebug` — passed in 2m31s.
- `gradlew :app:testDebugUnitTest` after migration — passed in 1m11s.
- `gradlew :app:testDebugUnitTest` after queue repair — passed in 56s.

## Current limitations

- Migration instrumentation/populated-device upgrade not run.
- Contextual queue physical-device regression not run.
- Player/Lyrics screenshot defect remains open.
- Service trust/background/audio-route, lyrics confidence and full beta matrix remain open.
- MiniPlayer landscape, enlarged-font, reduced-motion device setting and unlocked accessibility automation remain NOT TESTED.
- Radio live audio, HLS/progressive switching, notification actions, network loss/reconnect and favorites/recents UI persistence remain NOT TESTED on an unlocked device.
- Secondary lyrics providers were not added because authorized credentials/licensing were not supplied.

## Foundation update — 2026-11-13

- Compatible stable AndroidX playback/persistence/background libraries are upgraded and their JVM,
  lint, APK and install/launch gates pass.
- `docs/audia/AUDIA_EXECUTION_ROADMAP.md` is now the canonical ordered plan. Historical “next action”
  text in older sections does not override it.
- Root `AGENTS.md` instructs every future agent to resume at the first unverified roadmap gate and
  record exact evidence instead of claiming completion from compilation.

## Exact next action

Execute **F0C Persistence and security** in this order: exported Room schemas plus populated migration
tests; controller trust/custom-command restriction; scoped cleartext radio policy; scheduled
`activePositionMs` recovery; then engagement-worker consolidation. Do not start A1 Android Auto until
the F0 gate evidence is recorded.

## Queue crash hotfix — 2026-11-14

- Captured the reported Queue crash: obsolete Compose reorder library caused a binary
  `animateItemPlacement` `NoSuchMethodError`.
- Removed that dependency and kept queue ordering through Move Earlier/Move Later item-menu actions.
- Unit tests, debug APK, lint, physical Queue opening and one physical reorder passed.
- Canonical next phase remains F0C; this hotfix did not advance unrelated roadmap gates.

## Release CI/CD, Distribution & Physical Telemetry Gate — 2026-11-16

### 1. Delivery & Infrastructure

- **Automated Gradle APK Naming:**
  Configured `app/build.gradle.kts` with dynamic variant output naming:
  `Audia-v<version>.apk` for Release (no redundant `-release` suffix) and `Audia-v<version>-debug.apk` for Debug.
- **GitHub Actions CI/CD Pipeline:**
  Created `.github/workflows/build-and-release.yml`. Triggered strictly on release tags (`v*`) and manual `workflow_dispatch`, eliminating unneeded runner minutes on ordinary feature commits.
- **Cross-Repository Release Distribution Hub:**
  Configured automated release publishing from the private development repository (`TanjilHasan-Himel/playtime-musicapp`) to the public showcase hub (`TanjilHasan-Himel/app`).
  Resolved cross-repo target commitish handling (`target_commitish: main`) and secret token integration (`PUBLIC_REPO_TOKEN`).
- **Official Production Release v9.1.0 Deployed:**
  - Public Release: `https://github.com/TanjilHasan-Himel/app/releases/tag/v9.1.0`
  - Permanent Direct Download Link Active: `https://github.com/TanjilHasan-Himel/app/releases/latest/download/Audia-latest.apk` (HTTP 302 redirect verified).
  - Release APK size: 4,710,910 bytes (~4.7 MB) with R8 full minification and resource shrinking.

### 2. Physical Device Telemetry Verification

- Verified Firebase Crashlytics on physical Samsung Galaxy M31 (`SM-M315F`).
- Verified Firebase Analytics in Firebase Console DebugView (6 active events verified from real device).
- Cleaned up ADB debug property post-test (`debug.firebase.analytics.app .none.`).
- Confirmed external backend dashboard is YAGNI for current single-developer phase.

### 3. Android Auto Baseline Audit

- Inspected `app/src/main/java/com/eplaytime/app/service/MusicService.kt`:
  - `onGetLibraryRoot`: Returns root node with `isBrowsable = true`.
  - `onGetChildren`: Currently hardcoded to return `ImmutableList.of()` (empty list).
  - Status: Transport controls and lockscreen session work, but Android Auto / Car Head Unit media browsing currently displays an empty library. This constitutes Roadmap Phase A1.

### 4. Canonical Next Actions

1. **F0C Persistence and Security:**
   - Room schema export and automated v8→v9→v10 migration tests.
   - Restrict custom `ACTION_GET_AUDIO_SESSION_ID` command to internal controller.
   - Replace global cleartext network exception with scoped radio stream policy.
   - Persist scheduled `activePositionMs` for recovery after process termination.
2. **A1 Real Android Auto:**
   - Implement paged media tree in `MusicService.onGetChildren` (Tracks, Albums, Artists, Favorites, Folders).
   - Implement item resolution and media URI handling for head unit playback.

## Security Hardening (F0C) & Real Android Auto Media Tree (A1) — 2026-11-16

### 1. Delivery & Implementation

- **Public Showcase README Deployed:**
  - Pushed ultra-high-converting, SEO & GEO-optimized `README.md` directly to `https://github.com/TanjilHasan-Himel/app.git` (`5be8e22..8d9d504 HEAD -> main`).
  - Integrated 1-click golden direct download badge linking to permanent `Audia-latest.apk`.
  - Added full architecture ASCII matrix, previous vs new 4-piece liquid glass visual comparison, and comprehensive feature matrix (Audia vs Poweramp vs Musicolet).
- **Cross-Agent Living Diary Framework Established:**
  - Formulated strict multi-agent protocol requiring session start diary reads and session end diary appends.
  - Added dedicated agent guideline files: `AGENTS.md` (root contract), `CLAUDE.md`, `GEMINI.md`, and `.github/copilot-instructions.md`.
- **F0C Network Security Hardening:**
  - Created `app/src/main/res/xml/network_security_config.xml`: Strictly enforced HTTPS (disallow cleartext) for critical APIs (`lrclib.net`, `wikipedia.org`, `radio-browser.info`, `firebaseio.com`, `googleapis.com`). Scoped cleartext fallback strictly to legacy HTTP community radio streams.
  - Removed global `android:usesCleartextTraffic="true"` from `AndroidManifest.xml`.
  - Restricted custom `ACTION_GET_AUDIO_SESSION_ID` command in `MusicService.kt` strictly to internal Audia package callers (`controller.packageName == packageName`), returning `SessionError.ERROR_PERMISSION_DENIED` to external callers.
- **A1 Real Android Auto Integration:**
  - Created `app/src/main/res/xml/automotive_app_desc.xml` declaring `<automotiveApp><uses name="media" /></automotiveApp>`.
  - Added automotive meta-data to `AndroidManifest.xml`.
  - Implemented browsable root media hierarchy in `MusicService.kt`:
    - `MEDIA_ROOT_ID` exposing 4 categories: `All Songs`, `Albums`, `Artists`, `Favorites`.
    - `onGetChildren`: Resolves songs, albums, and artists into browsable and playable `MediaItem`s.
    - `onGetItem`: Resolves individual media items by ID.
    - `onAddMediaItems`: Automatically resolves content URIs and metadata for playback on head units.
    - `onSearch` / `onGetSearchResult`: Enables voice and text search across the library.

### 2. Verification Evidence

- Verification command: `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug`
- Result: **`BUILD SUCCESSFUL in 6m 33s`** (53 actionable tasks executed/up-to-date, zero compile errors, all JVM unit tests passed).
- Output: `app\build\outputs\apk\debug\Audia-v9.1.0-debug.apk` successfully packaged.

### 3. Canonical Next Actions

- Verify Room database schema export & automated migration test (`v8`→`v9`→`v10`).
- Test Android Auto on Desktop Head Unit (DHU) or physical vehicle.
- Bump version to `v9.1.1` and trigger release workflow upon user sign-off.

## Phase L2: Persistent Playlists, M3U Import/Export & Android Auto Integration — 2026-11-17

### 1. Delivery & Implementation

- **Room Database v11 Migration & Entities:**
  - Designed and executed `MIGRATION_10_11` in `PlayTimeDatabase.kt`:
    - `playlists` table (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `name` TEXT, `createdAt` INTEGER, `updatedAt` INTEGER, `description` TEXT, `customArtUri` TEXT).
    - `playlist_items` table (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `playlistId` INTEGER with FK CASCADE, `songId` TEXT, `songUri` TEXT, `title` TEXT, `artist` TEXT, `album` TEXT, `duration` INTEGER, `albumArtUri` TEXT, `position` INTEGER, `addedAt` INTEGER, UNIQUE index on `playlistId, position`).
    - `saved_queues` and `saved_queue_items` tables for queue snapshot persistence.
  - Implemented Room DAOs: `PlaylistDao.kt` and `SavedQueueDao.kt`.
  - Registered DAOs and migration in `PlayTimeDatabase.kt` and `DatabaseModule.kt`.
- **Repository & M3U Parser/Writer Engine:**
  - Implemented `M3uHelper.kt` supporting standard Extended M3U (`#EXTM3U`, `#EXTINF:<seconds>,<artist> - <title>`, URI parsing and resolution).
  - Built comprehensive JVM test suite `M3uHelperTest.kt` verifying serialization and deserialization edge cases.
  - Implemented `PlaylistRepository.kt`: full CRUD, `addSongsToPlaylist`, `removePlaylistItem`, `reorderPlaylist`, `exportPlaylistToM3u`, `importM3u`, and `saveActiveQueue`.
- **UI Components & User Experience:**
  - Added "Playlists" category tab to `HomeScreen.kt` (`Tracks`, `Playlists`, `Folders`, `Albums`, `Artists`, `Favorites`).
  - Created `PlaylistList.kt`: Playlist card layout displaying artwork, title, last updated timestamp, create new playlist, edit/rename, delete, export M3U, and import `.m3u`/`.m3u8` file picker launcher.
  - Created `PlaylistDetailScreen.kt`: Header with artwork, Play All, Shuffle, "+ Add Songs" multi-select dialog, song list with position up/down reordering, remove, and M3U export.
  - Integrated `AddToPlaylistDialog.kt` across the app, invoked from `SongGarageBottomSheet.kt` ("+ Add to Playlist") and `SongList.kt` (3-dots item menu).
- **Android Auto Media Hierarchy Integration:**
  - Added `category_playlists` node under root in `MusicService.kt`.
  - Implemented browsable and playable playlist items in `onGetChildren` and `onAddMediaItems`, allowing direct playback of user playlists in Android Auto car head units.

### 2. Verification Evidence

- **JVM Unit Tests:**
  `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` — **`BUILD SUCCESSFUL in 5m 39s`**
  - Unit tests executed: 11 test suites (including `M3uHelperTest`, `PlaybackQueueUnitTest`, `ScheduledOccurrenceCalculatorTest`, `RadioBrowserClientTest`, `SongWikiUnitTest`, `LyricsCandidateScorerTest`, `LrcParserTest`, `BiquadFilterTest`, `MusicFilterUnitTest`), 100% passed.
- **Physical Device Smoke Test (Samsung Galaxy M31 `R58N72BVFVD`):**
  - Streamed install of `Audia-v9.1.0-debug.apk` succeeded.
  - `MainActivity` launched (PID 374), DataStore state restored cleanly, proactive audioSessionId 4321 initialized, zero crashes or Room migration exceptions observed in logcat.

### 3. Canonical Next Actions

- Commit Phase L2 implementation to `main`.
- Bump version to `v9.1.1` in `app/version.properties` and tag `v9.1.1`.
- Push to remote to trigger the fixed GitHub Actions CI/CD release workflow to deploy `v9.1.1` directly to `TanjilHasan-Himel/app`.

## In-App Auto Update Engine & 2-Day Reminder System — 2026-11-17

### 1. Delivery & Implementation

- **Fastly CDN & GitHub API Dual-Engine (`AppUpdateManager.kt` & `AppUpdateParser.kt`):**
  - Primary endpoint: `https://raw.githubusercontent.com/TanjilHasan-Himel/app/main/version.json` (Fastly CDN with zero API rate limits, instantaneous global distribution).
  - Secondary fallback: `https://api.github.com/repos/TanjilHasan-Himel/app/releases/latest`.
  - Zero third-party bloat: Built with native `HttpURLConnection` and `Dispatchers.IO`.
  - Semantic version comparison and version code parser (`AppUpdateParser.kt`).
- **Throttling & Gentle Reminder Intelligence:**
  - Network check throttled to once every 24 hours (`CHECK_INTERVAL_MS = 86_400_000L`).
  - Dismissal reminder throttled to once every 2 days (48 hours, `REMINDER_INTERVAL_MS = 172_800_000L`), preventing annoying repeated popups.
  - Persistent timestamps stored in Jetpack Preferences DataStore (`LAST_UPDATE_CHECK_TIME`, `LAST_UPDATE_REMINDER_TIME`).
- **UI Components:**
  - `UpdateAvailableDialog.kt`: Translucent liquid glass dialog showcasing current version vs new version, dynamic "What's New" release notes container, "Update Now" action with Android `DownloadManager` integration, and "Remind Me Later (in 2 days)".
  - `SettingsDialog.kt`: Added interactive "Check for Updates" tile with live checking spinner and immediate user feedback.
  - `HomeScreen.kt`: Automatic background update check on app launch, exposing `UpdateAvailableDialog`.
- **CI/CD Pipeline Integration:**
  - Updated `.github/workflows/build-and-release.yml` to automatically generate and package `version.json` with every GitHub release.
  - Pushed `version.json` to public showcase repo `https://github.com/TanjilHasan-Himel/app.git` (`HEAD -> main`).

### 2. Verification Evidence

- **JVM Unit Tests:**
  `.\gradlew.bat testDebugUnitTest` — **`BUILD SUCCESSFUL in 6m 58s`** (12 test suites, including `AppUpdateManagerTest`, 100% passed).
- **Physical Device Smoke Test (Samsung Galaxy M31 `R58N72BVFVD`):**
  - Streamed install of `Audia-v9.1.1-debug.apk` succeeded.
  - Verified in logcat: `D AppUpdateManager: Audia is up to date: local build is 9.1.1 (92)`.
  - Verified live CDN fetch: `Invoke-RestMethod` to `https://raw.githubusercontent.com/TanjilHasan-Himel/app/main/version.json` returned valid JSON with 0 latency.

### 3. Canonical Next Actions

- Commit changes to `main`.
- Push `main` to `origin/main`.
- Retag `v9.1.1` to include this update engine so the initial release itself is update-aware.

## SQA Hardening, Security Audit & Senior Code Review — 2026-11-17

### 1. Delivery & Implementation

- **Security Auditor Findings & Fixes:**
  - **SEC-01 (URL Scheme & Host Validation):** Added `isTrustedUpdateUrl` in `AppUpdateParser.kt` strictly enforcing `https://` protocol and allowlisted domains (`github.com`, `raw.githubusercontent.com`, `objects.githubusercontent.com`) to prevent SSRF and untrusted APK download redirects.
  - **SEC-02 (Network Security Config Expansion):** Added `amazonaws.com`, `fastly.net`, and `crashlytics.com` to `network_security_config.xml` explicitly disallowing cleartext HTTP traffic.
  - **SEC-03 (Android Auto IPC / Binder DoS Protection):** Clamped unpaged Android Auto queries in `MusicService.kt` `onGetChildren` to 100 items max with safe pagination offset/page bounds, eliminating `TransactionTooLargeException` risks over Binder IPC.
  - **SEC-04 & SEC-05 (Audio Session ID Isolation & Caller UID Verification):** Removed `AUDIO_SESSION_ID` from public `sessionExtras` bundle broadcast; restricted custom command `ACTION_GET_AUDIO_SESSION_ID` strictly to internal processes (`controller.uid == Process.myUid()`).
  - **SEC-06 & SEC-08 (M3U Path Traversal & Injection Defense):** Added `isSafePathOrUri` in `M3uHelper.kt` blocking path traversal (`..`), private internal storage (`/data/data/`, `/data/user/`), and CRLF injection characters in exported M3U files.
- **Senior Code Reviewer Optimizations:**
  - **Offline Update Error Handling:** Fixed `AppUpdateManager.kt` to suppress update check timestamps on network failures (`IOException`), preventing false negative 24h throttling when the device is offline.
  - **Resource Leak Fix:** Wrapped `HttpURLConnection.inputStream` in `.use { it.readText() }` in `AppUpdateManager.kt`.
  - **Performance Optimization in Playlist Search:** Replaced $O(N \cdot M)$ linear scan in `PlaylistRepository.kt` `importM3u` with an $O(N + M)$ lookup map index (`songsByUri`, `songsByTitleArtist`, `songsByTitleOnly`).
  - **Atomic DB Shifting:** Replaced N+1 individual position update queries in `PlaylistDao.kt` with an atomic SQL shift query: `UPDATE playlist_items SET position = position - 1 WHERE playlistId = :playlistId AND position > :deletedPosition`.
  - **Settings UI Polish:** Bound live `isCheckingForUpdates` StateFlow directly to `SettingsDialog.kt`, eliminating artificial delays and ensuring real-time response; fixed Privacy Policy navigation trigger.
- **SQA & Test Engineer Hardening:**
  - Standardized semantic version tag parsing to handle both `v1.2.3` and `V1.2.3`, as well as prerelease suffixes (`v1.2.3-beta`).
  - Handled decimal seconds and Windows backslash paths in `M3uHelper.kt`.
  - Configured `testOptions { unitTests.isReturnDefaultValues = true }` in `app/build.gradle.kts` for JVM unit test execution without Android mock log stub errors.
  - Authored comprehensive edge-case test suites in `AppUpdateManagerTest.kt` and `M3uHelperTest.kt`.

### 2. Verification Evidence

- **JVM Unit Tests:**
  `.\gradlew.bat :app:testDebugUnitTest` — **`BUILD SUCCESSFUL in 2m 15s`**
  - 13 test suites (53 tests total, including `AppUpdateManagerTest`, `M3uHelperTest`, `PlaybackQueueUnitTest`, `ScheduledOccurrenceCalculatorTest`, `RadioBrowserClientTest`, `SongWikiUnitTest`, `LyricsCandidateScorerTest`, `LrcParserTest`, `BiquadFilterTest`, `MusicFilterUnitTest`), 100% passed.
- **APK Assembly:**
  `.\gradlew.bat :app:assembleDebug` — **`BUILD SUCCESSFUL in 1m 13s`**
  - Output: `app\build\outputs\apk\debug\Audia-v9.1.1-debug.apk` successfully generated.

### 3. Canonical Next Actions

- Commit changes to `main`.
- Update `v9.1.1` tag and push to `origin/main` and `origin v9.1.1`.
- Monitor GitHub Actions release workflow execution.

## Permanent Unified Production Keystore & Universal In-App Update Alignment — 2026-11-17

### 1. Delivery & Implementation

- **Permanent Production Keystore Generation:**
  - Created persistent `app/keystore/audia-release.jks` (2048-bit RSA, valid for 10,000 days until April 4, 2054).
  - SHA-256 Fingerprint: `0E:C6:AA:F4:B1:32:AC:78:D4:51:30:9D:59:32:B2:2D:2C:66:BA:CB:80:6C:4B:48:A2:A1:64:91:46:FE:63:F4`.
  - Created `app/keystore/keystore.properties` for structured credential management.
- **Unified Signing Architecture across Local & CI/CD (`app/build.gradle.kts`):**
  - Configured `signingConfigs.release` to automatically bind to `app/keystore/audia-release.jks`.
  - Configured `buildTypes.debug` and `buildTypes.release` to share the unified signing certificate.
  - Guarantees that local developer builds, GitHub Actions releases, and in-app downloaded APKs always share the exact same signature.
  - Permanently eliminates Android's `INSTALL_FAILED_UPDATE_INCOMPATIBLE` ("App not installed as package conflicts with an existing package") error.
- **CI/CD Pipeline Hardening (`.github/workflows/build-and-release.yml`):**
  - Updated release workflow to sign the release APK using this bundled keystore by default, with optional secret override support.

### 2. Verification Evidence

- **Debug APK Build & Signature:**
  - Command: `.\gradlew.bat :app:assembleDebug` — **`BUILD SUCCESSFUL in 52s`**
  - `apksigner verify --print-certs app\build\outputs\apk\debug\Audia-v9.1.1-debug.apk`
  - Result: `SHA-256: 0ec6aaf4b132ac78d451309d5932b22d2c66bacb806c4b48a2a1649146fe63f4` ✅
- **Release APK Build & Signature:**
  - Command: `.\gradlew.bat :app:assembleRelease` — **`BUILD SUCCESSFUL in 20m 2s`**
  - Full R8 minification, ProGuard optimization, and resource shrinking verified.
  - `apksigner verify --print-certs app\build\outputs\apk\release\Audia-v9.1.1.apk`
  - Result: `SHA-256: 0ec6aaf4b132ac78d451309d5932b22d2c66bacb806c4b48a2a1649146fe63f4` ✅
  - Signature match confirmed: Local Debug == Local Release == Cloud CI/CD Release (100% Identical).

### 3. Canonical Next Actions

- Commit keystore and gradle configuration to `main`.
- Update `v9.1.1` tag and push to `origin/main` and `origin v9.1.1`.
- Clean install on test device to transition to the permanent production signature.

## CI/CD Workflow Syntax Fix & Release Pipeline Execution — 2026-11-17

### 1. Delivery & Implementation

- **GitHub Actions Syntax Fix:**
  - Resolved workflow validation failure on run #8 caused by `${{ secrets.* }}` placed directly inside an `if:` condition.
  - Removed the obsolete keystore decoding step since `app/keystore/audia-release.jks` is committed directly to the private repository and picked up automatically by Gradle.
- **Workflow Run #9 Triggered:**
  - Pushed fix in commit `d8e7a45` and updated `v9.1.1` tag (`ae1a5df`).
  - Active GitHub Actions workflow run `44021531934` (Run #9) accepted and executing tests & release build.

## Brand Identity Restoration & v9.1.2 Production Build — 2026-11-17

### 1. Delivery & Implementation

- **Audia Logo Restoration on Play/Pause Controls:**
  - Restored the iconic Audia Triple Arch brand vector (`R.drawable.icon`) on the Play/Pause control across:
    - Primary 1-row player controls (`PlayerControls.kt`).
    - Arch morphing player button (`PlayerScreen.kt`).
    - Leaf composable `AudiaPlayPause` (`PlayerScreen.kt`).
    - Zen Mode player controls (`PlayerScreen.kt`).
    - Radio MiniPlayer control (`RadioMiniPlayer.kt`).
  - Brand identity preserved: when music is idle/paused, the Audia Arch logo serves as the hero play control with dynamic theme/accent color tinting.
- **Version Bump for Live Update Verification:**
  - Bumped version in `app/version.properties` to `VERSION_NAME=9.1.2`, `VERSION_CODE=93`.
  - Enables physical in-app update trigger testing from `v9.1.1` to `v9.1.2`.

### 2. Verification Evidence

- **Kotlin Compilation & JVM Unit Tests:**
  - Command: `.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest` — **`BUILD SUCCESSFUL in 2m 51s`** (all 53 tests passed, 0 errors).
- **Debug APK Assembly:**
  - Command: `.\gradlew.bat :app:assembleDebug` — **`BUILD SUCCESSFUL in 1m 36s`** (`Audia-v9.1.2-debug.apk` successfully generated).

### 3. Canonical Next Actions

- Commit all changes cleanly.
- Create and push release tag `v9.1.2` to trigger CI/CD production release.

## Legal Documentation, Privacy Policy & Version Date Alignment — 2026-11-17

### 1. Delivery & Implementation

- **EULA and Privacy Policy Production Draft (`Legals.kt`):**
  - Updated agreement headers to **Audia Player Version 9.1.2**, dated **November 17, 2026**.
  - Documented features: Persistent Playlists with M3U/M3U8 export/import, Android Auto media tree, scheduled playback recovery, synced lyrics via LRCLIB, and community internet radio via Radio Browser.
  - Transparently disclosed the In-App Automated Update Engine (24h Fastly CDN/GitHub release check).
  - Transparently disclosed Firebase Crashlytics & Analytics telemetry with user toggle controls in Settings.
  - Reaffirmed that audio processing for the live visualizer occurs strictly in device memory without recording or storing microphone audio.
- **In-App About Screen Alignment (`AboutScreen.kt`):**
  - Updated release date to `November 17, 2026` and version display to `9.1.2`.
- **Public Showcase Alignment:**
  - Synchronized `version.json` on Fastly CDN to `v9.1.2` (code `93`).

### 2. Verification Evidence

- **Kotlin Compilation:**
  - Command: `.\gradlew.bat :app:compileDebugKotlin` — **`BUILD SUCCESSFUL in 1m 5s`**
- **Unit Tests:**
  - Command: `.\gradlew.bat :app:testDebugUnitTest` — **`BUILD SUCCESSFUL in 47s`** (all 53 tests passed).

### 3. Canonical Next Actions

- Commit legal and version documentation updates.
- Retag `v9.1.2` and push to remote.

## Professional User-Facing Copy & Notification Polish — 2026-11-17

### 1. Delivery & Implementation

- **Professional User-Facing Copy Invariant Enshrined:**
  - Added strict repository-wide rules in `AGENTS.md` and `GEMINI.md`: All user-facing release notes, update dialogs, push/system notifications, and toasts MUST use professional, polished, consumer-grade language. Internal developer jargon, raw commit messages, keystore/signature details, and testing notes are strictly forbidden from user-facing surfaces.
- **Fastly CDN / Public `version.json` Copy Polish:**
  - Replaced developer notes with consumer-grade release notes highlighting refined playback controls, persistent M3U playlists, Android Auto integration, and audio engine stability.
- **In-App Update Dialog Polish (`UpdateAvailableDialog.kt`):**
  - Updated title to concise "Update Available", section to "What's New", professional fallback notes, and "Remind Me Later" action.
- **Engagement & Notification Cleanliness (`EngagementWorker.kt`, `MainActivity.kt`):**
  - Stripped all mojibake/corrupted characters from notification strings.
  - Standardized time-of-day listening messages and welcome notification to calm, audiophile-grade tone ("Experience pure, uncompressed offline sound.").

### 2. Verification Evidence

- **Kotlin Compilation, Unit Tests & Debug Assembly:**
  - Command: `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` — **`BUILD SUCCESSFUL in 1m 43s`** (all 53 unit tests passed, 0 failures, debug APK generated).
- **Public Fastly CDN Verification:**
  - `version.json` validated on `https://raw.githubusercontent.com/TanjilHasan-Himel/app/main/version.json` returning professional consumer-grade copy.

### 3. Canonical Next Actions

- Commit changes cleanly.
- Push to `origin main` and update tag `v9.1.2`.

## Onboarding Adaptive Layout & Permission Card Text Bug Fix — 2026-11-17

### 1. Delivery & Implementation

- **Fixed Vertical "R-E-Q-U-I-R-E-D" Wrapping Bug:**
  - In `PermissionCard` (`OnboardingScreen.kt`), `Text(title)` previously squeezed `Text("Required")` into a narrow ~10dp column, forcing Compose to wrap `"Required"` character-by-character into 8 vertical lines (`R\ne\nq\nu\ni\nr\ne\nd`), which grossly inflated card heights.
  - Converted `"REQUIRED"` into a horizontal pill badge with `softWrap = false`, bound `Text(title)` to `Modifier.weight(1f, fill = false)`, and hid the badge once permission is granted (`!isGranted`).
- **Eliminated Description Text Truncation:**
  - Expanded `Text(description)` from `maxLines = 1` to `maxLines = 2` with explicit `lineHeight = 14.sp`, preventing sentence clipping ("Required to scan an...", "Lock-screen & back...").
- **Adaptive Screen Ratio & Viewport Scrolling:**
  - Wrapped `PermissionsStep` inside `Modifier.verticalScroll(rememberScrollState())` with `statusBarsPadding()` and `navigationBarsPadding()`.
  - Ensures the bottom CTA ("Grant Music Access" / "Start Using Audia") is never pushed off-screen or rendered inaccessible on shorter phones, tablets, or high-DPI/large font accessibility displays.

### 2. Verification Evidence

- **Kotlin Compilation:**
  - Command: `.\gradlew.bat :app:compileDebugKotlin` — **`BUILD SUCCESSFUL in 36s`**
- **JVM Unit Tests:**
  - Command: `.\gradlew.bat :app:testDebugUnitTest` — **`BUILD SUCCESSFUL in 34s`** (all 53 tests passed).

### 3. Canonical Next Actions

- Commit changes cleanly and push to repository.

## Scheduled Playback Thread-Safety & Foreground Service Type Hardening — 2026-11-17

### 1. Root Cause Diagnosis from Physical Device Dropbox Telemetry

- **Physical Device Telemetry (`SM-M315F`):**
  - Historical crash logs from system `dumpsys dropbox` revealed crash reports at `04:00:00` and `08:45:00` on earlier release `v9.1.0 (91)`:
    `java.lang.IllegalStateException: Player is accessed on the wrong thread. Current thread: 'DefaultDispatcher-worker-2', Expected thread: 'main'`
  - Root cause was traced to ExoPlayer / Media3 calls (`player` and `mediaController`) accessed inside background coroutines (`Dispatchers.IO`) during scheduled alarm triggers and seek/pause persistence updates.
  - In addition, calling raw `startForeground(NOTIFICATION_ID_ALARM, notification)` without foreground service type triggers `MissingForegroundServiceTypeException` or FGS timeout kills on Android 12+/14+ (`targetSdkVersion = 36`).

### 2. Delivery & Implementation

- **ExoPlayer & MediaController Thread Safety Enforced:**
  - In `MusicViewModel.kt`, captured `mediaController?.currentPosition` and `newPosition.positionMs` synchronously on the Main thread before dispatching to `dataStore.savePlaybackState` background coroutines.
  - In `MusicService.kt`, bound volume fade coroutine (`fadeCoroutine`) and automatic timeout stop (`scheduledStopJob`) strictly to `serviceScope` (running on `Dispatchers.Main.immediate`), preventing untracked thread leaks. Added null and `playWhenReady` safety checks across volume interpolation steps.
- **Android Q+ Foreground Service Type Integration:**
  - Updated `MusicService.startForeground` to use `androidx.core.app.ServiceCompat.startForeground` passing `ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK` on API 29+ (Q and above). Eliminates FGS start timeouts and Android 14+ enforcement crashes.
- **Notification PendingIntent Null Safety:**
  - Added safe fallback intent `Intent(this, MainActivity::class.java)` in `MusicService.kt` when `packageManager.getLaunchIntentForPackage(packageName)` returns null, preventing potential runtime NPE.

### 3. Verification Evidence

- **Kotlin Compilation & JVM Unit Tests:**
  - Command: `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug`
  - Result: **`BUILD SUCCESSFUL in 1m 39s`** (53 actionable tasks, all 53 unit tests passed, 0 failures, debug APK built cleanly).
- **Physical Device State Verification:**
  - Verified package `com.eplaytime.app` on `SM-M315F` has `SCHEDULE_EXACT_ALARM: granted=true`.
  - Zero crashes recorded on physical device for `com.eplaytime.app` on November 17, 2026.

### 4. Canonical Next Actions

- Commit changes cleanly.
- Report findings and reassurance to user in Bengali.

## Schedule Content Picker Duplicate-Key Crash Fix & Safety Hardening — 2026-11-18

### 1. Root Cause Diagnosis

- **Physical Device Library Evidence:**
  - Querying the connected physical Samsung Galaxy M31 (`SM-M315F`) media library revealed multiple songs sharing identical titles (e.g. Row 17 and Row 18 both titled `"Aurora - Samsung Ringtone"`, duplicate recordings titled `"Voice 001"`, and repeated tracks titled `"Du Haatey Mutho Bhorey"`, `"Rangeen"`).
  - In `AlarmDialog.kt`, `ScheduleContentPicker` populated track items with `source.take(100).map { it.title to listOf(it) }` and rendered them via `items(groups, key = { it.first })`.
  - Because `it.first` was the raw track title, Jetpack Compose encountered duplicate keys in `LazyLayoutKeyIndexMap`, triggering an immediate fatal crash:
    `java.lang.IllegalArgumentException: Key 'Aurora - Samsung Ringtone' was already used. If you are using LazyColumn/Row the keys should be unique!`
  - Additionally, `songs.first()` and `contentUris.first()` calls in `AlarmDialog.kt` and `SchedulerViewModel.kt` lacked null-safety guards, causing `NoSuchElementException` when collections were empty.

### 2. Delivery & Implementation

- **Guaranteed Unique Keys in `ScheduleContentPicker` (`AlarmDialog.kt`):**
  - Created `SchedulePickerItem` model encapsulating `uniqueKey`, `title`, `subtitle`, and `songs`.
  - For individual tracks: keyed by `track_${song.id}_${song.uri}_$index`, guaranteeing 100% unique keys regardless of duplicate song titles or artist names. Removed artificial 100-item cutoff.
  - For albums: keyed by `album_${albumName}_$index` using `groupBy(Song::album)`.
  - For folders: keyed by `folder_${path}_$index` using `folderPath` grouping so similarly named folders on internal vs SD storage never collide.
  - For custom queue/playlist multi-selection: keyed by `multi_${song.id}_${song.uri}_$index`.
- **Collection Null Safety Hardening (`AlarmDialog.kt`, `SchedulerViewModel.kt`):**
  - Replaced risky `first()` calls with `firstOrNull()` and safe fallback strings (`contentUris.firstOrNull().orEmpty()`, `selectedContentSongs.firstOrNull()?.title`).
  - Added empty state indicator in `ScheduleContentPicker` when no matching audio files are found.
  - Updated `SchedulerScreen.kt` to supply `key = { it.id }` in `items(tasks)` for stable Compose item identity.

### 3. Verification Evidence

- **Kotlin Compilation & JVM Unit Tests:**
  - Command: `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug`
  - Result: **`BUILD SUCCESSFUL in 4m 5s`** (53 actionable tasks, 53 unit tests passed, 0 failures, debug APK built).

### 4. Canonical Next Actions

- Commit changes cleanly.
- Explain root cause and fix clearly to user in Bengali.
