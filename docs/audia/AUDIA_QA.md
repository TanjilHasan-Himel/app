# Audia QA record

## Baseline environment

- Physical device previously connected: Samsung SM-M315F, Android 12/API 31, 1080×2340, 60 Hz.
- Current Stage A evidence: static inspection plus local Gradle build/unit tests. The supplied screenshot is user evidence for Player/Lyrics layout, not a new automated device run.

## Executed

| Scenario | Expected | Actual | Result |
|---|---|---|---|
| Debug unit tests + APK assembly | Tests and compilation succeed | `BUILD SUCCESSFUL` in 2m31s; 50 tasks | Pass |
| Secret heuristic scan | No embedded credentials | No matches in tracked/worktree files excluding build/.git | Pass (heuristic only) |
| Queue code trace | Collection determines queue | All inspected collection taps end at full-library `playSong` | Fail |
| Database upgrade trace | User data preserved | Destructive fallback configured | Fail |
| Provider route trace | Direct client, no developer proxy | LRCLIB/Wikipedia/MusicBrainz direct HTTPS URLs | Pass (static) |

## Required regression matrix

- Playback: all songs, album, artist, folder, favorites, search; next/previous; shuffle/repeat; deleted/unavailable item.
- Lifecycle: Home, lock screen, app switch, recreation, recents removal, notification actions, service stop.
- Audio: speaker, wired, Bluetooth, disconnect, incoming call/audio focus. **NOT TESTED in Stage A.**
- Player: 320/360/393/440dp, compact/tall, 16:9–20:9, gesture/three-button nav, landscape, 1.3× font. **NOT TESTED in Stage A.**
- Lyrics: exact/ambiguous/remix/instrumental/offline/429/wrong-cache replacement and offset persistence.
- Data: Room migration preserving all entities and DataStore loop/settings values.

The static database finding was repaired after this baseline table: explicit v6/v7→v8 migrations now add only the two online-cache tables. An instrumentation migration test and populated-device upgrade remain required before the row can be marked Pass.

After the contextual-queue repair, `:app:testDebugUnitTest` passed in 56s. `PlaybackQueueUnitTest` covers invalid and duplicate URI normalization. Album/folder/artist/favorites/search Next/Previous remains **NOT TESTED on device**.

## MiniPlayer visual phase (UI-002)

Date: 2026-11-12

| Scenario | Expected | Actual | Result |
|---|---|---|---|
| Unit tests + debug assembly | Existing tests and compilation remain green | `:app:testDebugUnitTest :app:assembleDebug` completed successfully in 2m14s | Pass |
| Physical portrait smoke test | MiniPlayer remains readable and operable while paused/playing | Samsung SM-M315F / Android 12 showed the artwork progress ring, glass/accent surface, metadata and play/pause state without a crash | Pass (available-device regression only; not a benchmark) |
| Motion workload inspection | No permanent shimmer; glow work stops while paused | Shimmer is one 650ms track-change animation; liquid phase runs only while playing and is capped at 20 updates/second | Pass (code inspection) |
| Reduced motion | Decorative movement is disabled when system animators are disabled | Guard implemented with `ValueAnimator.areAnimatorsEnabled()` | Implemented; device setting test NOT TESTED |
| Accessibility semantics re-check | Previous, Play/Pause and Next expose accessible names | Controls define content descriptions and 48dp targets; final UIAutomator dump reached Samsung lock/AOD rather than the app | Static pass; device automation BLOCKED by locked screen |
| Responsive matrix | Compact/tall, landscape and 1.3x font remain usable | Not executed in this phase | NOT TESTED |

Installed verification build was 9.0.82 (94). That higher number was used only to allow an in-place debug install; tracked source version was restored to 9.0.77 (89).

## Lyrics and real-radio integration — 2026-11-12

| Scenario | Actual | Result |
|---|---|---|
| Full debug unit suite | 21 tests passed, including lyrics scoring and Radio Browser parsing | Pass |
| Debug compilation/APK | Kotlin compilation passed; debug APK assembled in 4m | Pass |
| LRCLIB live smoke | Official endpoint returned 20 candidates for a known title/artist | Pass (network smoke only) |
| Radio Browser live smoke | Community mirror returned 3 working station records with UUID, resolved URL, codec and bitrate | Pass (directory smoke only) |
| Device install/startup | Final verification build 9.0.84 (96) installed on Samsung SM-M315F; app process started; no startup fatal exception observed | Pass |
| Radio playback/UI | Device remained locked, so station search/play, HLS/progressive audio, favorites/recent persistence and notification controls were not exercised | NOT TESTED |
| Lyrics manual picker | Compiled and covered by scoring tests; interactive selection not exercised on device | NOT TESTED on device |

Known build warnings: Android Gradle Plugin 8.8 is validated only through compileSdk 35 while this project uses 36; deprecated Gradle features remain. These warnings predate this phase and are release-work items.

## Folder MiniPlayer and coverflow repair — 2026-11-12

| Scenario | Actual | Result |
|---|---|---|
| Debug compilation and full unit suite | Folder MiniPlayer/radio wiring and perspective carousel compiled; 21 tests passed | Pass |
| Folder queue trace | Selecting a folder song now builds playback from that folder instead of falling back to an unrelated queue | Pass (static + unit regression) |
| Non-gesture carousel access | 48dp previous/next controls with accessibility labels accompany swipe | Pass (static) |
| Reduced motion | Idle drift is visually disabled and transitions snap when system animators are disabled | Implemented; device setting test NOT TESTED |
| Physical visual/playback regression | No ADB device was connected during this repair | NOT TESTED |

Follow-up physical run on Samsung SM-M315F / Android 12 found the Folder MiniPlayer initially colliding with the three-button navigation area. The Folder bottom bar was updated to reserve `WindowInsets.navigationBars`; verification build 9.0.86 (98) then showed the full MiniPlayer above system navigation with Play/Previous/Next semantics in non-zero bounds. Result: **Pass after repair**.

## Most Played featured carousel — 2026-11-13

| Scenario | Actual | Result |
|---|---|---|
| Compile and full unit suite | Kotlin compilation and 21 unit tests completed successfully | Pass |
| Physical rendering | Featured card, rank, title/artist overlay, indicator and 48dp arrows rendered on Samsung SM-M315F | Pass |
| Horizontal swipe | Rank changed from `#01` to `#02` and content advanced | Pass |
| Arrow navigation | Next arrow advanced from `#02` to `#03` | Pass |
| Card playback | Tapping `#03` started Jhol; MiniPlayer changed to Jhol and exposed Pause | Pass |
| Missing artwork | Final installed APK showed the Audia logo on the `#02` no-art card | Pass |
| Wider responsive matrix | Landscape, 1.3x font and disabled system animators were exercised on the available phone; tablet was not available | Partial pass; tablet NOT TESTED |

## MiniPlayer circle cleanup and DSP foundation — 2026-11-13

| Scenario | Actual | Result |
|---|---|---|
| MiniPlayer artwork treatment | Removed the inactive outer halo and artwork shadow; retained one 2dp gold progress arc around the 58dp artwork | Pass on physical portrait capture |
| Landscape | MiniPlayer stayed fully above the side navigation area with controls and metadata visible | Pass on Samsung SM-M315F / Android 12 |
| Enlarged font | At 1.3x font scale metadata ellipsized without overlapping playback controls | Pass on available phone |
| Reduced motion | With all three system animation scales set to 0, cold-start dismissal, Home and MiniPlayer remained usable; process stayed alive | Pass on available phone |
| Device restoration | Font scale, animation scales, auto-rotation and rotation were returned to their original values and read back | Pass |
| DSP unit prototype | Disabled bypass, flat identity, +6dB/1kHz response and finite-output checks passed | Pass |
| Full debug suite + APK | `:app:testDebugUnitTest :app:assembleDebug` completed successfully; verification APK 9.0.90 (102) installed and launched | Pass |
| Playback DSP integration | Isolated biquad is not connected to the player; OEM Android effects remain the production path | NOT IMPLEMENTED by design |
| Audio measurements | THD+N, clipping headroom, end-to-end latency, CPU, thermal and battery measurements were not run | NOT TESTED |

DSP rationale, licensing constraints and staged measurement gates are recorded in `AUDIA_DSP_FEASIBILITY.md`. JamesDSP was not embedded because the current candidates introduce GPL/system-capture constraints that require a separate product and licensing decision.

## DSP state and MiniPlayer progress repair — 2026-11-13

| Scenario | Actual | Result |
|---|---|---|
| EQ fader geometry | Knob offset now uses the track center; top, center and bottom map to +max, 0 and -min without the previous double Y offset | Pass; focused geometry test + physical rendering |
| Session re-attachment | Bass, virtualizer and EQ desired values are retained, clamped and reapplied whenever Android creates a new audio session | Pass; code path + active-session device log |
| Device DSP sheet | Five device-reported bands rendered at 60Hz, 230Hz, 910Hz, 3.6kHz and 14kHz; session 180953 remained initialized | Pass on Samsung SM-M315F / Android 12 |
| MiniPlayer progress | Removed the separate artwork progress ring; elapsed playback now adds a left-to-right ambient fill and 2dp gold progress edge across the glass player | Pass on physical playing-state capture |
| Regression suite | Debug unit tests and APK assembly completed successfully | Pass |

The production path remains Android's session-bound Equalizer/BassBoost/Virtualizer. The isolated Audia biquad prototype is still intentionally disconnected until measured playback integration is approved.

## Recently Played lazy coverflow — 2026-11-13

| Scenario | Actual | Result |
|---|---|---|
| Rendering architecture | Replaced the eager 15-card custom ring and per-frame coroutine/idle animation with lazy `HorizontalPager`; only the viewport and one neighbour are composed | Pass |
| Motion and accessibility | Native fling/snap, 48dp previous/next targets, card focus on side-card tap, and system reduced-motion guard retained | Pass |
| Compile and regression | Debug Kotlin compilation, 25 JVM tests and debug APK assembly completed successfully | Pass |
| Physical install/startup | Final debug APK installed and launched on Samsung SM-M315F / Android 12; Home, Recently Played and MiniPlayer rendered | Pass |
| Physical debug frame trace | Six scripted carousel swipes: 194 frames, 72.16% missed deadlines, p50 44ms, p90 77ms; GPU p50 4ms/p99 16ms | Improved tail/GPU, but NOT smooth enough |

The trace identifies remaining UI-thread work rather than GPU saturation. A release/profileable Macrobenchmark on modern 60/90/120Hz devices is still required; this debug result must not be presented as a passing performance benchmark.

## Scheduled Playback Sessions and final coverflow verification — 2026-11-13

| Scenario | Actual | Result |
|---|---|---|
| Build and JVM regression | `:app:assembleDebug :app:testDebugUnitTest` completed successfully; 28 JVM tests are green | Pass |
| Recurrence calculation | One-time future/past and selected-weekday occurrence cases are deterministic unit tests | Pass |
| In-place database migration | Existing physical install was upgraded through schema v9 to v10; app process started with PID 12053 and no Room/SQLite/FATAL migration error was present | Pass on Samsung SM-M315F / Android 12 |
| Platform configuration | Installed build reports versionCode 90, minSdk 24, targetSdk 36; exact-alarm special access is granted | Pass on available device |
| Scheduled playback end-to-end | Exact trigger, multi-hour completion, volume restoration, reboot/timezone recovery, permission revocation and bounded crash recovery were not run as a timed physical matrix | NOT TESTED |
| Final ten-card coverflow performance | Large-card 3D styling compiled, but a new release/profileable frame trace was not captured after the final visual adjustment | NOT TESTED; previous debug trace remains a known performance risk |
| Responsive/visual matrix | The device was locked/AOD during final verification, so final scheduler and coverflow visuals, landscape, tablet, enlarged font and reduced motion were not inspected | BLOCKED by device state |

The scheduler intentionally uses process-free exact alarm registration while waiting and a foreground media service only during active playback. Android force-stop, device power-off at the trigger time, uninstall/clear-data and revoked special access remain explicit operating-system or user boundaries rather than guarantees the app can bypass.

## Endless 3D Recently Played ring — 2026-11-13

| Scenario | Actual | Result |
|---|---|---|
| Geometry | Replaced the finite pager with an unbounded ten-cover ring; covers travel front → side → back → front without a first/last edge | Pass on physical rendering |
| Drag and settle | A physical ADB swipe changed the focused cover and settled the ring; process remained alive with no fatal runtime log | Pass on Samsung SM-M315F / Android 12 |
| Depth treatment | Focused cover is full-size; side/back covers use continuous orbit position, scale, alpha, Y rotation and Z ordering | Pass on physical screenshot |
| Alternatives and motion preference | 48dp endless previous/next controls remain; Android disabled-animator state removes spring and 3D rotation | Implemented; reduced-motion physical re-test pending |
| Regression | Debug compilation, APK assembly and all 28 JVM tests completed with zero failures | Pass |
| Performance | No release/profileable frame trace was captured for this final ring implementation | NOT TESTED |

The native implementation adapts Bencho's MIT-licensed interaction geometry without bundling its React/CSS code or photographs. Attribution is recorded in `THIRD_PARTY_NOTICES.md`.

## F0 compatible stable dependency foundation — 2026-11-13

| Scenario | Actual | Result |
|---|---|---|
| Stable compatibility batch | Media3 1.11.1, Room 2.8.5, WorkManager 2.12.0, DataStore 1.3.0, Core 1.18.0, Lifecycle 2.10.0 and Activity 1.13.0 resolved and compiled | Pass |
| JVM regression | `:app:testDebugUnitTest` completed; 28 tests passed | Pass |
| Debug APK | `:app:assembleDebug` produced a 26,642,667-byte APK | Pass |
| Lint migration repair | First run found 18 Media3 result-code/opt-in errors; constants and opt-in scope were corrected, then `:app:lintDebug` passed | Pass after repair |
| Install and cold launch | Installed in place on Samsung SM-M315F / Android 12; package reports version 9.0.78 (90), target 36; PID 18337 remained alive and no fatal runtime line was captured | Pass (smoke only) |
| Playback/service regression | Local/radio audio, notification actions, focus/noisy route, DSP attachment and process-death recovery were not exercised in this smoke run | NOT TESTED |
| Platform/device matrix | Android Auto/DHU, Android 16/17, tablet, modern 90/120 Hz hardware and release/profileable performance were not exercised | NOT TESTED |

The newest API-37-bound Compose/Core/Lifecycle/Hilt releases are intentionally outside this batch.
They require an isolated compileSdk 37 / AGP 9.1 toolchain migration; mixing that migration into the
playback-library update would make failures harder to isolate.

## Queue screen Compose crash repair — 2026-11-14

| Scenario | Actual | Result |
|---|---|---|
| User crash evidence | Physical crash buffer showed `NoSuchMethodError` from `org.burnoutcrew.reorderable` calling removed Compose `animateItemPlacement` while Queue composed | Root cause confirmed |
| Compatibility repair | Removed the obsolete reorderable dependency and its Queue wrapper; retained ordering through accessible Move Earlier/Move Later menu actions backed by the existing MediaController move API | Pass |
| Automated regression | `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug` completed successfully | Pass |
| Physical Queue navigation | Updated APK installed on Samsung SM-M315F / Android 12; Player → Tools → Queue rendered a non-empty queue, PID 23819 remained alive and crash buffer was empty | Pass |
| Physical reorder | Move Later swapped the first two upcoming tracks (`Alfaaz` moved after `Alice In Chains…`) and the queue remained open | Pass |
| Exact folder reselection | The original crash log came from the user's folder-play path; the repaired physical run reused the already populated non-empty playback queue rather than selecting that same folder again | NOT RETESTED exactly |

## Small-beta script

Ask each of 3–4 testers to record device/Android version, then complete: permission flow, search a song, open an album and choose a non-first track, next/previous, seek, hero swipe, lyrics, Tools, speed, A-B save/apply, sleep timer, equalizer, background notification control and headphone disconnect. Record completion, confusion, failed taps, layout clipping and exact reproduction steps. This is qualitative feedback, not statistically representative research.

