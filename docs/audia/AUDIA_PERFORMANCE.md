# Audia performance stabilization

## Baseline environment

- Device: Samsung SM-M315F, Android 12 (API 31), 1080x2340, 60 Hz.
- Battery: 81%, 36.8 C. Android thermal status: 0 (no throttling reported).
- Build: debug APK on the same code line. Debug overhead means absolute frame times are not release claims.
- Scenario: 12-second `gfxinfo` windows on the foreground app. The final capture was machine-verified on the Player/Lyrics screen while `Majboor` was playing.

## Baseline methodology and results

Commands used: `dumpsys gfxinfo <package> reset`, fixed observation window, then `dumpsys gfxinfo` and `dumpsys meminfo`. The same device and orientation are used for comparisons.

- Frames: 258 total; 258 janky (100%).
- CPU/UI frame percentiles: P50 97 ms, P90 105 ms, P95 113 ms, P99 125 ms.
- GPU frame percentiles: P50 3 ms, P90 4 ms, P95 5 ms, P99 11 ms.
- Total PSS: 314,062 KB; total RSS: 446,219 KB.
- Graphics PSS: 100,123 KB; reported GPU cache: 46.10 MB.

The baseline screen was not machine-verified before capture, so its before/after deltas are directional rather than a benchmark-grade apples-to-apples result. The GPU numbers were below the 16.7 ms frame budget while UI-thread numbers were not. This points to a CPU/UI-side bottleneck, but a System Trace is still required to attribute individual functions precisely.

## Confirmed hot paths

- Player root collected 100 ms progress plus visualizer bass/energy updates, invalidating a broad composition scope.
- Visualizer fallback allocated boxed `Float` lists about 30 times per second and ran whenever playback was active.
- Home used a large 56 dp blurred offscreen artwork layer.
- Song selection rebuilt every `MediaItem` even when the source library list had not changed.
- Favorites membership used a nested list scan.
- Rescan used delete-then-insert without a Room transaction.

## Changes

- Visualizer waveform snapshots now use `FloatArray`, are capped at 20 fps, and run only while Player UI is visible.
- Bass/energy are collected inside visual leaf composables rather than the Player root.
- Visible progress updates are capped at 250 ms. A-B enforcement retains a separate 20 ms check cadence.
- Unchanged library lists reuse cached `MediaItem` instances.
- Favorites membership uses a `HashSet`.
- Room replacement is atomic and change detection uses `COUNT(*)`.
- Removed the large Home artwork blur; the existing artwork-derived palette gradient remains.
- Replaced the full-screen Lyrics offscreen mask with lightweight top/bottom gradient overlays.
- Removed the visualizer's independent infinite redraw clock; motion now follows sampled audio energy.

## Validation checkpoints

- `:app:testDebugUnitTest :app:assembleDebug`: passed.
- Latest debug APK installed successfully on the physical device.
- Player, artwork, active playback and live lyrics were visually/UI-hierarchy smoke-tested on device.
- Final verified 12-second Player capture: 405 frames; 400 janky by the current metric (98.77%; legacy metric 100%). P50 69 ms, P90 89 ms, P95 93 ms, P99 101 ms.
- Final memory: total PSS 200,749 KB, total RSS 331,435 KB, graphics PSS 45,559 KB.
- Directional delta from the initial capture: P50 -28.9%, P90 -15.2%, P95 -17.7%, P99 -19.2%; PSS -36.1%, RSS -25.7%, graphics PSS -54.5%.
- The jank rate remains unacceptable. This pass reduced frame cost and memory pressure, but does not establish a smooth 60 fps result.

## Remaining work and risks

- Profileable/release-like System Trace, Compose recomposition counters, Macrobenchmark and Baseline Profile are not yet configured. They are required before making release-performance claims.
- A controlled benchmark should automate navigation to the same track, playback position and Lyrics state; the initial ad-hoc baseline cannot guarantee that equivalence.
- Palette extraction is duplicated between Home and Player.
- The legacy unreachable Player branch remains until it can be deleted and visually regression-tested independently.
- Shuffle/repeat, queue-context, A-B persistence/reset and landscape need a complete manual regression matrix; this pass only smoke-tested the active Player path.
