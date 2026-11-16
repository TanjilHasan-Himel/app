# Audia repair backlog

| ID | Priority | Depends on | Files | Acceptance criteria / tests | Status |
|---|---|---|---|---|---|
| DATA-001 | P0 | — | database, Hilt module | No destructive fallback; explicit v6/v7→v8 path; migration test preserves favorites/schedules/songs. | Implemented; instrumentation migration test pending |
| DATA-002 | P1 | DATA-001 | ViewModel, database module | All DAOs/clients share one injected database instance. | Confirmed |
| QUEUE-001 | P1 | DATA-001 | ViewModel + playback callers | Album/folder/artist/favorites/search queue equals visible collection and selected item remains current; unit tests cover invalid/duplicate URIs. | Implemented; device regression pending |
| SERVICE-001 | P1 | QUEUE-001 | service, manifest | Intended controllers connect; untrusted controllers are rejected if external browsing is not intended; background/lock/recents QA documented. | Confirmed |
| UI-001 | P1 | QUEUE-001 | Player, Lyrics | No blank band, clipping or overlap on tested compact/tall/landscape configurations; touch targets remain usable. | Reproduction captured |
| LYRICS-001 | P1 | DATA-001 | LRCLIB client, DAO, ViewModel | Candidate requires title+artist+duration confidence; wrong cache can be replaced; unit tests cover ambiguity. | Confirmed |
| EFFECT-001 | P1 | SERVICE-001 | effect manager, service, sheets | Effects attach to current session, release once and have audible hardware evidence or honest unsupported state. | Needs device test |
| NETWORK-001 | P2 | LYRICS-001 | network clients | Cancellation, dedupe, timeout and 429 Retry-After behavior tested; no proxy introduced. | Confirmed |
| SHARE-001 | P2 | QUEUE-001 | manifest, activity | Safe ACTION_VIEW/SEND content URI handling; cold/warm intent tests; no automatic download. | Missing |
| BUILD-001 | P2 | core P1 work | Gradle config | No compile SDK support warning; debug/release compile and tests pass. | Open |
| QA-001 | P1 | each repair | tests/docs | Exact executed tests and device configurations recorded; untested cases stay marked NOT TESTED. | In progress |
| UI-002 | P2 | UI-001 | MiniPlayer, Home | Fluid glass base, playing-only capped liquid glow and one-shot track shimmer; no touch/a11y regression. | Implemented; portrait device pass, extended responsive/a11y matrix pending |
| LYRICS-002 | P1 | LYRICS-001 | lyrics client/cache/UI | Correct fallback order, scoring, cancellation, dedupe, 429 handling and manual replacement before secondary providers. | Implemented; focused/unit suite pass, live API smoke pass |
| RADIO-001 | P2 | SERVICE-001, NETWORK-001 | service/data/new radio UI | Direct real-station playback through Radio Browser with metadata, retries, favorites and separate queue semantics. | Implemented; build/install/startup pass, live station device playback pending |
| UI-003 | P2 | QUEUE-001, RADIO-001 | Folder, Home feed, carousel, radio search | Folder playback always exposes the active local/radio MiniPlayer; Recently Played uses an accessible depth carousel; radio search scope is explicit. | Implemented; phone pass after navigation-inset repair, wider matrix pending |
| UI-004 | P2 | UI-003 | Home Most Played | One featured real-data card with rank, readable metadata, swipe, arrows, tap-to-play and lightweight fallback rendering. | Implemented; phone interaction pass, extended responsive matrix pending |
| DSP-001 | P1 | SERVICE-001, EFFECT-001 | DSP feasibility, isolated filter prototype | Preserve the OEM-effect baseline; document licensing/latency constraints; prove a disabled-safe biquad with deterministic tests before playback integration. | Feasibility and isolated prototype complete; playback integration and audio measurement pending |

Next validation batch: unlocked-device radio playback, search/favorite/recent persistence, notification controls, HLS/progressive streams and network-switch regression. Authorized secondary lyrics provider remains gated on credentials/licensing.

## Canonical forward phases — 2026-11-13

Detailed scope, dependencies and exit gates now live in `AUDIA_EXECUTION_ROADMAP.md`:

`F0 foundation/security/recovery → A1 Android Auto → L2 playlists/saved queues → P3 gapless/ReplayGain → B4 backup → T5 tags/Open With → PERF6 benchmarks`.

The next executable task is F0C. Do not infer that the existing transient Media3 queue is a saved
queue, that the current in-memory Tag Studio writes files, or that the empty Auto browse callback is
a completed Android Auto integration.

