<p align="center">
  <img src="branding/audia-player-logo.png" alt="Audia Player Logo" width="128" height="128">
</p>

<h1 align="center">Audia Player</h1>

<p align="center">
  <strong>A modern, privacy-first, audiophile music player built with Jetpack Compose & Media3.</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Version-v9.0.77-gold?style=for-the-badge" alt="Version">
  <img src="https://img.shields.io/badge/Platform-Android_8.0+-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android">
  <img src="https://img.shields.io/badge/UI-Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Compose">
  <img src="https://img.shields.io/badge/Engine-Media3_ExoPlayer-FF6F00?style=for-the-badge" alt="Media3">
  <img src="https://img.shields.io/badge/Privacy-100%25_Offline-00E676?style=for-the-badge" alt="Privacy">
  <img src="https://img.shields.io/badge/Ads-Zero_Ads-red?style=for-the-badge" alt="Zero Ads">
</p>

---

## 🌟 Overview

**Audia Player** is an advanced, high-performance offline music player engineered for modern Android devices. Designed from the ground up with **Jetpack Compose 1.7+** and **AndroidX Media3 (ExoPlayer)**, Audia combines responsive, fluid glassmorphic visuals with audiophile-grade audio playback, real-time synchronized lyrics, and comprehensive audio workstation tools.

---

## ✨ Key Features

### 🎨 Fluid Stage & Dynamic Visuals
- **Dynamic Ambient Bloom:** Album-art reactive fluid ambient lighting that dances with playback.
- **Real-Time FFT Visualizer:** Audio-frequency reactive lighting powered by native FFT bass and energy analysis.
- **Custom Arch Morph Button:** Distinctive Arch Play/Pause button that smoothly morphs into a circle during active playback.
- **Adaptive Multi-Screen Layout:** Responsive layout architecture that scales gracefully across compact screens (16:9, 18:9), modern tall aspect ratios, foldable devices, and 3-button navigation.

### 📜 Real-Time Synced Lyrics
- **LRCLIB Integration:** Ultra-fast, millisecond-accurate synchronized karaoke lyrics.
- **Zero-Shift 48dp Slot:** Deterministic lyrics preview stage prevents layout jumping between loading, synced lines, and instrumental breaks.
- **Immersive Lyrics Stage:** Tap-to-expand edge-to-edge full lyrics mode with auto-scroll and manual seek synchronization.
- **Offline Cache:** Powered by Room Database for instant offline lyrics retrieval.

### 🔁 Precision A-B Looper
- **Musician-Grade Looping:** Set millisecond-precise loop points (A → B) on any track.
- **Named Loop Presets:** Save, name, and recall custom loop sections (choruses, guitar solos, dance routines) permanently in local storage.

### 🛠️ Song Garage & Audio Inspector
- **Lossless Quality Badge:** Automatic detection and badge display for FLAC, Hi-Res 24-bit/96kHz, WAV, AAC, and MP3.
- **Audio Inspector:** View bitrate, sample rate, bit depth, channel configuration, and container specs.
- **Built-in Audio Trimmer & Tag Editor:** Edit ID3 tags, album art, and trim audio segments on-device.

### 🎚️ Sound Processing & Effects
- **10-Band Graphic Equalizer** with genre presets and custom curve saving.
- **Bass Boost & Virtualizer:** Deep low-end acoustic amplification.
- **Loudness Enhancer:** Crisp acoustic clarity.

### 🌙 Zen Bedtime Mode & Smart Sleep
- **Zen Ambient View:** Distraction-free, minimal night listening screen.
- **Smart Sleep Timer:** Gentle audio volume fade-out with optional "Stop after current track" toggle.

---

## 📱 Tech Stack & Architecture

- **UI:** Jetpack Compose, Material 3, Compose Animations, Custom Canvas Drawing
- **Audio Engine:** AndroidX Media3 (`MediaSessionService`, `ExoPlayer`, `MediaBrowserCompat`)
- **Architecture:** Clean Architecture + MVI/MVVM with Kotlin Coroutines & `StateFlow`
- **Dependency Injection:** Dagger Hilt
- **Local Persistence:** Room Database (Lyrics, Loop Presets, Song Wiki, Favorites) + DataStore Preferences
- **Image Loading:** Coil 2.6+ with Palette extraction
- **Background Tasks:** AndroidX WorkManager (Library scanner, periodic cleanup)

---

## 🚀 Building from Source

### Prerequisites
- Android Studio Ladybug / Meerkat (or newer)
- JDK 17 or higher
- Android SDK 35 (compileSdk: 35, minSdk: 26)

### Steps
1. Clone the repository:
   ```bash
   git clone https://github.com/TanjilHasan-Himel/playtime-musicapp.git
   cd playtime-musicapp
   ```
2. Build the debug APK:
   ```bash
   ./gradlew assembleDebug
   ```
3. Build the release APK / Bundle:
   ```bash
   ./gradlew assembleRelease
   ./gradlew bundleRelease
   ```

---

## 🔒 Privacy & Permissions

- **100% Offline-First:** Audio playback, metadata, and database operations happen strictly on-device.
- **No Telemetry / No Ads:** Zero analytics trackers, zero advertising SDKs.
- **Permissions Used:**
  - `READ_MEDIA_AUDIO` / `READ_EXTERNAL_STORAGE`: To index and play your local audio files.
  - `POST_NOTIFICATIONS`: For the persistent Media3 media playback notification and controls.
  - `FOREGROUND_SERVICE_MEDIA_PLAYBACK`: For uninterrupted background audio playback.
  - `INTERNET`: Exclusively for optional on-demand synced lyrics retrieval from open public lyrics APIs.

---

<p align="center">
  Crafted with care by <strong>Tanjil Hasan Himel</strong>
</p>
