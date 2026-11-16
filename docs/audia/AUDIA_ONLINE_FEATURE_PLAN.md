# Audia online feature plan

Date: 2026-11-12. Execution is deliberately one subsystem at a time.

Status: MiniPlayer, Lyrics correctness and Radio Browser implementation are complete. The unlocked-device radio playback matrix remains pending; authorized secondary lyrics providers remain gated on credentials and content rights.

## Phase 1 — MiniPlayer visual polish (active)

Scope: presentation only. Preserve playback callbacks, circular artwork progress, Outfit typography and 48dp controls.

Acceptance criteria:

- Translucent glass base with artwork-derived accent supplied by Home.
- Subtle liquid glow only while playing, capped at 20fps.
- One-shot shimmer when the track changes; no permanent shimmer loop.
- Motion stops when paused or system animations are disabled.
- No layout shift, clipping, lost semantics or reduced touch targets.
- Debug build/unit tests pass and the available physical device is visually checked.

## Phase 2 — Lyrics correctness and fallback

Order: embedded/local LRC → user-selected lyrics → valid cache → LRCLIB exact match → confidence-scored LRCLIB candidates → manual selection → unavailable state.

Before adding another provider: implement cancellation, request deduplication, `429 Retry-After`, cache replacement and title/artist/duration scoring. YouLy+/LyricsPlus and Better Lyrics/Unison are not approved production providers until their Android API access and content rights are documented. Musixmatch requires authorized access and must not expose a secret in the APK.

## Phase 3 — Real internet radio

Use Radio Browser only after Phase 2. Streams play directly in Media3; no developer proxy. Required: station search/country/language/tag, favorites/history, codec/bitrate, ICY metadata, click counter, retry/backoff, broken-stream handling, separate radio/local queue semantics, notifications and privacy disclosure. Hardware FM is out of scope.

