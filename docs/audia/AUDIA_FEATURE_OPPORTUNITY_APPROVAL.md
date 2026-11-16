# Audia — feature opportunity audit and senior approval brief

**Prepared:** 14 November 2026 (Asia/Dhaka)  
**Decision owner:** Audia product owner / senior reviewer  
**State:** Research and proposal; no feature in this document is approved merely by appearing here.  
**Scope:** Current Android repository, existing Audia roadmap, and a desk review of four competing players. This is a product and engineering audit, not a user study or a Play Store review analysis.

## Executive decision

Audia already has a strong concept: scheduled listening, per-song named A–B loops, a local library and a distinctive artwork-first Player. The biggest gap is not another visual effect. Users need to trust the current journeys and keep their collections and settings when they change devices. The next investments should therefore be reliability, Android Auto browsing, saved playlists/queues, backup, and measured playback quality.

**Approval requested:** endorse the phase order and feature boundaries below. This document does not authorize shipping, third-party licensing, new permissions, or an API integration. Each feature still needs its own acceptance gate and evidence.

## What existed before this report

- `AUDIA_EXECUTION_ROADMAP.md` already names F0, Android Auto, persistent playlists/queues, gapless/ReplayGain, backup, file tags, performance and later features.
- `AUDIA_ONLINE_FEATURE_PLAN.md` records lyrics/radio provider choices and explicitly defers unlicensed secondary providers.
- `AUDIA_AUDIT.md`, `AUDIA_FEATURE_MATRIX.md`, `AUDIA_QA.md` and `AUDIA_DSP_FEASIBILITY.md` contain useful code and device findings.
- `AUDIA_VIDEO_MASTER_CONTEXT.md` describes the app for a video; it is not a senior approval document.

**Gap before this report:** there was no single current competitor comparison with an opportunity inventory, business rationale, scope ruling, dependencies, trade-offs and approval questions. The earlier market recommendations were useful, but they did not constitute a complete, verified market study. This report fills the decision-record gap; hands-on competitor tests, user interviews and pricing/review analysis remain undone.

## Assumed user and expected load

Primary user: one person with a local music library, possibly many thousands of songs, who wants both ordinary playback and scheduled long sessions. Audia also has optional internet radio and online lyrics/metadata. The performance target must eventually include an older 60 Hz phone and a current 90/120 Hz phone, with large libraries and multi-hour playback. No server-side multi-user load is assumed because the inspected app has no Audia-operated backend.

## Market scan — official evidence

| Product | What its own materials confirm | What it teaches Audia | What it does **not** prove |
|---|---|---|---|
| [Poweramp](https://powerampapp.com/) | Folder/library browsing, playlists, smart playlists, tag editing, gapless, crossfade, ReplayGain, Android Auto, widgets and extensive output controls | These are established expectations for a serious local player; Auto and audio claims need real device testing | It does not validate Audia's scheduler demand or prove Audia should copy its heavy audio engine |
| [Musicolet Play listing](https://play.google.com/store/apps/details?id=in.krosbits.musicolet) | Multiple queues, playlist access, tag/art editing and Android Auto browsing are advertised | Multiple saved queues and reliable local library organization are a real comparison point | Listing claims are not an independent hands-on test, and feature quality was not measured here |
| [Symfonium](https://symfonium.app/) and [its documentation](https://docs.symfonium.app/) | Smart playlists/filters, ReplayGain, smart fades, Android Auto, Cast/UPnP and many server/cloud providers | Smart library rules and playback continuity can add value after local reliability; network-provider breadth is costly | Audia need not become a NAS/cloud client to differentiate itself |
| [foobar2000 overview](https://www.foobar2000.org/?page=Overview) | Gapless, ReplayGain, advanced tagging, local-network playback and internet radio including Radio Browser | Shows that measured audio behavior and library data quality matter alongside UI | Its cross-platform feature set is not an Android implementation plan for Audia |

**Positioning inference:** most compared players compete on breadth of library/audio/output features. Audia can be distinctive by making a scheduled listening session easy to set up, inspect, recover and trust, while retaining an expressive Player. This is a product inference from published feature sets, not a validated customer preference.

### Market research still missing

No hands-on competitor journey test, UX timing study, real user interview, willingness-to-pay test, Play Store review clustering, accessibility comparison, battery comparison or controlled audio measurement has been done for this brief. Before a public positioning claim such as “first,” “best,” “most reliable” or “more efficient,” run those studies. Do not derive market share or user preference from official feature pages.

## Code reality and approval baseline

| Capability | Current state | Evidence in repo | Decision implication |
|---|---|---|---|
| Active queue | Exists through MediaController; Queue screen opens after the 14 November crash fix | `MusicViewModel.kt`, `QueueScreen.kt`, `AUDIA_QA.md` | Do not market as multiple saved queues |
| Saved playlists/queues | No dedicated persistent entities | `PlayTimeDatabase.kt` entities; roadmap L2 | Core library-management proposal |
| Android Auto | Exported MediaLibraryService and root exist; children are empty | `MusicService.kt:onGetChildren` | Car browsing is not delivered |
| Tag Studio | Updates in-memory song fields; does not persist media file tags | `MusicViewModel.kt:updateSongTags`, `SongGarageBottomSheet.kt` | Current success wording should be repaired before marketing |
| Scheduler | Room task, AlarmManager, receivers and service playback exist | `ScheduledTask.kt`, `AlarmScheduler.kt`, `AlarmReceiver.kt`, `MusicService.kt` | Protect as USP; full timed/OEM/API 37 matrix still missing |
| Backup/import | No portable one-file restoration path | Roadmap B4 | Data portability gap, especially before more saved data |
| Gapless/ReplayGain | No verified, complete production implementation | Roadmap P3; DSP feasibility note | Do not claim audiophile or normalized playback |
| Radio | Radio Browser directory client and direct streams in code | `RadioBrowserClient.kt`, `RadioRepository.kt`, QA | Broad live-stream/reconnect matrix pending |
| Performance | Debug traces still showed high UI jank; Macrobenchmark absent | `AUDIA_PERFORMANCE.md`, roadmap PERF6 | No smooth-60/120-fps claim |
| Security | Exported service custom command broadly available; global cleartext enabled | `MusicService.kt`, `AndroidManifest.xml` | Resolve before beta distribution |

## Ranked findings

### Must fix

1. **The scheduled session still lacks an end-to-end reliability certificate.**
   - **What this is:** A saved task is registered with AlarmManager and starts playback in a media service.
   - **Problem:** Long playback, reboot/time changes, exact-alarm revocation, Android 16/17 behavior and recovery from a crash have not passed a timed device matrix. Android 17 changes background audio rules; the exact-alarm exception described by Android concerns `USAGE_ALARM`, while Audia currently uses `USAGE_MEDIA` ([Android 17 guidance](https://developer.android.com/about/versions/17/changes/bg-audio)).
   - **Fix:** Finish F0 scheduler state/status, bounded elapsed-position recovery and physical tests across the relevant OS/device matrix. Display permission and last-run health in Scheduler UI.
   - **If skipped:** The defining promise can fail silently at the time the user depends on it.

2. **User collections cannot be saved or moved between devices.**
   - **What this is:** The active Media3 queue and local settings work during ordinary playback.
   - **Problem:** There are no persisted playlist/saved-queue records and no `.audia` backup. A reinstall or device change cannot restore those collections.
   - **Fix:** Add ordered Room playlist/queue entities with migrations, then a versioned export/import with dry-run and rollback.
   - **If skipped:** The app becomes more valuable to use but more painful to lose.

3. **Android Auto looks integrated but cannot browse music.**
   - **What this is:** The media service is exported and exposes a library root.
   - **Problem:** `onGetChildren()` returns an empty list, so a car controller cannot browse the actual library. Android's Media3 guidance expects a `MediaLibraryService` for browsable playback ([Android Media3 guidance](https://developer.android.com/media/media3/session/background-playback)).
   - **Fix:** Add stable IDs, paged Tracks/Albums/Artists/Folders/Favorites children, item lookup, search and trust-restricted commands. Add playlists after persistence exists.
   - **If skipped:** Calling Audia “Android Auto ready” misleads users and leaves a major driving use case empty.

4. **The service/network boundary is wider than intended.**
   - **What this is:** A public MediaLibraryService accepts controllers, while selected HTTP radio stations require legacy transport.
   - **Problem:** The custom audio-session command is granted broadly and `usesCleartextTraffic=true` applies globally.
   - **Fix:** Restrict custom commands to Audia's own controller and scope cleartext handling to justified radio traffic; test external controllers and provider HTTPS.
   - **If skipped:** Other apps may reach internal session details, and unrelated traffic can use insecure transport.

### Should fix

5. **Tag Studio reports a file save it has not done.**
   - **What this is:** The UI lets a user edit displayed metadata.
   - **Problem:** `updateSongTags` mutates current in-memory state; the original audio file remains unchanged after restart or external read.
   - **Fix:** Immediately make the result wording honest. Later implement format-aware MediaStore/SAF writes with preview, permission flow, rescan and read-only handling.
   - **If skipped:** Users can believe their library is repaired when it is not.

6. **Playback continuity and loudness are not measured.**
   - **What this is:** Media3 plays local tracks and the app exposes Android audio effects.
   - **Problem:** True gapless boundaries, album/track ReplayGain and fade behavior have no production acceptance evidence.
   - **Fix:** Use test-tone fixtures; define separate Off/Gapless/Fade modes; first read existing ReplayGain/R128 tags, apply clipping-safe gain and test by route/device. Scanning the full library is a later subphase.
   - **If skipped:** Album transitions may have gaps and mixed songs may jump in volume.

7. **Performance claims currently outrun measurements.**
   - **What this is:** Home/Player use custom Compose motion, artwork and visualizers.
   - **Problem:** Existing debug traces reported substantial UI-thread jank. There is no release/profileable Macrobenchmark or Baseline Profile evidence.
   - **Fix:** Benchmark startup, carousel fling, Player open and lyrics scroll on old and modern devices; repair measured work before generating a profile.
   - **If skipped:** A visually impressive screen can still feel laggy and drain battery.

8. **Feature/status documents disagree.**
   - **What this is:** Audits, feature matrices, checkpoints and roadmap were written across different phases.
   - **Problem:** Older files still say Room v8, old dependency versions or unresolved defects that later changed.
   - **Fix:** Keep one live status table and mark older audits historical; link each verified item to current QA evidence.
   - **If skipped:** Seniors may approve work based on stale claims.

### Nice to have after core gates

9. **Long content needs a deliberate resume/bookmark policy.**
   - **What this is:** Audia already persists general playback position and supports long scheduled content.
   - **Problem:** A user listening to a lecture/Quran passage may want several named points and per-track resume behavior.
   - **Fix:** Add per-track Remember Position plus optional named bookmarks once recovery/backup semantics are settled.
   - **If skipped:** Long-form listeners must seek manually; it does not block basic music playback.

10. **Home insight could be more useful than another decorative rail.**
    - **What this is:** Recently/Most Played already records listening behavior locally.
    - **Problem:** Those counters do not yet answer simple questions like last week's listening or next scheduled session.
    - **Fix:** Add a local-only weekly summary and upcoming-session card after the performance and data model gates.
    - **If skipped:** Discovery is less informative, but core playback remains intact.

**Verdict:** fix the scheduled-playback trust chain and F0 security/data gates first. Then implement Auto browsing, saved collections and portable backup. Validate audio and performance before making quality claims.

**Lean:** remove the obsolete reorder dependency (already completed); no further line-count or dependency-saving number is defensible from this product-level review.

**Not checked:** Play Store reviews/analytics, hands-on competitor apps, Android Auto hardware/head unit, Android 16/17 physical device, tablet, Wear OS, Cast device, sustained battery and laboratory audio-quality measurements. This is a targeted product/critical-path code audit, not a line-by-line audit of every file.

## Feature opportunity register and scope ruling

`Now` = proposed within current beta path; `Later` = preserve as a named opportunity with an entry condition; `Research` = decision needs proof/rights first; `Reject` = conflicts with product purpose. None means “forget it.”

| ID | Opportunity | Why it matters to Audia | Ruling | Dependency / acceptance evidence |
|---|---|---|---|---|
| O01 | Scheduler capability/status checklist | Makes the USP inspectable: permission, next run, last result, recovery state | **Now, P0** | F0 service/recovery; timed Android matrix |
| O02 | Precise scheduled position recovery | Long sessions should not restart an hour-long track after process death | **Now, P0** | Persist bounded `activePositionMs`; process-death test |
| O03 | Android Auto browse/search | Safe access in car; current tree is empty | **Now, P0** | F0 controller trust, stable IDs, paging, DHU test |
| O04 | Persistent playlists | Basic collection management and real scheduler selection | **Now, P1** | Room migration, order/duplicate/deleted-file rules, restart test |
| O05 | Multiple named saved queues | Strong local-player workflow, seen in Musicolet's advertised offering | **Now, P1** | O04 storage model; switch/resume/reboot test |
| O06 | One-file Audia backup/import | Preserves schedules, loops, favorites, settings and new collections | **Now, P0 before wider beta** | O04/O05, archive versioning, dry-run, corruption/rollback test |
| O07 | Gapless + separate Smart Fade | Continuity for albums, recitations and mixes | **Now, P1** | Media3 boundary measurement, route/offload matrix |
| O08 | ReplayGain Off/Track/Album | Avoids volume jumps without changing system volume | **Now, P1** | Existing tag reader first; peak/headroom test; scanner later |
| O09 | Honest tag/artwork editor | Repairs local-library quality and a misleading current UI | **Immediate truth fix; writer later P1** | MediaStore/SAF, external-reader and format tests |
| O10 | Open With/share audio | Lets file manager/WhatsApp hand audio to Audia | **Now, P1 after queue model** | URI grants, Play Now/Next/Queue chooser, cold/warm tests |
| O11 | Smart playlists | Makes filters and listening history useful together | **Later, P1** | Persisted playlists, predictable rule engine, large-library test |
| O12 | Per-track resume/bookmarks | Important for Quran, lecture and audiobook length | **Later, P1** | Position policy, sync with schedules and backup |
| O13 | Widgets incl. next schedule | Quick access and visible USP | **Later, P2** | Background state contract and privacy review |
| O14 | Local listening insights | Useful extension of Most Played | **Later, P2** | Local-only play history accuracy and opt-out |
| O15 | Opt-in scrobbling | History portability for interested users | **Later, P2** | Consent, endpoint/credential model, offline queue |
| O16 | Cast/UPnP | External speakers | **Later, P2** | Local playback stable, output/focus behavior and device tests |
| O17 | NAS/WebDAV/Navidrome | Access large remote libraries | **Later, P3** | Unified provider model, auth/caching/offline design |
| O18 | AutoEQ/custom PCM DSP | Strong sound-tuning story | **Research, P3** | Rights/licensing, latency, CPU, offload, audio measurement |
| O19 | Authorized secondary lyrics | Better lyric coverage | **Research** | Provider contract/rights, no embedded secret, match quality |
| O20 | Hardware FM tuner | Different hardware feature, not uniformly exposed on Android | **Reject for current product** | Only reconsider for a specific supported device family |
| O21 | More always-on shimmer/3D on utility UI | Looks active but increases battery/jank and weakens hierarchy | **Reject** | Signature surfaces already have motion; test restrained design |

### Scope verification rule

A feature marked **Later** or **Research** is not thrown away. It remains in this register with a reason, dependency and reopening condition. Only **Reject** removes it from the present product direction, and that ruling is reviewable. Do not mark an unimplemented feature “out of scope” merely to make a release checklist look complete. Beta scope should explicitly list what is included, what is deferred, and what the UI/marketing must not claim.

## Proposed phase gates for senior approval

| Gate | Deliverable | Required proof before closing | Product decision |
|---|---|---|---|
| G0 Foundation | F0 migration, controller trust, transport scope, scheduler recovery/status | Populated migration, schedule/device, service/controller, notification/route tests | Approve as first gate |
| G1 Car | Android Auto library tree/search | 5,000-item paging, item resolution, DHU/controller tests | Approve after G0 |
| G2 Collections | Playlists, named queues, scheduler snapshot semantics | Restart/reboot/order/duplicate/deleted-file tests | Approve after G1 IDs are stable |
| G3 Portability | One-file backup/import | Fresh-install restore, corruption rejection, no partial writes | Approve before wide beta |
| G4 Playback quality | Gapless, fade, ReplayGain | Test tones, clipping and speaker/wired/Bluetooth matrix | Approve implementation; prohibit quality claim until measured |
| G5 File tools | Honest tag write, artwork, Open With | External reader, unsupported format and URI-grant tests | Approve scoped formats first |
| G6 Performance | Macrobenchmark and Baseline Profile | Declared thresholds on old 60 Hz + modern 90/120 Hz devices | Approve measurement before marketing |
| G7 Expansion | Smart library, bookmarks, widgets, Cast, network sources, DSP | Separate feature briefs after beta gates | Keep in opportunity register |

**Order note:** G3 can run after G2's data model stabilizes. G4 and G5 can be implemented in parallel only when the team has capacity, but each keeps its own acceptance gate. The canonical engineering roadmap remains `AUDIA_EXECUTION_ROADMAP.md`; this brief proposes approval priorities and must not silently override that file.

## Questions for the senior decision record

1. **Approve the positioning?** “Local-first immersive player for reliable scheduled listening,” with A–B loops and real internet radio as supporting strengths.
2. **Approve the beta minimum?** G0–G3 plus proven local playback, scheduler and radio/lyrics regression. If not, name what can wait and why.
3. **Approve saved-queue semantics?** A scheduled session should capture an immutable snapshot by default; an explicit “Update from playlist” changes future content.
4. **Approve audio modes?** Off, Gapless and configurable Fade are separate; ReplayGain is Off/Track/Album, with no unmeasured “studio quality” claim.
5. **Approve the tag-editor truth fix immediately?** The UI should stop claiming tags were written until external-file persistence passes tests.
6. **Approve the research budget?** Hands-on competitor walkthroughs, 5–8 target-user interviews, Play Store review clustering and a two-tier device benchmark before final marketing claims.

## Evidence links and follow-up ownership

- Product/engineering status: [`AUDIA_EXECUTION_ROADMAP.md`](AUDIA_EXECUTION_ROADMAP.md), [`AUDIA_QA.md`](AUDIA_QA.md), [`AUDIA_DECISIONS.md`](AUDIA_DECISIONS.md).
- Technical feasibility: [`AUDIA_DSP_FEASIBILITY.md`](AUDIA_DSP_FEASIBILITY.md), [`AUDIA_PERFORMANCE.md`](AUDIA_PERFORMANCE.md), [`AUDIA_NETWORK_AUDIT.md`](AUDIA_NETWORK_AUDIT.md).
- Market sources: [Poweramp](https://powerampapp.com/), [Musicolet Play listing](https://play.google.com/store/apps/details?id=in.krosbits.musicolet), [Symfonium](https://symfonium.app/), [Symfonium docs](https://docs.symfonium.app/), [foobar2000](https://www.foobar2000.org/?page=Overview).
- Android feasibility: [Media3 background playback](https://developer.android.com/media/media3/session/background-playback), [exact alarms](https://developer.android.com/develop/background-work/services/alarms), [Android 17 background audio](https://developer.android.com/about/versions/17/changes/bg-audio).

**Approval log:** Pending. Record approved/deferred/rejected IDs, decision date, reviewer, reasons and any changed gate here; then update the canonical execution roadmap. No decision is implied by silence.
