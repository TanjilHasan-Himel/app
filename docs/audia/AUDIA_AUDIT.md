# Audia baseline audit

Remediation update (2026-11-12): NETWORK-001 and the lyrics-confidence portion of LYRICS-002 were repaired after this baseline. LRCLIB now uses URI-owned cache, cancellation/deduplication, Retry-After handling, confidence ranking and manual selection. Radio Browser and direct station hosts are additional disclosed destinations. See `AUDIA_QA.md` for current evidence.

Date: 2026-11-12. Branch: `main`. Scope: Stage A static audit plus local debug build/unit-test baseline. This document does not claim unperformed device coverage.

## Repository and build

- Single Android application module: `:app`; Kotlin 2.1.0, AGP 8.8.0, Java 17, Compose BOM 2024.09.00.
- Min SDK 24, target SDK 34, compile SDK 36. AGP warns that 8.8.0 was tested only through compile SDK 35.
- Media3 1.3.1, Room 2.6.1, Hilt 2.55, DataStore 1.0.0, Coil 2.6.0 and WorkManager 2.9.0.
- The worktree already contained substantial uncommitted Player/filter/performance changes before this audit. They were preserved.
- `origin` is the public GitHub repository `TanjilHasan-Himel/playtime-musicapp`. No push, release or publication was performed.

## Architecture assessment

- `MainActivity` owns Compose navigation and one shared `MusicViewModel` for Home, Player, Queue, Folder and Scheduler routes.
- `MusicViewModel` connects to `MusicService` through a Media3 `MediaController`; `MusicService` owns one ExoPlayer and one `MediaLibrarySession`.
- MediaStore scanning is cached in Room. Favorites, scheduled tasks, lyrics and Song Wiki also use Room; user preferences, playback position and named A-B presets use Preferences DataStore.
- The immersive Player is implemented, including integrated artwork metadata, progress, gestures, adaptive controls, Lyrics/Tools and named per-song loop presets.
- Optional online data uses direct client requests. No developer-owned proxy was found.

## Confirmed findings

| ID | Severity | Finding | Evidence | Action |
|---|---|---|---|---|
| DATA-001 | P0 | Database upgrades may erase songs, favorites, schedules, lyrics and wiki cache. | Both Room builders call `fallbackToDestructiveMigration()`; schema version is 8 and schemas are not exported. | Add explicit migrations and migration tests before beta upgrades. |
| QUEUE-001 | P1 | Collection playback loses queue context. | Folder, album, artist, favorites and search call `playSong(uri)`; it always calls `setMediaItems()` with the complete `_songList`. | Add one context-aware queue entry point and migrate callers. |
| UI-001 | P1 | Player/Lyrics layout has a visible blank band and clipping risk on the tested tall device. | User screenshot; current Player has a very large adaptive implementation and Lyrics is placed below fixed Player regions. | Reproduce with layout bounds; repair after core queue/data risks. |
| SERVICE-001 | P1 | Playback service is exported without an explicit controller trust policy found in the audit. | Manifest has `android:exported="true"`; `onGetSession()` returns the session. | Define intended external-controller support and reject untrusted controllers if not required. |
| LYRICS-001 | P1 | LRCLIB search fallback may cache a wrong song. | On exact 404, `/api/search?q=title` accepts the first usable result; no artist/duration confidence threshold is evident. | Add conservative candidate scoring and replacement flow. |
| DATA-002 | P1 | Two database construction paths can create configuration drift. | `PlayTimeDatabase.getDatabase()` and Hilt `DatabaseModule` both build the same database independently. | Inject all DAOs/clients from the Hilt singleton. |
| BUILD-001 | P2 | Build tool/SDK support is mismatched. | AGP 8.8.0 emits compile SDK 36 compatibility warning. | Align compile SDK or upgrade AGP after regression check. |
| SHARE-001 | P2 | Incoming share/open-with is absent. | MainActivity has only MAIN/LAUNCHER; no ACTION_VIEW/SEND handlers. | Implement only after queue/service P1 work. |
| NETWORK-001 | P2 | Provider resilience is incomplete. | LRCLIB uses direct `HttpURLConnection`; no request deduplication, Retry-After/429 handling or track-change cancellation was found. | Add bounded provider behavior without a proxy. |
| WIKI-001 | P2 | Song Wiki can present weak or fabricated-looking metadata. | First-result fallbacks and the generated “high-fidelity digital master” summary are used when Wikipedia data is absent. | Never fabricate; label source and require a defensible match. |
| PERMISSION-001 | P2 | Permission surface needs justification. | Manifest includes RECORD_AUDIO, exact alarm variants, battery-optimization request and full-screen intent. | Gate by actual feature and document user benefit/platform policy. |
| TEST-001 | P1 | Automated coverage is far below visible feature surface. | Four unit-test files and one example instrumented test; no queue/service/migration/UI regression suite. | Add tests in repair order. |

## Existing improvements confirmed in source

- A-B state clears on a newly selected song; named presets remain keyed by song URI in DataStore.
- Repeat and shuffle state are represented directly in controls rather than requiring popup feedback.
- Scanning supports duration and folder/category filters; filter unit tests exist.
- Visualizer sampling/recomposition and large blur costs were reduced in the previous performance pass.

## Security and privacy

- No embedded API key or bearer-token pattern was found by the repository heuristic scan.
- Network destinations found: `lrclib.net`, `en.wikipedia.org`, `wikipedia.org` and `musicbrainz.org`.
- The music library remains locally scanned; online requests send per-track title/artist/album/duration metadata as applicable.
- The exported media service and broad permissions require resolution before beta readiness.

## Recommended repair order

1. DATA-001 migration safety and a single database instance.
2. QUEUE-001 contextual queue construction and tests.
3. Service/controller trust, lifecycle, notification and background tests.
4. Player/Lyrics responsive defect reproduction and focused repair.
5. Lyrics confidence, invalidation and provider resilience.
6. Remaining visible feature verification and beta regression matrix.

