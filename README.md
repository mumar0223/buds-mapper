# Buds Mapper

Buds Mapper is a lightweight, privacy-focused native utility designed to remap Bluetooth media commands from wireless earbuds into custom system and application actions (such as scrolling feeds or swiping Shorts and Reels).

Originally developed for Android with native Samsung One UI audio optimization, Buds Mapper is structured as a modern monorepo expanding toward cross-platform support.

---

## Target Hardware & Compatibility

* **Earbuds**: 
  * Samsung Galaxy Buds series (Galaxy Buds Core, Buds FE, Buds2, Buds2 Pro, Buds3, Buds Live).
  * Any wireless Bluetooth earbuds supporting standard Bluetooth AVRCP media commands (`PLAY`, `PAUSE`, `NEXT_TRACK`, `PREVIOUS_TRACK`).
* **Smartphones**:
  * Optimized and verified on Samsung Galaxy smartphones running Android 8.0 (API 26) through Android 15 (One UI 6.1+).
  * *Initial hardware reference device: Samsung Galaxy A35 5G.*
* **Other Phone Brands**:
  * Devices from other manufacturers (Google Pixel, OnePlus, Xiaomi, Motorola, etc.) have not been exhaustively tested yet. Compatibility depends on how the OEM's audio subsystem routes Bluetooth media buttons.
  * **Feedback & Reports**: If you test Buds Mapper on another brand of smartphone, we warmly appreciate your testing feedback and issue reports!

---

## Core Features

### 1. Zero-Timer Dual-Track Priority Engine
* Uses alternating sub-audible 16-bit PCM AudioTracks on `USAGE_MEDIA` to cleanly claim priority index 0 in the system's `MediaSessionStack`.
* Eliminates polling loops and millisecond timers, ensuring instant earbud gesture response with 0.0% idle CPU overhead.
* Features automatic background silence detection prevention tuned for Samsung One UI AudioFlinger.

### 2. Per-App Custom Profiles
* Define custom earbud tap mappings per application (e.g. YouTube, Instagram, Chrome, TikTok, Reddit, or any installed app).
* Remap earbud gestures to:
  * **Swipe Up**: Instantly scroll to the next Short, Reel, or TikTok.
  * **Swipe Down**: Return to the previous Short, Reel, or TikTok.
  * **Scroll Down / Scroll Up**: Smooth page and article scrolling.
  * **Play / Pause**: Toggle media playback.
  * **Default Media Behavior**: Standard pass-through audio command.
  * **Do Nothing**: Consume the gesture silently.

### 3. Dynamic Auto-Engagement
* The background media capture engine automatically engages **only** when an app with an active profile is in the foreground.
* Instantly releases media capture when switching to unmapped apps or returning to the home screen, keeping the phone's normal media player routing completely unaffected.

### 4. Modern Cyber Glassmorphism UI
* **Samsung Clock-Inspired Ambient Aura**: Smooth, floating 120Hz breathing background aura across both Dark and Light modes.
* **Game Space Frosted Cards**: Translucent glass cards with customizable neon glow borders.
* **Theme Engine (5 Accents)**:
  * **Cyber Violet** *(Default)*: Signature neon purple aesthetic matching the app launcher icon.
  * **Cyber Ice Blue**: Icy periwinkle neon theme.
  * **Hyper Crimson Red**: Aggressive red gaming palette with Lucide flame styling.
  * **Neon Cyber Mint**: Futuristic emerald teal theme.
  * **Fusion Synergy**: Harmonized multi-color gradient palette.

### 5. AndroidX Splash Screen
* Seamless cold-boot splash screen with `#0C0A14` deep violet canvas and centered glowing Bluetooth emblem.
* Eliminates white screen flash on app launch before transitioning into Compose.

### 6. Test & Capture Diagnostics Mode
* Interactive real-time hardware telemetry monitor displaying key codes, device IDs, raw AVRCP payloads, and gesture detections.
* **Smart Auto-Power**: Automatically activates the background engine when testing if currently turned off, and restores the original power state upon exiting.

---

## Troubleshooting: YouTube Player Overlay Controls

On Android, when *any* Accessibility Service is enabled, the YouTube app may automatically activate its built-in **Accessibility Player**, causing persistent Previous, Pause, and Next overlay buttons to appear over videos and Shorts.

To permanently turn off this overlay:
1. Open the **YouTube** app on your phone.
2. Tap your **Profile / You** icon in the bottom right corner.
3. Tap the **Settings** gear icon $\rightarrow$ **Accessibility**.
4. Toggle **Accessibility player** to **OFF**.

---

## Monorepo Project Structure

```text
BudsMapper/
│
├── apps/
│   ├── android/
│   │   └── app/
│   │       ├── src/main/
│   │       │   ├── java/com/budsmapper/app/
│   │       │   │   ├── MainActivity.kt
│   │       │   │   ├── accessibility/
│   │       │   │   │   └── BudsAccessibilityService.kt
│   │       │   │   ├── data/
│   │       │   │   │   ├── AppProfileStore.kt
│   │       │   │   │   ├── InstalledApp.kt
│   │       │   │   │   └── LastEventStore.kt
│   │       │   │   ├── media/
│   │       │   │   │   └── BudsMediaSessionManager.kt
│   │       │   │   ├── notification/
│   │       │   │   │   └── StatusNotificationManager.kt
│   │       │   │   ├── ui/
│   │       │   │   │   ├── BudsMapperApp.kt
│   │       │   │   │   ├── components/
│   │       │   │   │   ├── screens/
│   │       │   │   │   └── theme/
│   │       │   │   └── util/
│   │       │   │       └── AccessibilityUtils.kt
│   │       │   ├── res/
│   │       │   │   ├── drawable/
│   │       │   │   ├── mipmap-*/
│   │       │   │   ├── values/
│   │       │   │   └── xml/
│   │       │   └── AndroidManifest.xml
│   │       ├── build.gradle.kts
│   │       └── proguard-rules.pro
│   │
│   └── windows/          (Planned Compose Multiplatform Desktop app)
│
├── shared/               (Planned shared core logic)
│
├── build.gradle.kts      (Root Gradle build script)
├── settings.gradle.kts   (Monorepo project settings)
├── gradle.properties
├── gradlew
├── gradlew.bat
├── .gitignore
└── README.md
```

---

## Technology Stack

* **Platform**: Native Android (Kotlin)
* **Build System**: Gradle 9.7.1 with Kotlin DSL (`build.gradle.kts`)
* **Android Gradle Plugin (AGP)**: 9.5.1
* **Kotlin Compiler**: 2.4.21
* **UI Toolkit**: Jetpack Compose with Material 3
* **Compose BOM**: `2026.09.00`
* **Target SDK**: API 35 (Android 15)
* **Compile SDK**: API 37
* **Minimum SDK**: API 26 (Android 8.0 Oreo)
* **Bytecode Target**: Java 17

---

## Building from Source

To compile the debug APK using the Gradle wrapper:

### Windows (PowerShell / Command Prompt):
```powershell
.\gradlew.bat assembleDebug
```

### macOS / Linux:
```bash
./gradlew assembleDebug
```

### Output Binary Location:
```text
apps/android/app/build/outputs/apk/debug/app-debug.apk
```

---

## Installing & Testing via ADB

1. Connect your phone via USB with **USB Debugging** enabled:
   ```bash
   adb devices
   ```
2. Install the debug APK:
   ```bash
   adb install -r apps/android/app/build/outputs/apk/debug/app-debug.apk
   ```
3. Open **Buds Mapper** on your phone.
4. Enable the **Buds Mapper Accessibility Service** in your phone's Android Accessibility Settings.
5. Connect your Galaxy Buds or Bluetooth earbuds.
6. Toggle **Service Engine** ON to start remapping gestures.

---

## License

This project is licensed under the MIT License.
