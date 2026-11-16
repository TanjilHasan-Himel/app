# Audia decisions

## D-001 — preserve the immersive Player

Keep the approved cover-first surface. Repair measured layout defects; do not replace it with a generic album-art/details screen.

## D-002 — service owns playback

Keep one ExoPlayer and MediaSession in `MusicService`; UI communicates through MediaController. Do not add a second UI player.

## D-003 — direct-client optional providers

Keep optional lyrics/metadata requests device-to-provider. No developer proxy, scraping bypass or secret API key in the APK.

## D-004 — correctness before feature count

Fix migration, contextual queue, service and visible P1 defects before playlists, smart mixes or advanced DSP.

## D-005 — no popup for self-describing toggles

Repeat/shuffle/speed should show state in their controls. Reserve transient messages for errors or actions without persistent visible state.

## D-006 — do not trust legacy completion reports

Old screenshots and “complete” documents are historical evidence only. A feature becomes Verified only with current executed evidence.

