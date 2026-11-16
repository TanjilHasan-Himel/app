# Audia Player Stabilization Plan

## Objective

Finish the cover-first adaptive player without trading playback correctness for visual polish.

## Phase 1 — Functional correctness

- [x] Make scan-filter tests exercise the production filter implementation.
- [x] Apply duration and folder changes with the new value instead of a stale DataStore snapshot.
- [x] Detect call recordings and WhatsApp Voice Notes without hiding music in WhatsApp Audio.
- [x] Expose the call/voice-note filter in the Scan Filters sheet.
- [x] Clear unsaved A–B state whenever the active track changes.
- [x] Keep named loop presets stored per song and re-apply a selected preset after changing tracks.
- [x] Add a visible Saved loops entry instead of relying only on long-press.

## Phase 2 — Player interaction

- [x] Add live artwork movement and adjacent-artwork preview during horizontal swipes.
- [x] Keep double-tap seek and progress scrubbing separate from track swipes.
- [x] Add haptic confirmation to the primary play/pause control.
- [x] Give the custom Audia control a persistent surface in both play and pause states.

## Phase 3 — Adaptive presentation

- [x] Let the hero artwork use the full available width.
- [x] Keep metadata, progress, and timestamps on the artwork.
- [x] Give playback and Lyrics/Tools their own visually distinct surfaces.
- [x] Keep Tools vertically scrollable inside the available bottom-panel height.
- [x] Add a visible active-page indicator to Lyrics/Tools.
- [x] Refresh the mini-player artwork and play control to match the main player.

## Verification

- [ ] Production filter unit tests pass, including WhatsApp Voice Notes vs WhatsApp Audio.
- [x] Debug Kotlin compilation passes.
- [x] Existing unit-test suite passes (28 JVM tests).
- [ ] Physical-device checks: play/pause haptic, swipe feedback, slider conflict, compact overflow.
- [ ] Verify known OEM call-recording paths from real devices and add fixtures when a new path appears.

## Intentional boundaries

- Folder detection is pattern-based because Android/OEM call-recording paths are not standardized.
- Saved loops persist; an active unsaved loop never follows the user to a different song.
- No playback-engine, database-schema, or release-version change is part of this work.

## Phase 4 — Scheduled Playback Sessions (locked USP)

Audia scheduling is not an alarm tone. It is durable, exact-time, long-form media playback that
does not require the app process to remain alive while waiting.

- [x] Rename user-facing Alarm terminology to Scheduled Playback / Session.
- [x] Support one track, album, folder, playlist and custom queue snapshots.
- [x] Play the selected content once, stop on queue completion or configured stop limit, then wait
      for the next selected cycle without looping the same session.
- [x] Store next run, active cycle, queue position, completion/failure and last-run result durably.
- [x] Register the next occurrence before starting current playback.
- [x] Reconcile enabled schedules after boot, quick boot, time/timezone changes, app replacement,
      exact-alarm permission changes and normal app launch.
- [x] Restore interrupted scheduled playback after recoverable process death with bounded retries.
- [x] Preserve and restore the user's previous media volume after the scheduled session.
- [x] Keep the waiting state process-free; hold a wake lock only for receiver-to-service hand-off
      and run a foreground media service only while playback is active.
- [x] Surface Ready, Playing, Completed, Needs permission, Missing content and Failed states.
- [ ] Add tests for one-time disabling, queue completion, recovery and time/timezone changes.
- [x] Add deterministic recurrence-calculation tests for one-time and weekday schedules.

Android force-stop, uninstall/clear-data, powered-off devices and revoked permissions remain OS/user
boundaries; the UI must report those conditions instead of claiming an impossible guarantee.

## Phase 5 — Recently Played Audia 3D coverflow (locked identity)

- [x] Show the ten most recent unique tracks as large portrait cards.
- [x] Keep lazy composition as an implementation detail, not a smaller-card visual treatment.
- [x] Restore layered depth, Y-axis rotation, slight Z lean, glass sheen and spring snapping.
- [x] Compose only the focused card and nearby cards; never animate all ten continuously.
- [x] Stop idle motion during touch and disabled system animations/reduced motion.
- [x] Keep 48dp previous/next alternatives and descriptive accessibility semantics.
- [ ] Verify debug and profileable frame traces separately on small/large phones and landscape.
