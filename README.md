# Pocket Audiobook 🎧📖

**Pocket Audiobook** is a modern Android application built with **Jetpack Compose** that transforms any PDF file into an audiobook with natural text-to-speech (TTS) synthesis and background audio playback.

---

## 📥 Download APK

You can download the pre-compiled APK directly and install it on your Android phone:

👉 **[Download PocketAudiobook.apk](apk/PocketAudiobook.apk?raw=true)**

> **Installation Tip:** When installing the APK, if Android shows a warning about installing from an unknown source, tap **Settings** and toggle **Allow from this source**.

---

## ✨ Features

- 📄 **PDF to Speech**: Import and extract text page-by-page from PDF files using `pdfbox-android`.
- 🗣️ **Customizable TTS Voices**:
  - Choose from any installed system TTS voice.
  - Adjust speech rate, pitch, and chunking for smooth listening.
- 🎵 **Background Playback & Lock Screen Controls**:
  - Powered by **AndroidX Media3 ExoPlayer** and a foreground service.
  - Keep listening even when your screen is locked or the app is minimized.
  - Control playback (play/pause/skip) directly from the notification shade and lock screen.
- 📚 **Library & Reading History**:
  - Save books to your personal library.
  - Automatically resume listening exactly where you left off.
  - Track listening history and progress.
- 🎨 **Modern Material 3 UI**:
  - Clean, fluid interface built 100% with Jetpack Compose.
  - Edge-to-edge design with dynamic light & dark theme support.

---

## 🛠️ Tech Stack

- **Language:** [Kotlin](https://kotlinlang.org/)
- **UI Framework:** [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material Design 3
- **Audio & Media:** [AndroidX Media3 (ExoPlayer & MediaSession)](https://developer.android.com/media/media3)
- **PDF Extraction:** [PdfBox-Android](https://github.com/BirdBrainSoftware/PdfBox-Android)
- **Architecture:** MVVM (Model-View-ViewModel) + StateFlow / Coroutines
- **Build System:** Gradle (Kotlin DSL `.kts`)

---

## 📱 System Requirements

- **Minimum SDK:** Android 7.0 (API Level 24)
- **Target SDK:** Android 15 / 16 (API Level 37)
- **JDK:** Java 11 or higher

---

## 🚀 Building from Source

### Using Android Studio
1. Clone the repository:
   ```bash
   git clone https://github.com/GOWTHAM2036/PocketAudio.git
   ```
2. Open the project folder in **Android Studio** (Koala / Ladybug or newer recommended).
3. Allow Gradle to sync dependencies.
4. Select your connected device or emulator and press **Run** (or `Shift + F10`).

### Using Command Line (Gradle)
```bash
# Clone the repository
git clone https://github.com/GOWTHAM2036/PocketAudio.git
cd PocketAudio

# Build debug APK
./gradlew assembleDebug
```
The output APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 👤 Author

Developed by **Gowtham Reddy**  
- GitHub: [@GOWTHAM2036](https://github.com/GOWTHAM2036)
