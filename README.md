# Buds Mapper

Buds Mapper is a lightweight, privacy-conscious native Android application designed to diagnose and remap Bluetooth media commands from wireless earbuds into custom Android actions.

## Initial Target Hardware

* **Smartphone**: Samsung Galaxy A35 5G
* **Earbuds**: Samsung Galaxy Buds Core
* **Verified AVRCP Commands (via Bluetooth HCI Snoop & Wireshark)**:
  * `PLAY`
  * `PAUSE`
  * `FORWARD`
  * `BACKWARD`

## Milestone 1: Diagnostic Objective

Modern Android versions (particularly Samsung One UI) frequently route Bluetooth AVRCP commands via the media/audio subsystem (`AudioService` / `MediaSessionManager`) rather than dispatching them through standard system input pipelines.

The purpose of this milestone is **not** to implement gesture actions yet, but to **empirically determine whether Android dispatches these Bluetooth AVRCP media commands to an `AccessibilityService` as filterable `KeyEvent`s**.

* Key event filtering is requested via `FLAG_REQUEST_FILTER_KEY_EVENTS`.
* Incoming events are recorded to `LastEventStore`.
* Events are **never consumed** in this phase (`return false` from `onKeyEvent()`), preserving normal media playback routing.

---

## Modern Technology Stack (100% Stable 2026 Release)

* **Build System**: Gradle 9.7.1 with Kotlin DSL (`build.gradle.kts`)
* **Android Gradle Plugin (AGP)**: 9.5.1 Stable (Official built-in Kotlin integration)
* **Kotlin Compiler**: 2.4.21 Stable
* **Compose Compiler Plugin**: 2.4.21 (`org.jetbrains.kotlin.plugin.compose`)
* **UI Toolkit**: Jetpack Compose with Material 3
* **Compose BOM**: `2026.09.00` Stable
* **Compile SDK**: API 37 (`platforms/android-37.0`)
* **Target SDK**: API 35
* **Minimum SDK**: API 26 (Android 8.0 Oreo)
* **Bytecode Target**: Java 17
* **Package Name**: `com.budsmapper.app`
* **Dependencies**: Zero unnecessary third-party libraries, no Firebase, no Room, no DI frameworks, no continuous polling.

---

## Architecture

The application is strictly separated into two independent layers:

```
[ Galaxy Buds Core ] 
        │ (Bluetooth AVRCP: Play, Pause, Next, Prev)
        ▼
[ Samsung A35 5G (Android OS Input / Audio Subsystem) ]
        │
        ├─ (If routed as input KeyEvent) ──────────┐
        │                                          ▼
        │                          [ BudsAccessibilityService ]
        │                          - onKeyEvent(event)
        │                          - returns FALSE (passthrough)
        │                                          │
        │                                          ▼ (Persist)
        │                                  [ LastEventStore ]
        │                              (SharedPreferences / StateFlow)
        │                                          │
        │                                          ▼ (Read on Resume)
        │                                   [ HomeScreen UI ]
        │                           - Service Enabled/Disabled Status
        │                           - Command Monitor (Play/Pause/Next/Prev)
        │                           - Last Received Event Card
        ▼
[ Active Media Player / Foreground App (Unaffected) ]
```

### Layer A: Interactive UI Layer (Jetpack Compose)
* **`MainActivity.kt`**: Minimal launcher Activity host. Does not keep the background service alive.
* **`HomeScreen.kt`**: Material 3 dark-themed dashboard displaying:
  * Accessibility Service status with direct 1-tap navigation to Android Settings.
  * Earbud Command Monitor for `MEDIA_PLAY`, `MEDIA_PAUSE`, `MEDIA_NEXT`, and `MEDIA_PREVIOUS`.
  * Last Received Event telemetry (Key code, Key name, Action `DOWN`/`UP`, Timestamp, Device ID).
  * Diagnostic status explaining live hardware testing state without fake connections.

### Layer B: Background Service Layer (Kotlin & Android APIs)
* **`BudsAccessibilityService.kt`**: Subclass of native `AccessibilityService` configured with `FLAG_REQUEST_FILTER_KEY_EVENTS`. Listens in `onKeyEvent()`, stores event details, and returns `false`.
* **`LastEventStore.kt`**: Lightweight persistence backed by `SharedPreferences` and observable `StateFlow`. Updates asynchronously without blocking main thread.
* **`AccessibilityUtils.kt`**: System helper checking whether the service is enabled in Android `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES`.

---

## Folder Structure

```
BudsMapper/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── java/com/budsmapper/app/
│       │   ├── MainActivity.kt
│       │   ├── accessibility/
│       │   │   └── BudsAccessibilityService.kt
│       │   ├── data/
│       │   │   └── LastEventStore.kt
│       │   ├── ui/
│       │   │   ├── BudsMapperApp.kt
│       │   │   ├── screens/
│       │   │   │   └── HomeScreen.kt
│       │   │   └── theme/
│       │   │       ├── Color.kt
│       │   │       ├── Theme.kt
│       │   │       └── Type.kt
│       │   └── util/
│       │       └── AccessibilityUtils.kt
│       ├── res/
│       │   ├── values/
│       │   │   └── strings.xml
│       │   └── xml/
│       │       └── accessibility_service_config.xml
│       └── AndroidManifest.xml
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── .gitignore
└── README.md
```

---

## AccessibilityService Configuration

* **Manifest Declaration**: `android.permission.BIND_ACCESSIBILITY_SERVICE`
* **Intent Filter**: `android.accessibilityservice.AccessibilityService`
* **Configuration XML (`accessibility_service_config.xml`)**:
  * `canRequestFilterKeyEvents="true"`
  * `accessibilityFlags="flagRequestFilterKeyEvents"`
  * `canRetrieveWindowContent="false"` (No window/content inspection)
  * No gesture dispatching, screenshot collection, or intrusive capabilities requested.

---

## Build Instructions

To build the debug APK using the Gradle wrapper:

### Windows:
```cmd
gradlew.bat assembleDebug
```

### macOS / Linux:
```bash
./gradlew assembleDebug
```

### Output APK:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## ADB Installation & Physical Device Testing Instructions

1. Connect the Samsung Galaxy A35 via USB with USB Debugging enabled:
   ```cmd
   adb devices
   ```
2. Install the APK:
   ```cmd
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
3. Launch **Buds Mapper** on the phone.
4. Tap **Enable Service in Settings** and toggle **Buds Mapper Diagnostic Service** to ON.
5. Connect **Samsung Galaxy Buds Core**.
6. Open YouTube, Spotify, or any media app and start playback.
7. Perform earbud gestures (single tap, double tap, triple tap).
8. Return to Buds Mapper:
   * If `MEDIA_PLAY`, `MEDIA_PAUSE`, `MEDIA_NEXT`, or `MEDIA_PREVIOUS` appear under **Earbud Command Monitor** and **Last Received Event**, the `AccessibilityService` is capable of intercepting Galaxy Buds Core AVRCP commands!
   * If no events appear after verified earbud taps, Android is routing AVRCP commands directly through `AudioService`/`MediaSessionManager`. This provides the empirical proof required to pivot to a `MediaSession` / `MediaButtonReceiver` architecture in Phase 2.
