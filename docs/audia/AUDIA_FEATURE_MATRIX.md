# Audia feature matrix

Status meanings: **Verified** requires executed evidence; **Partial** means code exists but coverage is incomplete; **Broken** has a confirmed defect; **Missing** has no implementation; **Unknown** needs runtime verification.

| Feature | Source / UI entry | Persistence / dependency | Status | Severity | Evidence and next action |
|---|---|---|---|---|---|
| Local library scan | `MusicRepository`, Home | MediaStore + Room | Partial | P1 | Code and filter tests exist; large/corrupt libraries not tested. |
| Scan duration/folder filters | `MusicScanFilter`, Settings sheet | DataStore | Partial | P1 | Unit tests pass; call-record/WhatsApp device behavior not reverified in this stage. |
| All Songs playback | Home → `playSong` | Media3 | Partial | P1 | Builds; basic physical playback was previously smoke-tested. |
| Album browsing | `AlbumList` | Library state | Partial | P1 | Albums exist, but no dedicated album-detail route. |
| Album contextual queue | Album list → `playSongInQueue` | Media3 | Partial | P1 | Context is now passed; runtime Next/Previous verification pending. |
| Artist contextual queue | `ArtistList` → `playSongInQueue` | Media3 | Partial | P1 | Context is now passed; runtime verification pending. |
| Folder contextual queue | `FolderScreen` → `playSongInQueue` | Media3 | Partial | P1 | Context is now passed; runtime verification pending. |
| Favorites | Home, Player | Room | Partial | P1 | Persistence code exists; favorite queue also loses context. |
| Search | Home search | In-memory filtering | Partial | P1 | UI exists; search playback loses context. |
| Playlists | — | — | Missing | P3 | Do not add before core reliability. |
| Queue screen | `QueueScreen` | MediaController | Partial | P1 | UI exists; origin contract is broken and reorder behavior unverified. |
| Next/Previous | Player/MiniPlayer/notification | Media3 queue | Partial | P1 | Queue construction repaired and unit-tested; physical collection-flow test pending. |
| Shuffle/Repeat | Player Tools | Media3 + DataStore state | Partial | P1 | State code exists; lifecycle and queue semantics need tests. |
| Play/Pause + haptics | Player/MiniPlayer | Media3 | Partial | P1 | UI/code exists; accessibility and device haptic strength not fully verified. |
| Artwork swipe/seek gestures | Hero artwork | Media3 | Partial | P1 | Gesture code exists; conflict matrix not executed. |
| Immersive responsive Player | `PlayerScreen` | Compose | Broken | P1 | User screenshot confirms Lyrics gap/clipping on one device. |
| MiniPlayer circular progress | `MiniPlayer` | MediaController position | Partial | P2 | Implemented; device/font-scale matrix pending. |
| Synchronized lyrics | `LyricsView`, `LrcLibClient` | local LRC/Room/LRCLIB | Partial | P1 | Direct provider works, but fallback match is unsafe. |
| Lyrics offset/resync | Player Tools/Lyrics | ViewModel state | Unknown | P2 | UI/code discovered; per-song persistence and sign convention need verification. |
| Song Wiki | `SongWikiBottomSheet` | Room + Wikipedia/MusicBrainz | Broken | P2 | Weak match and fabricated fallback wording confirmed. |
| A-B live loop | Player Tools | ViewModel | Partial | P1 | Clear-on-new-song exists; boundary accuracy needs automated/device test. |
| Named loop presets | `LoopPresetsBottomSheet` | DataStore by song URI | Partial | P2 | Multiple named presets implemented; upgrade/invalid URI behavior untested. |
| Playback speed | Player Tools | Media3 + DataStore | Partial | P2 | Code exists; transition and restore test pending. |
| Equalizer/effects | `AudioEffectsSheet`, manager | Android audio session | Unknown | P1 | UI and platform effects exist; audibility/session lifecycle require hardware test. |
| Sleep timer | Player Tools | ViewModel job | Partial | P1 | Timer code exists; process-death survival not supported/verified. |
| Stop after current | Player Tools | Polling job | Partial | P2 | Code exists; repeat/shuffle/end-of-queue interactions untested. |
| Scheduler | Scheduler route | Room + AlarmManager | Partial | P1 | CRUD/alarm code exists; exact alarm, reboot and OEM behavior unverified. |
| Song Garage | Bottom sheet | platform media APIs | Partial | P1 | Several visible tools exist; safe export/edit completion not verified. |
| Background playback | `MusicService` | Media3 service | Partial | P0 | Architecture exists; lock, recents, process/OEM matrix not executed. |
| Media notification/lock screen | Media3 session | System UI | Partial | P1 | Previously smoke-tested; metadata/action lifecycle needs formal QA. |
| Audio focus/noisy route | `MusicService` | Media3 | Partial | P0 | Audio attributes and becoming-noisy enabled; call/Bluetooth tests pending. |
| Incoming share/open-with | Manifest/MainActivity | Android intents | Missing | P2 | No filters/handling. |
| Engagement notifications | workers | WorkManager | Partial | P2 | Scheduled from Activity `onDestroy`; notification policy/UX needs review. |
| Settings | Settings dialog | DataStore | Partial | P2 | Many settings exist; complete persistence matrix pending. |
| Landscape | Player adaptive layout | Compose | Unknown | P1 | Portrait lock removed in dirty worktree; landscape not tested this stage. |

