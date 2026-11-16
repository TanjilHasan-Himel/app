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

