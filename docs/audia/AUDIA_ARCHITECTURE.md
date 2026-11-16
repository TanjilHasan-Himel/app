# Audia architecture

## Runtime flow

`MainActivity` → Compose `NavHost` → shared `MusicViewModel` → asynchronous Media3 `MediaController` → `MusicService` → one ExoPlayer + one `MediaLibrarySession`.

The service is the playback owner. UI screens observe StateFlows from the shared ViewModel. Playback events update current song, position, duration, repeat/shuffle and persistence. This is directionally correct, but the ViewModel remains very large and also owns scanning, lyrics, timers, effects, notifications and many settings.

## Navigation and UI

Routes are Home, Player, Queue, Folder, Scheduler and About. Albums, artists, favorites and search are Home tabs rather than independent navigation destinations. The Player is cover-first and contains integrated metadata/progress plus adaptive playback and Lyrics/Tools regions.

## Queue model

The current queue is whatever `MediaController.setMediaItems()` last received. `playSong(uri)` always constructs that list from the complete library. Collection screens therefore cannot preserve their origin. The repair should introduce a single method accepting `(songs, selectedUri)` and keep `playSong(uri)` only as the all-library convenience path.

## Data and persistence

- Room v8: songs, favorites, scheduled tasks, lyrics cache and Song Wiki cache.
- Preferences DataStore: playback state, UI/settings, scan filters, route position and named A-B loop presets.
- MediaStore: source of local audio metadata.

There are currently two Room builders and both allow destructive fallback. This is the first data-layer repair target.

## Lyrics and metadata

Resolution order in current code is cache/local lookup followed by direct LRCLIB. Song Wiki queries Wikipedia and MusicBrainz directly. Provider calls use `HttpURLConnection`; no application backend was found. Correctness controls, cancellation and rate-limit behavior need strengthening.

## Background work

Media3 owns playback. AlarmManager/receivers implement scheduled playback and boot rescheduling. WorkManager runs engagement/recommendation work. Scheduling periodic work from Activity `onDestroy` is lifecycle-dependent and should be reviewed, but not rewritten without a demonstrated failure.

