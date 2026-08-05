# 🎉 PlayTime Music Player v8.0.0 (Major Release)

We are incredibly proud to announce the release of **PlayTime Music Player v8.0.0**! 🎵

This is a milestone release that brings together months of development, culminating in a flagship-quality, production-ready Android music player. Version 8.0.0 includes our completely functional **Auto-Play Scheduler USP**, a stunning Glassmorphism UI, and robust background playback powered by modern Android architecture.

---

## 🌟 Key Features & Highlights

### ⏰ The Auto-Play Scheduler (Our USP!)
Never miss a beat or an important moment again. PlayTime isn't just a music player; it's your musical alarm clock.
*   **Exact Alarm Delivery:** Set a time, pick a song, and PlayTime will wake up your device and start playing.
*   **Smart Volume Override:** Alarms automatically boost volume up to 80% to ensure you hear them.
*   **Boot Persistence:** Your scheduled alarms survive device reboots automatically.
*   **WakeLock Support:** The app reliably wakes your phone from deep sleep when the scheduled time arrives.

### 🎨 Stunning Premium Design
*   **OLED Dark Theme:** Deep blacks (`#121212`) save battery on AMOLED screens and look incredibly sleek.
*   **Glassmorphism UI:** Beautiful, blurred, semi-transparent overlays and a sliding Mini Player that feels premium.
*   **Dynamic Colors:** The UI extracts colors from the currently playing Album Art to dynamically theme the Player Screen.
*   **Spinning Vinyl Art:** A classic touch with smooth, rotating album art during playback.

### 🎧 Core Playback Engine
*   **Powered by Media3 (ExoPlayer):** The industry standard for robust, gapless audio playback.
*   **Background Playback & Lockscreen Controls:** Full integration with Android's media session, allowing you to control music from anywhere.
*   **Smart Audio Focus:** Automatically pauses when you get a call, and handles headphone disconnections gracefully.
*   **A-B Loop (Loop+):** Create custom A-B loops to repeat your favorite guitar solo or chorus, and save them as presets!
*   **Audio Effects:** Built-in Equalizer and Bass Boost support.

---

## ✨ What's New in v8.0.0

While this release celebrates the complete app, we've also added some final polish in this specific update:
*   **Album Art Layout Fix:** Fixed an issue where the album art placeholder displayed with black borders on the Player Screen. It now perfectly scales (`ContentScale.Crop`) to fill the container beautifully.
*   **Enhanced Privacy (No Microphone Needed):** We listened to user feedback! The audio visualizer feature has been removed. The app **no longer requests the Microphone (`RECORD_AUDIO`) permission**, ensuring complete privacy and a faster, cleaner onboarding experience.

---

## 🛠️ Architecture & Under the Hood
For the developers out there, PlayTime is built using the absolute latest Android development standards:
*   **100% Kotlin & Jetpack Compose** for a modern, reactive UI.
*   **MVVM Architecture** heavily relying on `StateFlow` and Coroutines.
*   **Hilt Dependency Injection** for clean, scalable code.
*   **Room Database** for blazing-fast local storage of favorites and scheduled tasks.

---

**Enjoying PlayTime?** Drop a ⭐ on the repository, let us know your thoughts, or report any issues! Happy listening! 🎧
