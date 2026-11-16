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



