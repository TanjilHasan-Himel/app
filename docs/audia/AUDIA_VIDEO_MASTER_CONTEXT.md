# Audia Player — master context for an AI-made product video

**Snapshot:** 14 November 2026 (Asia/Dhaka)  
**Project:** Native Android app, application ID `com.eplaytime.app`  
**Audience for this file:** video writer, narrator, editor, motion designer, and any AI video tool.  
**Truth rule:** `Implemented` means code exists; `tested` means the cited scenario actually ran. Never turn an item in the roadmap into a shipped feature.

## 1. One-minute understanding

Audia is a local-first Android music player built around an immersive artwork-first Player and scheduled listening. It scans audio already on the phone, plays it through Android Media3, supports contextual queues, lyrics, per-song A–B loops, filters, audio effects and a mini player. It also has an internet-radio section using real station streams. The product's central idea is that a listener can choose music, a folder, an album or a queue for a future time, let it play for the selected session, and return to the next scheduled cycle. Examples include a morning Quran recitation, a long lecture, or an evening music session.

Audia is **not yet a finished, fully certified release**. Several important implementations have only partial device coverage, and saved playlists, Android Auto browsing, ReplayGain, true gapless validation and backup remain future work. A video may present the vision and working prototype, provided the narration distinguishes them.

## 2. Origin and product story

The exact first commit date, founder interview, team size, funding, download count and release history are **not established by the inspected project evidence**. Do not invent them. The documented evolution is:

1. A native local music player with device audio scanning, favorites, playback, a mini player and an expressive visual identity.
2. An immersive Player design: a portrait artwork surface with the header, song title, artist, progress slider and times integrated on the artwork. Playback controls adapt below it on taller screens or within its lower area on compact screens. The user explicitly rejected a conventional separated album-art-and-details page.
3. Scheduled Playback grew into the product's strongest differentiator: content selection, repeat days, target volume, optional fade, optional stop duration, run status and reboot/time-change rescheduling.
4. Repeated feedback shaped the mini player, 3D Recently Played/Most Played presentation, lyrics, queue context, radio search, audio filters and A–B loop saving.
5. Audit and stabilization exposed gaps. The project added explicit Room migrations, more deterministic queue construction, lyrics candidate matching, real Radio Browser discovery, performance fixes and a phased release roadmap.
6. On 13 November 2026, a compatible library update brought Media3 to 1.11.1, Room to 2.8.5, WorkManager to 2.12.0 and DataStore to 1.3.0. JVM tests, debug assembly, lint and a Samsung Android 12 launch smoke test passed.
7. On 14 November 2026, a real Queue crash was captured on device. An obsolete Compose reorder dependency caused `NoSuchMethodError`. It was removed; accessible Move Earlier/Move Later actions kept queue ordering. The repaired Queue screen and one reorder were tested on the phone.

This is a story of an opinionated product being refined through user feedback and physical-device evidence. It is not a claim that every phase has shipped.

## 3. Product requirements document (PRD / PDR)

### Problem

Most local players handle immediate playback well, but Audia's owner wants music that can start as a deliberate future listening session and continue for the actual selected content, including long Quran recitations, audiobooks, lectures and mixes. They also want expressive artwork, useful control over local files, and a mini player that feels connected to playback without burying utility actions.

### Users and primary jobs

| User/job | Needed outcome | Audia response |
|---|---|---|
| Listener with on-device music | Find and play a track in its album/folder/artist context | MediaStore library, browse/search, contextual Media3 queue |
| Listener scheduling long content | Start a chosen track/collection at a future time, then stop at completion or a limit | Room-backed scheduled task, AlarmManager, receiver, foreground playback service |
| Listener replaying a passage | Return to a chosen segment in one song | Live A–B loop and multiple named per-song presets |
| Listener with noisy device library | Exclude short clips, call recordings, voice notes or selected folders | Duration, type and folder filters |
| Listener wanting lyrics | View timed or plain words and correct a bad match | Local `.lrc`, cache, LRCLIB exact/search, candidate picker |
| Listener wanting real broadcasts | Find and play internet stations | Radio Browser directory and direct station streams |

### Product principles and decisions

- Keep the integrated artwork-first Player. Its metadata and progress belong on the hero artwork.
- Playback belongs to one service-owned ExoPlayer; the UI talks through a MediaController.
- Scheduled Playback is the flagship promise, but Android/OEM limits must be stated honestly.
- Saved A–B loops belong to the song URI; unsaved loop state should not leak to the next song.
- Local listening data stays on-device unless the user chooses an online feature.
- Repeat, shuffle and speed communicate their state in the control itself; transient messages are reserved for outcomes that are otherwise not visible.
- Fix correctness and evidence gaps before multiplying features.

### Scope at this snapshot

**Present in code:** local scanning, tracks/albums/artists/folders, search, favorites, current queue, contextual playback, shuffle/repeat/speed, immersive Player, mini player, lyrics, A–B presets, sleep timer, Android session-based EQ/BassBoost/Virtualizer, scheduled sessions, radio directory/streaming, Song Wiki, filters and 3D home carousels. Their verification levels vary; see section 11.

**Planned or incomplete:** persisted playlists and multiple saved queues, real Android Auto browse tree, reliable file tag writing, true measured gapless playback, ReplayGain, smart fade, one-file backup/import, Macrobenchmark/Baseline Profile, widgets, Cast/NAS and production custom DSP.

### Success criteria

The product should let a user finish its core journeys without losing the queue, misidentifying a song, missing a schedule silently, crashing on Queue, or showing controls clipped by screen size. Release claims require device tests for playback, scheduling, recovery, audio routes, migrations and responsive layouts; performance claims require release-like measurement rather than debug impressions.

## 4. Current user flows

### A. Local playback

`Grant audio permission → scan Android MediaStore → apply stored filters → Home/Library browse or search → choose a track → build the queue from that visible collection → service plays → mini player/immersive Player → Lyrics or Tools → Queue/Next/Previous`.

Folder, album, artist and search selection call `playSongInQueue(collection, selectedUri)`. The Queue screen reflects the active MediaController queue. It currently supports playback selection, Play Next/Later, remove, de-duplicate, auto-clean and Move Earlier/Later. This **active queue is not a named, persistent saved queue**.

### B. Scheduled listening

`Choose a track/album/folder/queue snapshot → select date/time or repeat days → configure volume/fade/optional stop duration → save Room task → AlarmManager registers next occurrence → AlarmReceiver validates task and starts MusicService → ExoPlayer plays selected URIs → service records result/restores volume → recurring task schedules its next cycle`.

Boot, time, timezone, app-update and exact-alarm permission broadcasts initiate rescheduling work. The scheduler stores a current cycle ID, track index, recovery attempts, last result and next run time. Recovery currently restores a track index but **not the elapsed position within that track**. Full timed tests across Android versions and OEM battery policies are still required. A force-stopped app or powered-off device cannot be guaranteed to play on time.

### C. Lyrics

Open Player → check song-local/cache data → load adjacent `.lrc` if present → query LRCLIB by exact metadata → confidence-ranked search → auto-use a strong candidate or let the user select → cache by song URI → show timed/plain lyrics.

The client handles request cancellation/deduplication and LRCLIB `429 Retry-After`. It does not bundle a licensed commercial lyrics catalog. A provider result can be wrong; candidate selection and refresh exist to correct it.

### D. A–B loop

`Play song → mark A → mark B → repeat that passage → optionally save it under a name → select a named preset for that same song later`. Presets are stored in Preferences DataStore keyed by song URI. Live state is reset on track changes; the user should not see one song's marks on a different song.

### E. Radio

`Open Radio → browse popular stations or search by station name/tag/country/language → inspect station details → confirm mobile-data use if required → stream the station directly → favorite or revisit recent station`. Radio Browser provides directory data and station URLs. Audia's local queue and radio state are handled separately in the UI. Broken streams can still exist in a community directory; live playback/reconnect has not passed a broad device/network matrix.

## 5. UX and visual design

- **Player:** portrait-oriented hero artwork. Minimize, NOW PLAYING, overflow, song/artist, slider and current/remaining times overlay the artwork. Controls adapt to available height. Lyrics and Tools occupy a supporting lower area. Landscape/tablet is a planned left/middle/right adaptation, not a completed certification.
- **MiniPlayer:** one glass-like surface with artwork, metadata, previous/play/next and playback progress along the surface. Its styling uses subtle ambient tint, shimmer on track transitions and a playing-state glow; reduced-motion behavior is considered.
- **Home:** Recently Played has an endless ten-cover 3D ring; Most Played has a featured card/carousel; Recently Added and library entry points remain. The presentation is meant to feel tactile without making every utility row animated.
- **Typography:** the current Compose theme requests Google Fonts **Outfit**, **Galada** and **Oswald**. Outfit is the main UI family; Galada is used for expressive headings. Actual font delivery can depend on Google Play services/network and fallback behavior.
- **Controls:** Audia logo/arch identity, dynamic accent colors and haptic play/pause feedback. Repeat/shuffle/speed should expose their changed state locally.
- **Accessibility intent:** labeled controls, 48 dp navigation targets in carousels, alternate buttons for gesture-only motion and reduced animation. A complete TalkBack/large-font/tablet matrix remains open.

## 6. Architecture

```text
Android MediaStore ──> MusicRepository ──> Room songs/cache ──> MusicViewModel ──> Compose UI
                                                    │                  │
Preferences DataStore ──────────────────────────────┘                  │
                                                                       ▼
                                                               MediaController
                                                                       │
                                                                       ▼
                                                         MusicService + MediaLibrarySession
                                                                       │
                                                                       ▼
                                                                 one ExoPlayer

Room scheduled tasks ──> AlarmScheduler ──> AlarmReceiver ──> MusicService
                             ▲                  ▲
                             └── BootReceiver / reschedule worker

LRCLIB / Wikipedia / MusicBrainz / Radio Browser ──> direct network clients ──> repository/UI
```

**Presentation:** `MainActivity` hosts a Jetpack Compose `NavHost` with Home, Player, Queue, Folder, Scheduler, Radio and About routes. `MusicViewModel`, `SchedulerViewModel` and `RadioViewModel` expose observable state. Most UI talks to the shared music ViewModel. This works, although `MusicViewModel` is oversized and currently combines playback, scan, lyrics, settings, timers and effects.

**Playback:** `MusicService` is a `MediaLibraryService` with one Media3 `ExoPlayer` and `MediaLibrarySession`. The UI connects asynchronously through a MediaController. The service supplies media notifications and background playback. It sets music audio attributes, audio focus handling, becoming-noisy behavior and a local wake mode. The library root exists, but `onGetChildren()` returns an empty list, so Android Auto browsing is not ready.

**Persistence:** Android MediaStore is the source of on-device audio metadata. Room schema **v10** stores songs, favorites, scheduled tasks, lyric cache and Song Wiki cache. Migrations cover v6/v7→v8, v8→v9 and v9→v10; exported schemas and populated migration instrumentation are still pending. Preferences DataStore stores user/playback/filter/loop preferences. Radio favorites and recents currently use a dedicated private `SharedPreferences` file. These stores are local to the app.

**Dependency injection and concurrency:** Hilt provides database/DAOs and ViewModels. Kotlin coroutines and `StateFlow` drive asynchronous loading and UI state. WorkManager handles rescheduling and engagement work. AlarmManager registers future playback triggers without keeping a service alive for the whole waiting period.

## 7. Technology stack and choice rationale

| Area | Current choice | Why it fits | Caveat |
|---|---|---|---|
| Language/toolchain | Kotlin 2.1.0, Java 17 bytecode, Gradle wrapper 9.1.0, AGP 8.9.3, KSP | Native Android APIs, coroutines and compile-time generated Room/Hilt code | AGP 8.9.3/Gradle 9.1 is not the official pairing; toolchain alignment is a separate task |
| Platform | minSdk 24; compile/target SDK 36 | Broad Android device coverage while targeting modern behavior | Android 16/17 physical matrix remains open |
| UI | Jetpack Compose, Material 3, Compose BOM 2024.09.00 | Adaptive declarative UI and custom artwork/gesture surfaces | UI jank and compatibility must be measured per screen |
| Playback | Media3 ExoPlayer/session/HLS 1.11.1 | One playback engine, background session, notifications, local and internet streams | Gapless/offload and route behavior require measured validation |
| Structured data | Room 2.8.5 | Queryable, migratable local entities | Migration instrumentation and exported schema missing |
| Preferences | DataStore 1.3.0 | Async persistence for settings, last playback and named loops | One-file portable backup is not implemented |
| Scheduled work | AlarmManager + receivers; WorkManager 2.12.0 for rescheduling | Precise future trigger plus persistent background maintenance | OEM battery limits, exact-alarm access and newer OS restrictions require tests |
| Injection | Hilt 2.55 + KSP | Shared dependency graph without manual wiring | ViewModel size still needs architectural cleanup |
| Images/visuals | Coil 2.6.0, Palette, Lottie, Compose drawing | Album art loading, ambient colors and signature motion | Debug frame measurements still show jank |
| Network | `HttpURLConnection` clients over HTTPS for directory/metadata | Direct client access without Audia-operated backend or secret API keys | Provider availability, terms and cache rights need release review |
| Tests | JUnit, AndroidX test/Espresso, Compose UI test dependencies | Fast unit tests plus available device-test framework | Real instrumentation suite is currently thin |

These are **current implementation reasons**, not a claim that all choices are optimal forever. The AndroidX versions are sourced from `app/build.gradle.kts` and `gradle/libs.versions.toml` at this snapshot.

## 8. Internet, data and privacy

| Destination/source | What Audia uses it for | What leaves the device | Local playback dependency? |
|---|---|---|---|
| Android MediaStore | Reads audio files/metadata already on the phone | Nothing to an internet server | Yes, for local library discovery |
| `lrclib.net` | Exact/search lyrics lookup | Song title, artist, album and duration used as query metadata | No |
| `en.wikipedia.org` | Song/artist summary | Search terms for selected song/artist | No |
| `musicbrainz.org` | Recording/release metadata | Song/artist query | No |
| Radio Browser community API (`*.api.radio-browser.info`) | Station discovery/search, working flags and click registration | Search term or station UUID, plus ordinary network metadata | Only for radio discovery |
| Individual radio-station hosts | Actual live audio stream | Connection to the selected station; host can see ordinary connection metadata | Only for radio playback |
| Google Fonts provider on the device | Outfit/Galada/Oswald font retrieval | Depends on Google Play services/font cache | No |

There is no Audia-operated account backend or cloud library sync in the inspected architecture. Local audio is not uploaded for ordinary playback. Network availability is necessary for uncached online lyrics, Song Wiki and radio. Radio may use mobile data; the UI checks metered connection and offers a warning/choice.

**Current privacy/security state:** the manifest requests audio read permission, internet/network state, notification permission, exact alarm access, foreground media playback, boot completed, wake lock, audio setting and microphone/visualizer-related permission. Android backups are disabled in the manifest. Release signing uses environment-provided credentials rather than a hardcoded keystore. Room no longer uses destructive migration fallback.

**Open security work:** the exported media service currently grants its custom audio-session command broadly; controller trust must be restricted. The manifest currently allows cleartext traffic globally to accommodate HTTP radio streams; this needs a scoped policy. The online providers' distribution/display/cache terms need confirmation before public release. Do not narrate the app as security-audited or fully private merely because local playback is local.

## 9. Engineering challenges and actual responses

| Challenge | Implemented response | Remaining work |
|---|---|---|
| Folder/album context was lost when playing a song | Collection-aware `playSongInQueue` builds Media3 items from the chosen collection | Full physical Next/Previous matrix |
| Scheduled music must wait without draining battery | AlarmManager registration, receiver trigger, foreground service only during active playback, boot/time rescheduling | Exact multi-hour, reboot/OEM/API 37 tests; restore exact elapsed position |
| App upgrade must preserve user data | Explicit Room migrations replaced destructive fallback | Export schemas and populated migration tests |
| Wrong lyric matches / provider throttling | Exact lookup, confidence-ranked search, manual picker, URI-owned cache, cancellation and Retry-After handling | Broader provider quality/device checks and terms review |
| Internet stations can be dead or use varied formats | Radio Browser working flags/mirrors, direct streams, fallback/retry handling, Media3 HLS module | HLS/progressive, network-switch and reconnect physical matrix |
| EQ may attach to an obsolete audio session | Session ID reattachment and device-reported band geometry | Audible hardware and audio-quality measurements |
| Animated UI stressed an older 60 Hz phone | Reduced redraw/allocation, lighter gradients, cached MediaItems, revised carousels | Release/profileable trace; current debug jank remains high |
| Queue crashed after the dependency update | Removed obsolete reorder library; moved ordering actions to item menu | Exact original folder re-selection sequence not rerun after fix |
| Third-party carousel concept had a license | Native Compose geometry was built with Bencho MIT attribution | Keep notice with future distribution |

The isolated Audia biquad math prototype passes JVM checks but is **not connected to playback**. Current production effects use Android's session-based Equalizer, BassBoost and Virtualizer. Do not claim JamesDSP integration, AutoEQ, lossless DSP or audiophile measurements.

## 10. Product management and delivery process

The product is managed through feedback, a canonical phased roadmap, explicit product decisions, a feature/task backlog, checkpoint notes and an evidence-based QA log. The owner supplies visual references and concrete interaction feedback; engineering translates these into invariants, implementation tasks and acceptance gates. A feature is recorded as **Implemented** when the code exists and **Verified** only when the named tests/device scenarios have run.

The current delivery order is:

1. **F0 foundation:** toolchain decision, migration tests, service/controller security, network transport scope and scheduled recovery.
2. **A1 Android Auto:** real browse tree/search/play with stable media IDs and pagination.
3. **L2 library management:** persisted playlists and multiple named saved queues, plus scheduler snapshot semantics.
4. **P3 playback fundamentals:** measured gapless, configurable fade and ReplayGain modes.
5. **B4 backup:** a versioned, portable Audia export/import file.
6. **T5 file management:** honest tag/artwork editing and Android Open With/share audio.
7. **PERF6:** Macrobenchmark, trace-driven jank fixes and Baseline Profile.
8. **X7 later:** widgets, bookmarks, insights, scrobbling, Cast/UPnP, NAS/Navidrome and measured custom DSP.

The primary product trade-off is to protect the scheduler/loop/immersive identity while improving reliability. Feature count alone is not the target. A future agent is instructed by root `AGENTS.md` to read the roadmap, preserve the dirty worktree, resume the first unverified gate and record exact evidence.

## 11. Evidence and honest status as of this snapshot

| Topic | Evidence | Safe video wording |
|---|---|---|
| Android build | 28 JVM tests, debug APK assembly and lint passed after the compatible dependency update; Samsung SM-M315F/Android 12 install/launch smoke passed | “The current Android prototype builds and launches on a tested phone.” |
| Queue crash fix | Device crash log identified `animateItemPlacement` binary mismatch; repaired APK opened a populated Queue and moved one upcoming song with no new crash | “A real Queue crash was traced and repaired.” |
| Scheduler | Recurrence unit tests and an in-place v9→v10 device upgrade passed | “Scheduled sessions are implemented; long-running and newer-Android reliability is still being validated.” |
| Radio | Live directory smoke and code implementation exist | “Audia can discover real stations and is undergoing stream/device regression tests.” |
| Lyrics | LRCLIB candidate scoring tests and live API lookup smoke exist | “Audia supports local/cached and online lyrics with correction options.” |
| Carousels | Phone interaction/visual checks passed for featured and endless ring variants | “The home interface uses a custom depth carousel.” Do not claim 60/120 fps. |
| Performance | Older debug traces showed significant UI-thread jank, even after improvements | “Performance work is ongoing.” |
| Android Auto | Exported MediaLibraryService exists, but browse children are empty | “Android Auto browsing is on the roadmap.” |
| Playlists/saved queues | No persisted playlist/saved-queue entities | “Planned.” |
| Tag editing | Tag Studio changes in-memory data and currently shows a success message | “A file-safe tag editor is planned.” |
| Gapless/ReplayGain | No measured complete implementation | “Planned and subject to audio tests.” |
| Custom DSP | Isolated biquad prototype only | “Research prototype; current effects use Android's audio effects.” |

**Not tested/certified:** physical Android 16/17, tablet, Android Auto head unit/car, long scheduled sessions, complete audio route/focus matrix, laboratory audio quality, sustained battery/thermal behavior, release-like Macrobenchmark and Play Store analytics/reviews.

## 12. Suggested video structure

1. **Opening (problem):** “What if a music player could be ready for a listening session at the time you choose?” Show a morning routine or a planned long recitation. Do not promise an unstoppable alarm.
2. **Identity (visual):** show the integrated hero cover, expressive typography, artwork-based color and mini player. Use real Audia captures or clearly label concept footage.
3. **Daily flow:** local scan → browse a folder → play a track → mini player → Player → lyrics/tools → active queue.
4. **Signature feature:** schedule a track or collection, set recurrence/volume/stop limit, show next-session status. Explain how AlarmManager wakes the service at run time.
5. **Personal control:** named A–B passage, filters, repeat/shuffle/speed, Android session effects.
6. **Optional internet:** LRCLIB lyrics and real radio, naming that station audio comes from the selected broadcaster.
7. **Engineering story:** one service-owned player, local database, migrations, direct provider clients, the measured performance work and the Queue crash repair.
8. **Honest close:** roadmap for Android Auto, saved playlists, backup, gapless/ReplayGain and full beta validation. Invite testing without implying public release readiness.

### Narration guardrails

- Say “scheduled listening” or “scheduled playback,” not “guaranteed alarm replacement.”
- Say “real internet radio,” not “FM radio hardware.”
- Say “Android audio effects,” not “custom 64-bit DSP” or “JamesDSP powered.”
- Say “current playback queue,” not “multiple saved playlists.”
- Do not show Android Auto, Chromecast, backup or ReplayGain as existing screens/features.
- Do not claim zero battery drain, bit-perfect output, flawless performance or broad device compatibility.
- Do not invent launch date, active users, awards, ratings, revenue or partners.
- Obtain rights to any album art, song audio, lyrics, station logos or reference imagery used in the final video. A UI screenshot does not automatically license the music/artwork displayed in it.

## 13. Source map for fact checking

- Canonical plan: `docs/audia/AUDIA_EXECUTION_ROADMAP.md`.
- Product decisions: `docs/audia/AUDIA_DECISIONS.md`.
- Executed QA and device evidence: `docs/audia/AUDIA_QA.md`.
- Current handoff: `docs/audia/AUDIA_CHECKPOINT.md` and root `AGENTS.md`.
- Current dependencies/platform: `app/build.gradle.kts`, `gradle/libs.versions.toml`, `app/src/main/AndroidManifest.xml`.
- Playback and Auto limitation: `app/src/main/java/com/eplaytime/app/service/MusicService.kt`.
- UI routes and flows: `MainActivity.kt`, `HomeScreen.kt`, `PlayerScreen.kt`, `QueueScreen.kt`, `SchedulerScreen.kt`, `RadioScreen.kt`.
- Local library and filter: `MusicRepository.kt`, `MusicScanFilter.kt`.
- Scheduler: `ScheduledTask.kt`, `AlarmScheduler.kt`, `AlarmReceiver.kt`, `BootReceiver.kt`, `BootRescheduleWorker.kt`.
- Local persistence: `PlayTimeDatabase.kt`, `DatabaseModule.kt`, `PlayTimeDataStore.kt`, `RadioRepository.kt`.
- Online sources: `LrcLibClient.kt`, `SongWikiClient.kt`, `RadioBrowserClient.kt`.
- DSP/performance boundaries: `docs/audia/AUDIA_DSP_FEASIBILITY.md`, `docs/audia/AUDIA_PERFORMANCE.md`.
- Third-party carousel attribution: `THIRD_PARTY_NOTICES.md`.

## 14. Copy-ready context prompt for the video AI

> Create a documentary-style product video about Audia, a native Android local music player whose signature idea is scheduled listening. Use the facts, flows, feature status and claim limits in this document. Show the immersive cover-first Player with title/artist/progress over artwork; the glass mini player; local folder playback and active queue; a future scheduled session; per-song named A–B loops; lyrics; and optional real internet radio. Explain the architecture simply: Jetpack Compose UI, ViewModels, MediaController, one service-owned Media3 ExoPlayer, Room/DataStore, AlarmManager/receivers, and direct network clients. Present Android Auto browsing, persisted playlists/saved queues, ReplayGain, backup and production custom DSP as future work. Avoid invented metrics, dates, partners, permissions, flawless-reliability claims or copyrighted music/artwork without rights. Tone: thoughtful, premium, technically honest, human-centered. Use real Audia screenshots or label generated imagery as concept visuals.

