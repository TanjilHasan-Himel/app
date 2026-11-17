package com.eplaytime.app.util

/**
 * Audia Player legal text — official production draft, 2026-11-18.
 */
object Legals {

    const val EULA = """
END USER LICENSE AGREEMENT AND TERMS OF USE
Audia Player | Version 9.1.3
Developer: Tanjil Hasan Himel
Last Updated: November 18, 2026
Contact: taanjilhasan@gmail.com
GitHub: https://github.com/TanjilHasan-Himel

1. ACCEPTANCE AND SCOPE
These Terms govern your use of Audia Player (the "App"), an Android application developed by Tanjil Hasan Himel (the "Developer"). By installing or using the App, you agree to these Terms to the extent permitted by applicable law. If you disagree, please stop using the App. Nothing in these Terms limits rights that cannot legally be waived.

2. WHAT THE APP DOES
Audia Player is a local-first audiophile audio player and workstation designed to discover, organize, and play audio already accessible on your device. Features include:
• Local media scanning, album art visualization, and ID3 tag parsing
• Persistent custom playlists with standard Extended M3U and M3U8 import/export
• Smart folder and call recording / short voice note filters
• Advanced A-B looping and named loop presets
• Scheduled Playback sessions with durable recovery
• Online synchronized lyrics via LRCLIB and community internet radio via Radio Browser
• Android Auto media hierarchy integration for car head units
• Automatic in-app update checks and gentle reminder notifications
Feature availability and performance may vary by Android version, hardware, permissions, device settings, and App version.

3. LIMITED LICENSE
Subject to these Terms, the Developer grants you a limited, non-exclusive, non-transferable, revocable license to install and use the App for lawful, personal, non-commercial purposes. You may not redistribute, sell, impersonate, or commercially exploit the App without permission, except as permitted by applicable law or applicable third-party open-source licenses (GPLv3).

4. OWNERSHIP AND THIRD-PARTY COMPONENTS
The Developer retains rights in the App's original code, branding, triple-arch mark, and original materials. Open-source components are provided under their respective licenses (e.g. AndroidX Media3, Google Jetpack Compose, Coil). These Terms do not transfer ownership of your audio files or third-party content to the Developer.

5. YOUR AUDIO FILES AND COPYRIGHT
The App is a local media player, not a supplier of music licenses or copyrighted recordings. You are responsible for using audio you are legally entitled to access and for complying with applicable copyright law. The App reads and organizes existing media without intentionally altering original audio files; however, you should keep independent backups of important files.

6. PERMISSIONS AND DEVICE INTEGRATION
Depending on your Android version, the App requests permissions required to deliver its features:
• Music and Audio: Required to discover and play local music files.
• Notifications: For lock-screen media controls, background playback, and update reminders.
• Alarms and Background Execution: For scheduled playback sessions.
• Audio Recording / Microphone: Strictly used on-device for the live audio visualizer frequency spectrum. No audio is ever recorded or uploaded.
You can manage permissions in Android Settings at any time.

7. SCHEDULED PLAYBACK LIMITATIONS
Scheduled playback is a convenience feature, NOT a safety-critical alarm or guaranteed wake-up service. Android battery optimizations, low battery, device restarts, or silent modes can affect timing. Do not rely on scheduled playback for life-critical, medical, or legal obligations.

8. IN-APP UPDATES
The App includes an automated update check that queries public repository releases (GitHub / Fastly CDN) once every 24 hours to inform you of newer versions. Downloads occur only upon explicit user confirmation.

9. DISCLAIMER OF WARRANTIES
The App is provided "as is" and "as available" without warranties of uninterrupted operation or fitness for a particular purpose, to the maximum extent permitted by applicable law.

10. LIMITATION OF LIABILITY
To the maximum extent permitted by applicable law, the Developer is not liable for indirect, incidental, or consequential damages resulting from use of the App.

11. GOVERNING LAW
These Terms are governed by the laws of Bangladesh, without prejudice to mandatory local consumer protection regulations.

12. CONTACT
Tanjil Hasan Himel — taanjilhasan@gmail.com
GitHub: https://github.com/TanjilHasan-Himel

© 2026 Tanjil Hasan Himel. All rights reserved in original materials.
"""

    const val PRIVACY_POLICY = """
PRIVACY POLICY
Audia Player | Version 9.1.3
Developer: Tanjil Hasan Himel
Last Updated: November 18, 2026
Privacy Contact: taanjilhasan@gmail.com
GitHub: https://github.com/TanjilHasan-Himel

1. OVERVIEW
Audia Player is an offline-first Android music player. Your personal audio files, listening history, and local playlists remain strictly on your device. We do not sell, rent, or profile your personal data.

2. INFORMATION PROCESSED ON YOUR DEVICE
• Media Files & Metadata: The App scans local audio files (titles, artists, albums, duration, artwork) to build your offline library.
• Preferences & Playlists: Custom playlists, favorites, listening statistics, loop presets, and settings are stored locally in Room database and DataStore.
• Audio Visualizer: The visualizer processes live audio frequency data in device memory. No audio is recorded, saved, or uploaded.

3. NETWORK COMMUNICATIONS & THIRD-PARTY SERVICES
The App connects to external servers only for specific, user-beneficial features:
• In-App Updates: Checks Fastly CDN (raw.githubusercontent.com) and GitHub API for version.json every 24 hours. No personal user data is sent; only standard HTTP GET requests are performed.
• Synced Lyrics (LRCLIB): When lyrics are requested, the song title, artist, and duration are sent to lrclib.net over HTTPS.
• Internet Radio (Radio Browser): Radio station queries and click counting connect to community Radio Browser mirrors and direct station stream hosts over secure connections where available.
• Diagnostics & Telemetry (Firebase): To improve app stability, Firebase Crashlytics collects anonymous crash reports and Firebase Analytics tracks non-identifying aggregated events. Users can disable Analytics and Crashlytics at any time in the in-app Settings dialog.

4. PERMISSIONS EXPLAINED
• READ_MEDIA_AUDIO / READ_EXTERNAL_STORAGE: Access local music files.
• POST_NOTIFICATIONS: Display lock-screen playback controls and update alerts.
• SCHEDULE_EXACT_ALARM: Trigger user-configured scheduled playback sessions.
• RECORD_AUDIO: Read output audio frequency waves for the live visualizer.

5. DATA RETENTION AND DELETION
All local data (playlists, preferences, history) is deleted when you clear app data or uninstall the App. External services (LRCLIB, Radio Browser, Firebase) retain connection logs according to their independent privacy policies.

6. CHILDREN'S PRIVACY
Audia Player is a general-audience music player and does not knowingly collect data from children under 13.

7. YOUR CHOICES AND CONTROL
You can revoke permissions in Android Settings, toggle off Crashlytics and Analytics in App Settings, or clear app data at any time.

8. POLICY UPDATES
Any updates to this policy will be reflected with a revised version and date within the App.

9. CONTACT
Tanjil Hasan Himel
Email: taanjilhasan@gmail.com
GitHub: https://github.com/TanjilHasan-Himel

© 2026 Tanjil Hasan Himel. All rights reserved.
"""
}
