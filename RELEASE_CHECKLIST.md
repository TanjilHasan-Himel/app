# Audia Player — privacy, legal & Android release checklist

This checklist accompanies `Legals.kt`. The text is a **review draft**, not a security audit, legal opinion, or proof of compliance. It assumes no current network services or telemetry; verify against your actual release build.

Developer: **Tanjil Hasan Himel**  
Email: [taanjilhasan@gmail.com](mailto:taanjilhasan@gmail.com)  
GitHub: [https://github.com/TanjilHasan-Himel](https://github.com/TanjilHasan-Himel)

---

## Mandatory pre-release checks

- [ ] Confirm the App name, publisher identity, support email, current version and update date match the published listing.
- [ ] Inspect the **merged release AndroidManifest.xml** for `INTERNET`, `ACCESS_NETWORK_STATE`, `READ_MEDIA_AUDIO`, `READ_EXTERNAL_STORAGE` (legacy), `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`, foreground service, boot, and wake permissions. Remove unneeded access.
- [ ] Inspect dependencies and the actual release APK/AAB for analytics, crash-reporting, ads, networking, identifiers, SDK permissions, and unexpected telemetry.
- [ ] Test app behavior while offline; verify no upload of paths, media metadata, user events, or audio. If any online service exists, update the policies and disclosures before release.
- [ ] Decide whether Android cloud backup/device-to-device transfer is allowed. If truly local-only with no transfer desired, configure `android:allowBackup`, Android 12+ `dataExtractionRules`, and legacy `fullBackupContent` appropriately; test on actual devices. `allowBackup=false` alone may not stop every device-to-device transfer.
- [ ] Ensure App Data Safety answers match the final release and every included SDK. Have a public, non-editable, non-PDF privacy policy webpage linked in Play Console and available in-app.
- [ ] If collecting new data or adding Firebase Analytics/Crashlytics/online lyrics/Shazam, update this Privacy Policy, the data-safety declaration, any consent flow, and the app itself before release.
- [ ] Avoid claiming `zero-knowledge`, `100% secure`, `guaranteed exact alarm`, `no tracking` or `permanently deleted` without rigorous supporting evidence.
- [ ] Review open-source dependency licenses and attribution; a blanket proprietary-rights claim does not erase third-party licenses.
- [ ] Ensure users can open Privacy Policy and EULA in Settings/About and read without network access.

## Android scheduler checks

- [ ] Prefer `SCHEDULE_EXACT_ALARM` when a user-facing scheduled playback feature genuinely needs exact timing; do not add restricted `USE_EXACT_ALARM` without verifying Play eligibility.
- [ ] Handle the runtime `canScheduleExactAlarms()` state, denial/revocation, reboot, force-stop, Doze, manufacturer battery policies, DND and silent/volume routing.
- [ ] Test actual Media3 background playback service and `FOREGROUND_SERVICE_MEDIA_PLAYBACK` declaration on Android 14+.
- [ ] Provide a UI warning: **Scheduled playback is not a guaranteed alarm; use a dedicated backup alarm for important events.**

## Hardening checks (beyond legal text)

- [ ] Use current dependencies and sign release builds with controlled signing keys; enable R8/minification if applicable.
- [ ] Ensure only intended activities, services, receivers, and providers are exported; validate external intents and deep links.
- [ ] Use explicit and immutable `PendingIntent` where practical; avoid unsafe intent forwarding.
- [ ] Do not log media file paths, listening behavior, or sensitive metadata in release builds.
- [ ] Ensure local databases are not world-readable, and sensitive values are not exposed via exported content providers.
- [ ] Use the smallest practical runtime permissions; never request broad all-files access for a conventional local audio player.
- [ ] Test library indexing, folder exclusion, schedule cancellation, restore/reboot handling, battery usage, and failure messaging.
- [ ] Consider local **Export / Import** for playlists and schedules, so users can control backup without developer-hosted cloud.

## Useful official references

- Google Play user-data policy: https://support.google.com/googleplay/android-developer/answer/10144311
- Google Play Data safety: https://support.google.com/googleplay/android-developer/answer/10787469
- Android Auto Backup: https://developer.android.com/identity/data/autobackup
- Android exact alarms: https://developer.android.com/develop/background-work/services/alarms
- Play exact-alarm permission: https://support.google.com/googleplay/android-developer/answer/16558241
- Media3 background playback: https://developer.android.com/media/media3/session/background-playback

**Important:** Before publishing, have a qualified lawyer review the agreement for your target jurisdictions and confirm statements against the exact release version.

