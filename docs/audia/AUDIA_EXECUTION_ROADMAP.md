# Audia executable roadmap

Date: 2026-11-13. This is the canonical implementation order for future agents. Older audits remain
historical evidence; when status conflicts, verify code/tests and update this file plus QA.

Status values: **Not started**, **In progress**, **Implemented**, **Verified**, **Blocked**.
`Implemented` is not `Verified`: verification requires the acceptance evidence named below.

## Product invariants

1. Scheduled Playback is Audia's reliability USP, not an alarm-tone clone.
2. The Player remains an integrated artwork-first media surface.
3. Named A-B loops remain per-song; unsaved state never follows another song.
4. Radio is real internet radio and owns a queue separate from local music.
5. Local data stays local unless the user explicitly enables an online operation.
6. 3D/glass motion is reserved for signature surfaces; utility UI stays calm and accessible.

## Phase table

| Phase | Scope | Depends on | Status | Exit gate |
|---|---|---|---|---|
| F0 | Truth, compatible stable foundation, security and scheduler recovery | — | In progress | F0 gate |
| A1 | Real Android Auto library and trusted controller contract | F0 | Not started | A1 gate |
| L2 | Persistent playlists and saved queues | F0, A1 media IDs | Not started | L2 gate |
| P3 | Gapless, Smart Fade and ReplayGain | F0 | Not started | P3 gate |
| B4 | Portable Audia backup/export/import | L2 | Not started | B4 gate |
| T5 | Honest tag/artwork editor and Open With | L2 | Not started | T5 gate |
| PERF6 | Macrobenchmark, jank repair and Baseline Profile | F0–T5 stable journeys | Not started | PERF6 gate |
| X7 | Widgets, bookmarks, insights, Cast/NAS and measured custom DSP | beta gates | Not started | feature-specific |

## F0 — Truth and stable foundation

### F0A Toolchain

- Current working environment: AGP 8.9.3, Gradle 9.1.0, JDK 25, compile/target 36.
- Official AGP 8.9 pairing is Gradle 8.11.1, but Gradle 8.11.1 cannot run here with the only
  installed JDK 25. Do not silently leave a non-building wrapper.
- Next toolchain decision is one isolated task: use a project/CI JDK 17 or 21 with Gradle 8.11.1,
  or perform a separately scoped AGP 9/API 37 migration. Do not mix that migration with product work.

### F0B Compatible stable libraries

- [x] Media3 1.3.1 → 1.11.1; compilation passed.
- [x] Room 2.6.1 → 2.8.5; compilation passed.
- [x] WorkManager 2.9.0 → 2.12.0; compilation passed.
- [x] DataStore 1.0.0 → 1.3.0; compilation passed.
- [x] Core 1.12.0 → 1.18.0, Lifecycle 2.7.0 → 2.10.0, Activity 1.8.2 → 1.13.0;
      compilation passed.
- [x] Full JVM suite, lint, debug APK assembly and install/launch smoke test.
- [ ] Local/radio playback, notification controls, audio-route and process-restart device QA.
- API-37-bound Compose 2026.09, Core 1.19.1, Lifecycle 2.11 and AndroidX Hilt 1.4 are intentionally
  deferred to F0A; AAR metadata requires compileSdk 37 and AGP 9.1.

### F0C Persistence and security

- Export Room schemas and add populated v8→v9→v10 migration instrumentation.
- Restrict the custom DSP/session-ID command to Audia. Trusted system/Auto controllers receive only
  standard media/library commands; reject or limit all others.
- Replace global cleartext permission with a scoped radio-stream policy.
- Persist scheduled `activePositionMs`; recovery restores track index and bounded position.
- Merge or remove the duplicate engagement/recommendation workers; reminders must be explicit opt-in.
- Reconcile stale docs against code and keep this roadmap canonical.

### F0B verification record (2026-11-13)

- `:app:testDebugUnitTest` passed (28 JVM tests).
- `:app:assembleDebug` passed; generated APK size was 26,642,667 bytes.
- Initial `:app:lintDebug` exposed 18 Media3 migration errors. The migration now uses
  `SessionError` result codes and explicit `UnstableApi` opt-in; the lint rerun passed.
- The APK was installed in place and cold-launched on Samsung SM-M315F / Android 12. The process
  remained alive and the captured fatal-runtime log was empty.
- Playback behavior, Android Auto, Android 16/17, tablet and performance certification remain
  explicitly unverified.

### F0 gate

- Debug unit tests, lint and APK assembly pass.
- Release compilation is tested; signing remains environment-owned.
- Populated migration preserves songs, favorites, schedules, lyrics and wiki cache.
- Local and radio playback, background notification actions, audio focus/noisy route and process
  restart are exercised on device.
- No destructive migration, unrestricted custom command, or unjustified global cleartext remains.

## A1 — Real Android Auto

Implement stable media IDs and paged roots for Tracks, Albums, Artists, Folders and Favorites.
Add `onGetChildren`, `onGetItem`, search/search results and playable metadata/URI resolution.
Playlists join the tree only after L2 persistence exists; schedules may be a safe read-only category.

### A1 gate

- Media Controller Test App and Desktop Head Unit browse root → category → item and can search,
  play, pause, seek and skip.
- Paging handles at least 5,000 tracks without binder-size failure.
- Missing/deleted items fail gracefully; same-app custom commands are not exposed externally.
- Physical-car validation remains a final matrix item, not a reason to fake completion.

## L2 — Persistent playlists and saved queues

Add `Playlist`, ordered `PlaylistItem`, `SavedQueue` and ordered `SavedQueueItem` entities with an
explicit migration and transactional DAO. Support create, rename, delete confirmation, multi-select
add, reorder, duplicate policy, Play Next/Later and process-death persistence. Import/export M3U/M3U8
through the Storage Access Framework.

Scheduler default: save an immutable queue snapshot. “Update from playlist” is an explicit action;
never silently change tomorrow's scheduled content when a playlist is edited.

### L2 gate

- Names, order and duplicates survive restart/reboot.
- Deleted media is shown unavailable instead of deleting playlist history.
- Scheduler snapshot/reference behavior is deterministic and tested.
- Android Auto exposes real playlists, not scheduler URI snapshots.

## P3 — Playback fundamentals

1. Prove true gapless with consecutive encoded test tones and offload combinations.
2. Add Smart Fade as a separate Off/Gapless/1–8 second mode.
3. Add ReplayGain Off/Track/Album, preamp and clipping prevention; read embedded Track/Album/R128
   metadata first. Optional analysis comes later and never changes system media volume.

### P3 gate

Measured boundary gap meets the declared threshold; no dropped/double playback. Gain mode and album
semantics are deterministic, clipping is prevented, and speaker/wired/Bluetooth/focus regressions pass.

## B4 — Portable Audia backup

Create a versioned `.audia` ZIP/JSON export through the Storage Access Framework. Include playlists,
saved queues, favorites, schedules, named loops, filters and settings. Exclude audio, replaceable
caches and secrets. Add checksums, preview/dry-run, URI relinking, conflict policy and atomic rollback.

### B4 gate

Export → clear app data → import restores counts, names and ordering. Corrupt/newer backups are rejected
without partial writes, and the archive leaks no private path, audio content or network credential.

## T5 — Honest tags, artwork and Open With

Current Tag Studio only changes in-memory song objects; it must not show file-save success. Implement
tested container support through MediaStore write requests/SAF with atomic temp-write, rescan and clear
unsupported/read-only states. Add `ACTION_VIEW`/`ACTION_SEND` for `audio/*` with Play now, Play next,
Add to queue and Add to playlist choices.

### T5 gate

Tags/artwork survive restart and an external reader. Unicode, cancellation, corrupt and read-only files
are tested. Cold/warm intents respect URI grants and never auto-download or auto-play without intent.

## PERF6 — Performance certification

Add a separate `:macrobenchmark` module and profileable target. Measure cold/warm startup, Home scroll,
3D ring fling, Player open, lyrics scroll and large-library scan using `FrameTimingMetric`,
`StartupTimingMetric` and traces. Fix measured invalidation/image/palette work before generating a
Baseline Profile; never use a profile to hide jank.

### PERF6 gate

Declare thresholds before testing. Record release-like results on an older 60 Hz device and modern
90/120 Hz device, plus memory, thermal, reduced-motion and large-font checks. No debug trace is a
release performance certificate.

## X7 — Later, only after beta gates

Widgets and schedule-next widget; per-track bookmarks/resume; local listening insights and opt-in
scrobbling; Cast/UPnP; NAS/WebDAV/Navidrome; then measured/licensed AutoEQ or custom 64-bit DSP.
Do not add unofficial lyric/stream scraping, embedded credentials, social feeds, or 3D/glow on every row.

## Standard phase handoff

Every agent must append to QA and update this roadmap with:

1. Phase/gate and exact scope.
2. Files changed and behavior delivered.
3. Exact commands and test/device results.
4. Explicit `NOT TESTED` cases.
5. Remaining risk and the next executable gate.
