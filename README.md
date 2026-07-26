# 🎵 AderSic - AndroidMusic

A modern, high-performance, and responsive local music and video player for Android. Built with 100% Kotlin and the latest Jetpack libraries.

---

## 🌟 Key Features

### 🔐 Authentication & Security
- **Smart Login/Register**: Minimalist username and password system.
- **Guest Access**: Instantly browse and play without creating an account.
- **Session Persistence**: Automatic reconnection using **Jetpack DataStore**.

### 🎧 Superior Playback Experience
- **Media3 (ExoPlayer)**: Robust background playback and notification controls.
- **Audio & Video Support**: Seamlessly switch between local music and video files.
- **Dynamic Queue (Up Next)**: Real-time **Live-Swap Drag & Drop** for hundreds of items with smooth animations.
- **Volume & Progress**: Custom "YouTube-style" vertical volume popup and precision seek bars.

### 📊 Insights & Stats
- **Animated Dashboards**: Beautiful Canvas-based charts (Circular gauges & Bar charts).
- **Listening History**: Keep track of your most played songs and total listening time.

### 🚀 UI/UX Optimizations
- **Fast Scrollbar**: Interactive A-Z/Date indicators for massive libraries (800+ songs).
- **Dark Blue Theme**: Modern, elegant, and eye-friendly dark blue aesthetic.
- **Multi-language**: Full support for English (EN) and French (FR).

---

## 🛠 Tech Stack

- **UI**: Jetpack Compose (Material 3)
- **Database**: Room (SQLite)
- **Session**: DataStore Preferences
- **Media Engine**: Android Media3 (ExoPlayer & MediaSession)
- **Architecture**: Clean Code with ViewBinding for native RecyclerView performance.
- **Language**: 100% Kotlin (Coroutine & Flow)

---

## 🏗 Compilation Guide (For Developers)

> [!IMPORTANT]
> This repository is a **Source-Only (100% Kotlin)** project to showcase logic and architecture. XML resources and the Android Manifest are excluded from the main branch to focus on the Kotlin code.

To compile and run this application locally, follow these steps:

1. **Initialize Project**: Create a new Empty Compose Activity project in Android Studio.
2. **Setup Dependencies**: Copy the dependencies from the `.kts` files in this repository to your `build.gradle.kts`.
3. **Merge Source Code**:
   - Clone this repository.
   - Copy the `com/example/myapplication` folder content into your `src/main/java` directory.
4. **Resources & Manifest**:
   - Manually define your `AndroidManifest.xml` with required permissions (`READ_MEDIA_AUDIO`, `READ_MEDIA_VIDEO`, `POST_NOTIFICATIONS`).
   - Create the necessary XML layouts (e.g., `item_queue.xml`) as described in the code comments or source fragments.
5. **Sync & Run**: Run a Gradle Sync and deploy to your device.

---

## 👤 Author

Developed by **AvotraAder**
- **GitHub**: [@AvotraAder](https://github.com/AvotraAder)
- **Email**: andriniainavotraader@gmail.com

---
*If you find this project helpful, don't forget to give it a ⭐!*
