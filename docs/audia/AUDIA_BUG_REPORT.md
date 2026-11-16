# Audia confirmed bug report

## DATA-001 — destructive database upgrade

Reproduction: inspect both Room builders and install a build whose schema version has no migration from the installed database. Expected: user data migrates. Actual configuration: `fallbackToDestructiveMigration()` permits recreation. Impact: potential loss of favorites, schedules and caches. Do not simulate on the user's populated device until a backup-safe migration test exists.

## QUEUE-001 — collection context discarded

Reproduction: choose a track from a folder/album/artist/favorites/search result, then press Next. Expected: next item in that collection. Actual code: the selected URI index is resolved in `_songList` and the controller receives the complete library. Runtime reproduction remains required after the repair test is added.

Status 2026-11-12: repaired in source with `playSongInQueue`; Queue screen now observes the controller-backed queue. Compile/unit tests pass; physical device regression is pending, so this is not yet Verified.

## UI-001 — Lyrics region blank band/clipping

Evidence: supplied screenshot shows a large empty region immediately below the Lyrics/Tools tabs and lyrics continuing beneath viewport boundaries. Expected: the Lyrics/Tools region consumes remaining usable height without a dead band or overlap. Exact device/font/display scaling is not supplied, so root cause is not yet assigned.

## LYRICS-001 — unsafe first search result

Reproduction: exact LRCLIB `/get` returns 404 for a title with multiple versions. Expected: conservative artist/duration match or unavailable state. Actual code: title-only search and first usable result may be cached. Impact: wrong lyrics persist for the song.

