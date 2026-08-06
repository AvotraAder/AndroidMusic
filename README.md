# 🎵 AderSic - AndroidMusic

A modern, high-performance, and responsive local music and video player for Android. Built with 100% Kotlin and the latest Jetpack libraries, featuring advanced real-time audio visualization.

---

## 🌟 Key Features

### 🔐 Authentication & Security
- **Smart Login/Register**: Minimalist username and password system.
- **Guest Access**: Instantly browse and play without creating an account.
- **Session Persistence**: Automatic reconnection using **Jetpack DataStore**.

### 🎧 Elite Playback & Visualization
- **Real-Time FFT Visualizers**: High-fidelity audio analysis using Android's `Visualizer` API.
- **Circular "Orbital" Visualizer**: 360° frequency display (Bass, Mids, Highs) surrounding a rotating vinyl-style album artwork.
- **Horizontal "Bowtie" Bass Visualizer**: Center-aligned bass-only pulse with 60% width and dynamic "Butterfly" shape.
- **Dynamic RGB Theme**: Smooth color-cycling (Rainbow spectrum) across the entire UI in Dark Mode.
- **Album Artwork Integration**: Automatic extraction and smooth loading of metadata covers using **Coil**.

### 📋 Advanced Queue Management (Up Next)
- **Live-Swap Drag & Drop**: Real-time physical item swapping with smooth animations for up to 800+ tracks.
- **Native RecyclerView Engine**: Ultra-high performance using ViewBinding for heavy lists.
- **Turbo Auto-Scroll**: Intelligent acceleration when dragging items to screen edges.

### 📊 Dashboard & Insights
- **Animated Statistics**: Beautiful Canvas charts for total listening time and top played tracks.
- **Interactive UI**: "YouTube-style" vertical volume popups and precision progress seek bars.
- **Fast Scrollbar**: A-Z and Date indicators for easy navigation in massive libraries.

---

## 🛠 Tech Stack

- **UI**: Jetpack Compose (Material 3) & native RecyclerView
- **Graphics**: Hardware-accelerated Canvas animations
- **Database**: Room (SQLite)
- **Session**: DataStore Preferences
- **Media Engine**: Android Media3 (ExoPlayer & MediaSession)
- **Image Loading**: Coil (Asynchronous & Caching)
- **Language**: 100% Kotlin (Coroutines, Flow, State)

---

## 🏗 Compilation Guide (For Developers)

> [!IMPORTANT]
> This repository is a **Source-Only (100% Kotlin)** project to showcase logic and architecture. XML resources and the Android Manifest are excluded from the main branch to focus on the Kotlin code.

To compile and run this application locally, follow these steps:

1. **Initialize Project**: Create a new Empty Compose Activity project in Android Studio.
2. **Setup Dependencies**: Copy the dependencies (Media3, Coil, Room, DataStore) from the `.kts` files in this repository.
3. **Merge Source Code**:
   - Clone this repository.
   - Copy the `com/example/myapplication` folder content into your `src/main/java` directory.
4. **Resources & Manifest**:
   - Manually define your `AndroidManifest.xml` with required permissions (`RECORD_AUDIO`, `READ_MEDIA_AUDIO`, `READ_MEDIA_VIDEO`, `POST_NOTIFICATIONS`).
   - Create the necessary XML layouts (e.g., `item_queue.xml`) as referenced in the `AndroidView` components.
5. **Sync & Run**: Run a Gradle Sync and deploy to your device.

---

## 👤 Author

Developed by **AvotraAder**
- **GitHub**: [@AvotraAder](https://github.com/AvotraAder)
- **Email**: andriniainaavotraader@gmail.com
  Download App here => https://drive.google.com/file/d/1HmAZHtmlm_o9QOxKDI5HPCrBbV6zphie/view?usp=sharing
---
*If you find this project helpful, don't forget to give it a ⭐!*
