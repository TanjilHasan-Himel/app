package com.eplaytime.app.util

/**
 * Audia Player legal text — review draft, 2026-11-08.
 *
 * IMPORTANT BEFORE SHIPPING:
 * 1) Verify ALL factual statements against the release APK/AAB, merged manifest,
 *    dependencies/SDKs, actual network traffic, and Android backup rules.
 * 2) This draft assumes a local-first player with no developer-operated cloud,
 *    advertising, analytics, or crash-reporting SDKs.
 * 3) If network lyrics, Shazam, Firebase, cloud sync, or other SDKs are enabled,
 *    update the Privacy Policy, Play Data Safety answers, and consent flows FIRST.
 * 4) Obtain local legal review before relying on these terms for legal protection.
 */
object Legals {

    const val EULA = """
END USER LICENSE AGREEMENT AND TERMS OF USE
Audia Player | Version 8.0.0
Developer: Tanjil Hasan Himel
Last Updated: November 8, 2026
Contact: taanjilhasan@gmail.com
GitHub: https://github.com/TanjilHasan-Himel

1. ACCEPTANCE AND SCOPE
These Terms govern your use of Audia Player (the "App"), an Android application developed by Tanjil Hasan Himel (the "Developer"). By installing or using the App, you agree to these Terms to the extent permitted by applicable law. If you disagree, please stop using the App. Nothing in these Terms limits rights that cannot legally be waived.

2. WHAT THE APP DOES
Audia Player is designed to discover, organize, and play audio already accessible on your device. Features may include local playlists, favorites, smart folder filtering, background playback, and scheduled playback. Feature availability and performance may vary by Android version, hardware, permissions, device settings, and App version.

3. LIMITED LICENSE
Subject to these Terms, the Developer grants you a limited, non-exclusive, non-transferable, revocable license to install and use the App for lawful, personal, non-commercial purposes. You may not redistribute, sell, impersonate, or commercially exploit the App without permission, except as permitted by applicable law or applicable third-party licenses. Restrictions on reverse engineering or modification apply only to the extent permitted by law.

4. OWNERSHIP AND THIRD-PARTY COMPONENTS
The Developer retains rights in the App's original code, branding, design, and original materials. Some components may be provided under separate open-source or third-party licenses; those licenses govern the relevant components and take precedence where applicable. These Terms do not transfer ownership of your audio files or third-party content to the Developer.

5. YOUR AUDIO FILES AND COPYRIGHT
The App is a local media player, not a supplier of music licenses or copyrighted recordings. You are responsible for using audio you are legally entitled to access and for complying with applicable copyright law. You do not need to own copyright in every recording you lawfully listen to. The App is designed to read and organize existing media without intentionally altering the original audio files; however, you should keep independent backups of important files. Media scanning or filtering may occasionally classify a file incorrectly or omit it from the displayed library.

6. PERMISSIONS AND DEVICE INTEGRATION
Depending on the Android version and features used, the App may request access to local audio, notification permissions, foreground media playback, or special access for scheduling alarms. You can manage supported permissions through Android Settings. Some features may not work when permissions are denied or revoked. System services, connected accessories, audio focus, and other apps may affect playback.

7. SCHEDULED PLAYBACK AND ALARM LIMITATIONS
Scheduled playback is a convenience feature, NOT a safety-critical alarm or guaranteed wake-up service. The App attempts to start playback at the time you choose, but delivery and audible playback can be delayed or prevented by Android restrictions, revoked permissions, battery optimization, device shutdown, force-stop, low battery, Do Not Disturb, silent/volume settings, missing or moved audio files, Bluetooth/audio routing, restarts, or device-specific behavior. Exact timing and successful playback are not guaranteed. Do not rely on the feature as your sole reminder for medical, travel, examination, employment, emergency, or other time-critical obligations. Use a dedicated alarm and a backup when timing is important.

8. AVAILABILITY, UPDATES, AND SUPPORT
The Developer may improve, modify, suspend, or discontinue features or support, subject to applicable law. Changes to the App, Android, or device settings may affect compatibility. The Developer does not guarantee uninterrupted availability or future updates. Material changes to these Terms will be communicated through an appropriate App notice, release notes, or an updated document where reasonably practicable.

9. DISCLAIMER OF WARRANTIES
To the maximum extent permitted by applicable law, the App is provided "as is" and "as available" without warranties of uninterrupted operation, exact alarm delivery, perfect media detection, error-free playback, or fitness for a particular purpose. Nothing in these Terms excludes a warranty or consumer right that applicable law does not permit to be excluded.

10. LIMITATION OF LIABILITY
To the maximum extent permitted by applicable law, the Developer is not liable for indirect, incidental, special, or consequential losses arising from use or inability to use the App, including missed schedules or loss of locally stored App settings. This provision does not exclude liability that cannot legally be limited or excluded, including liability for fraud or other non-excludable conduct under applicable law. Keep backups of important files and do not rely solely on scheduled playback for critical activities.

11. RESPONSIBLE USE
You agree not to misuse the App, attempt to compromise other users or systems through it, violate intellectual property rights, or use it in a way prohibited by applicable law. Where legally permitted, the Developer may stop providing access to versions or services that are misused.

12. PRIVACY
The App's handling of data is described separately in the Privacy Policy available within the App. That policy should be read together with these Terms. If the App adds online services or third-party analytics in the future, the relevant disclosures and any required choices will be updated before they are enabled.

13. SEVERABILITY AND GOVERNING LAW
If any provision is unenforceable, the remainder stays effective to the extent permitted by law. These Terms are governed by the laws of Bangladesh, subject to any mandatory consumer-protection or other applicable laws. Disputes may be brought before a competent court with lawful jurisdiction; this clause does not remove non-waivable rights.

14. CONTACT
For support, privacy, copyright, or legal inquiries, contact:
Tanjil Hasan Himel — taanjilhasan@gmail.com
GitHub: https://github.com/TanjilHasan-Himel

© 2026 Tanjil Hasan Himel. All rights reserved in original materials.
"""

    const val PRIVACY_POLICY = """
PRIVACY POLICY
Audia Player | Version 8.0.0
Developer: Tanjil Hasan Himel
Last Updated: November 8, 2026
Privacy Contact: taanjilhasan@gmail.com
GitHub: https://github.com/TanjilHasan-Himel

1. OVERVIEW
Audia Player is designed as a local-first Android audio player. In the version described by this policy, core library, playback, favorites, playlists, and schedule functions operate on the device without a Developer-operated account or cloud service. The App is not designed to sell or profile your listening history. The disclosures below must match the actual App build, including any third-party software components.

2. INFORMATION ACCESSED AND PROCESSED ON YOUR DEVICE
With the permissions available to it, the App may read media information such as audio file names, titles, artists, albums, durations, artwork, and file locations or identifiers to display and play local audio. It may store local preferences, favorites, playlists, listening counts, filters, and scheduled playback settings. These records are intended for on-device functionality and are stored in App-managed storage. The App does not require an account, email address, or telephone number for its core local player functions.

3. DATA SENT TO THE DEVELOPER
The local-player version described here does not intentionally upload your audio files, file paths, playlists, listening history, or schedule details to a Developer-operated server. It is not configured to use Developer-operated user analytics, advertising networks, or remote crash-report collection. This statement applies only after verification of the released App and all included third-party libraries. Android, your device manufacturer, the app store, and any enabled device backup service may independently process device or diagnostic information under their own policies.

4. PERMISSIONS AND PURPOSES
The App may request or use the following, as applicable to your Android version and selected features:
• Music/audio access: discover and play audio available on the device.
• Foreground media playback: continue eligible audio playback while the App is in the background and display playback controls.
• Notifications: show playback-related controls and notices when supported and permitted by the system.
• Alarms and reminders special access: attempt to trigger user-configured scheduled playback at requested times; Android may require separate approval.
• Device startup or wake-related access: only if implemented, to restore eligible scheduling behavior after a restart or during sleep.
Permissions may be denied or changed in Android Settings. Denial can limit the related feature. The App should not request access unrelated to an enabled feature.

5. LOCAL STORAGE, BACKUP, AND DELETION
Preferences, playlists, favorites, listening counts, and scheduling configurations are normally stored in the App's local storage. Clearing App data or uninstalling the App usually removes its local private data from that device. However, Android or device-manufacturer backup and transfer systems may preserve or restore certain App data, depending on your settings and the App's backup configuration. The Developer does not have a server-side copy of the local library in the version covered by this policy. Original audio files stored outside the App remain under your control and are not ordinarily deleted by uninstalling Audia Player.

6. SECURITY
The App relies on Android application isolation and platform permission controls for access to locally stored data. No software or device can be guaranteed completely secure against loss, unauthorized access, malware, or hardware failure. Protect your device with a secure screen lock and keep important audio and settings backed up through a method you trust.

7. ADVERTISING, ANALYTICS, AND THIRD-PARTY SERVICES
The local-player version described here does not intentionally show third-party advertisements or include a Developer-operated behavioral analytics service. If a future update introduces Firebase, Crashlytics, online lyrics, music identification, streaming, social functions, cloud sync, or any other online service, this policy and any required in-App disclosures or consent mechanisms must be updated BEFORE enabling that functionality. Information sent to external providers and their retention practices will be described at that time.

8. CHILDREN
The App is a general-purpose local audio player and is not specifically designed or marketed to children. The Developer does not knowingly solicit children's personal information through a registration system. Parents or guardians should manage device permissions and the content available to children. Any distribution specifically directed at children will be reviewed for additional applicable legal and store requirements.

9. YOUR CHOICES AND CONTROL
You can manage the App's media and notification permissions in Android Settings, review or remove locally stored playlists and schedules where the App supports this, clear App storage, or uninstall the App. These actions may remove local preferences or stop related functions. For questions about privacy or data handling, contact the address below.

10. POLICY CHANGES
This policy may be updated to reflect actual App behavior, new features, software dependencies, or legal requirements. The effective date above will be changed when a revised policy is published. Where appropriate, significant changes will be communicated in the App or release notes before the new data processing begins.

11. CONTACT
Tanjil Hasan Himel
Email: taanjilhasan@gmail.com
GitHub: https://github.com/TanjilHasan-Himel

© 2026 Tanjil Hasan Himel. All rights reserved.
"""
}
