# 🎙️ Vocalize — Advanced Voice Memo App

<div align="center">

![Android](https://img.shields.io/badge/Platform-Android%208.0%2B-green?logo=android)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=android)
![Min SDK](https://img.shields.io/badge/Min%20SDK-API%2026-brightgreen)
![Target SDK](https://img.shields.io/badge/Target%20SDK-API%2034-blue)
![Architecture](https://img.shields.io/badge/Architecture-MVVM%20%2B%20Clean-orange)
![License](https://img.shields.io/badge/License-MIT-lightgrey)
![Build](https://github.com/neet-ctrl/Vocalize/actions/workflows/build-debug-apk.yml/badge.svg)

**Vocalize** is a production-ready, feature-complete Android voice memo application built entirely with Kotlin and Jetpack Compose. It delivers offline voice-to-text transcription, smart reminders, playlist and category management, full app backup export/import, and an advanced home screen widget — all in a beautiful Material 3 dark/light UI.

[Features](#-features) • [Screenshots](#-architecture) • [Architecture](#-architecture) • [Build](#-building-the-apk) • [CI/CD](#-cicd--github-actions) • [Contributing](#-contributing)

</div>

---

## ✨ Features

### 🎤 Recording & Storage
| Feature | Description |
|---|---|
| Voice Recording | Tap-to-record with real-time animated waveform |
| Auto-naming | Files named `yyyyMMdd_HHmmss_uniqueID.m4a` |
| Internal storage | All audio saved privately in app's internal folder — no external permissions |
| Room database | Full metadata (title, duration, date, note, transcription, category, reminder) |
| Import audio | Pick `.m4a` / `.mp3` from device and copy to app storage |

### ▶️ Playback & Management
| Feature | Description |
|---|---|
| Playback controls | Play, pause, seek, speed selector (0.5× – 2×) |
| Background playback | Foreground service with lock screen & notification controls |
| Playback resume | Remembers last position if you pause and close the app |
| Memo list | Scrollable list with lazy-loaded pagination |
| Recents tab | Last 10 memos for instant access |
| Card actions | ▶ Play, ⏰ Reminder, ⋮ menu (edit, delete, add to playlist, pin) |
| Swipe to delete | Swipe left on any memo card to dismiss |
| Share memo | Export as `.m4a` to any app |

### ⏰ Reminders & Notifications
| Feature | Description |
|---|---|
| One-time reminder | Pick date & time using the system date/time picker |
| Repeat reminders | Daily, Weekly, or Custom days (e.g. Mon/Wed/Fri) |
| Notification actions | Play (opens app & plays), Snooze (configurable), Dismiss |
| Full-screen intent | Android 10+ shows large player card when phone unlocks |
| Reminder tone control | Select a custom tone folder, preview sounds, and test reminder playback |
| Daily digest | Configurable morning notification listing today's reminders |
| Boot reschedule | All reminders restored after device reboot |

### 📂 Organisation
| Feature | Description |
|---|---|
| Categories | Work, Personal, Ideas, Shopping, Health — with custom colour & icon |
| Playlists | Create playlists, reorder memos, play entire playlist sequentially |
| Cross-fade | Smooth audio transition between playlist items |
| Pin to top | Star important memos — they appear in a pinned section |
| Batch operations | Multi-select → delete, categorise, or add to playlist in one go |

### 🔍 Search & Filters
| Feature | Description |
|---|---|
| Full-text search | Search by title or text note |
| Voice input for search | Mic button uses on-device speech recogniser |
| Filters | By category, date range, has reminder, playlist |
| Calendar view | Month grid with dots on reminder days; tap to see that day's memos |

### 🤖 Offline Voice-to-Text (Vosk)
| Feature | Description |
|---|---|
| Fully offline | No API key, no network needed for transcription |
| Vosk model | ~40 MB English model downloaded once, stored in internal storage |
| Auto-transcribe | Runs in background after every new recording |
| Multi-language | Download additional Vosk language models in Settings |

### ☁️ Backup & Restore
| Feature | Description |
|---|---|
| Export backup | Create a full `.voc` backup package with audio, memos, categories, tags, playlists, and reminders |
| Import backup | Restore app data from a `.voc` backup file, including audio and reminders |
| Local control | Choose the folder or backup file directly from the device |
| Manual backup | Export from Settings using a file picker |
| Restore | Import the selected `.voc` file and restore app state |

### 🎨 UI & Animations
| Feature | Description |
|---|---|
| Material 3 | Full dark/light theme support, follows system or manual override |
| Accent colour picker | 5 preset accent colours, applied app-wide |
| FAB pulse | Slow pulsing scale animation on the record button when idle |
| Waveform animation | Real-time bar chart driven by `MediaRecorder.getMaxAmplitude()` |
| Card slide-in | Staggered enter animation for memo cards |
| Shared element transition | Memo card expands into detail screen |
| Micro-interactions | Play button rotation, seekbar spring effect, FAB morphs into stop |
| Lottie | Splash screen, recording pulse, success checkmark animations |
| Swipe to delete | `SwipeToDismiss` with dismiss animation |

### 📱 Home Screen Widget
| Feature | Description |
|---|---|
| Advanced widget | Shows last 10 audio memos in a scrollable list |
| Quick record | Tap the mic button to open the recorder without opening the app |
| Quick play | Tap any memo row in the widget to play instantly |
| Widget actions | Sort, tag, and categorise directly from the widget |

---

## 🏗️ Architecture

Vocalize follows **MVVM + Clean Architecture** with strict separation of concerns:

```
app/
└── src/main/java/com/vocalize/app/
    ├── data/
    │   ├── local/
    │   │   ├── entity/         # Room entities (MemoEntity, CategoryEntity, PlaylistEntity, …)
    │   │   ├── dao/            # Room DAOs (MemoDao, CategoryDao, PlaylistDao)
    │   │   └── AppDatabase.kt  # Room database singleton
    │   └── repository/
    │       └── MemoRepository.kt
    ├── di/
    │   └── AppModule.kt        # Hilt dependency injection module
    ├── presentation/
    │   ├── home/               # HomeScreen + HomeViewModel
    │   ├── recorder/           # RecorderScreen + RecorderViewModel
    │   ├── detail/             # MemoDetailScreen + MemoDetailViewModel
    │   ├── calendar/           # CalendarScreen + CalendarViewModel
    │   ├── search/             # SearchScreen + SearchViewModel
    │   ├── settings/           # SettingsScreen + SettingsViewModel
    │   ├── playlist/           # PlaylistScreen + PlaylistViewModel
    │   ├── category/           # CategoryManageScreen + CategoryManageViewModel
    │   ├── components/         # MemoCard, PlaylistCard, WaveformView, BottomSheets
    │   ├── splash/             # SplashScreen
    │   ├── theme/              # VocalizeTheme, colour palette, typography
    │   └── NavGraph.kt         # Compose Navigation with animated transitions
    ├── service/
    │   ├── PlaybackService.kt  # Foreground media service
    │   └── VoskService.kt      # Background transcription service
    ├── util/
    │   ├── AudioRecorderManager.kt
    │   ├── AudioPlayerManager.kt
    │   ├── AudioFileManager.kt
    │   ├── BackupManager.kt
    │   ├── FileCompressor.kt
    │   ├── PermissionsHelper.kt
    │   ├── ReminderAlarmScheduler.kt
    │   ├── ReminderBroadcastReceiver.kt
    │   ├── BootReceiver.kt
    │   ├── NotificationHelper.kt
    │   ├── DailyDigestWorker.kt
    │   ├── WorkManagerInitializer.kt
    │   ├── VoskTranscriber.kt
    │   ├── Constants.kt
    │   └── Utils.kt
    ├── widget/
    │   └── VocalizeWidget.kt   # Home screen AppWidget
    ├── MainActivity.kt
    └── VocalizeApplication.kt
```

### Key Libraries
| Library | Purpose | Version |
|---|---|---|
| Jetpack Compose BOM | Declarative UI | 2024.06.00 |
| Room | Local database | 2.6.1 |
| Hilt | Dependency injection | 2.51.1 |
| WorkManager | Background tasks | 2.9.0 |
| Vosk Android | Offline ASR | 0.3.47 |
| Lottie Compose | Animations | 6.4.1 |
| DataStore | Preferences | 1.1.1 |
| Accompanist Permissions | Runtime permissions | 0.34.0 |
| Coil | Image loading | 2.6.0 |
| Coroutines | Async | 1.8.1 |

---

## 🔧 Building the APK

### Prerequisites
- **JDK 17** (Temurin / OpenJDK recommended)
- **Android Studio Hedgehog** (or newer) — for IDE builds
- **Android SDK** with API 34 and Build Tools 34

### Clone and build
```bash
git clone https://github.com/neet-ctrl/Vocalize.git
cd vocalize/vocalize-android

# Build debug APK
./gradlew assembleDebug

# Build release APK (unsigned)
./gradlew assembleRelease

# Install directly to a connected device
./gradlew installDebug
```

The debug APK is produced at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Signing a release build
1. Generate a keystore:
   ```bash
   keytool -genkey -v -keystore vocalize.jks \
     -alias vocalize -keyalg RSA -keysize 2048 -validity 10000
   ```
2. Build signed release:
   ```bash
   ./gradlew assembleRelease \
     -Pandroid.injected.signing.store.file=/path/to/vocalize.jks \
     -Pandroid.injected.signing.store.password=YOUR_STORE_PASS \
     -Pandroid.injected.signing.key.alias=vocalize \
     -Pandroid.injected.signing.key.password=YOUR_KEY_PASS
   ```

---

## 🚀 CI/CD — GitHub Actions

Vocalize ships with a production-grade GitHub Actions pipeline at `.github/workflows/build-debug-apk.yml`.

### Workflow triggers

| Trigger | Jobs |
|---|---|
| Push to `main` / `master` / `develop` | Debug + Release build |
| Pull request to `main` / `master` | Debug build + Lint check |
| Manual dispatch | Debug + optionally Release |

### Jobs

#### `build-debug`
- Sets up JDK 17 (Temurin) + Android SDK
- Caches Gradle files for fast subsequent builds
- Generates `gradle-wrapper.jar` if not in repo
- Runs `assembleDebug`
- Uploads APK as artifact (`vocalize-debug-<run>`) — retained 30 days

#### `build-release` (on `main`/`master` push or manual dispatch)
- Runs after `build-debug` succeeds
- Decodes and uses a signing keystore from GitHub Secrets (if configured)
- Falls back to unsigned APK if secrets not configured
- Uploads signed APK as artifact (`vocalize-release-<run>`) — retained 90 days
- Creates a GitHub Release automatically on tagged commits

#### `lint` (on Pull Requests)
- Runs `lintDebug` and uploads the HTML report as an artifact

### Setting up release signing secrets

Add the following secrets to your GitHub repository (`Settings → Secrets → Actions`):

| Secret | Description |
|---|---|
| `KEYSTORE_BASE64` | Base64-encoded `.jks` keystore: `base64 -i vocalize.jks` |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias |
| `KEY_PASSWORD` | Key password |

---

## 📋 Permissions

| Permission | Reason | API level |
|---|---|---|
| `RECORD_AUDIO` | Voice recording | All |
| `POST_NOTIFICATIONS` | Reminder & playback notifications | Android 13+ |
| `SCHEDULE_EXACT_ALARM` | Precise reminder timing | Android 12+ |
| `USE_EXACT_ALARM` | Supplementary exact alarm | Android 13+ |
| `USE_FULL_SCREEN_INTENT` | Full-screen reminder on lock screen | Android 10+ |
| `FOREGROUND_SERVICE` | Background recording / playback | All |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Media foreground service type | Android 10+ |
| `INTERNET` | Vosk model download + local backup export/import | All |
| `ACCESS_NETWORK_STATE` | Check connectivity before backup | All |
| `RECEIVE_BOOT_COMPLETED` | Reschedule reminders after reboot | All |
| `VIBRATE` | Notification vibration | All |
| `WAKE_LOCK` | Keep CPU on during recording | All |
| `READ_MEDIA_AUDIO` | Import audio from device | Android 13+ |
| `READ_EXTERNAL_STORAGE` (max API 32) | Import audio on older devices | ≤ Android 12 |

---

## 📁 Resource Files

```
app/src/main/res/
├── drawable/
│   ├── ic_mic_bg.xml          # Notification + widget mic icon
│   ├── ic_mic.xml             # Mic icon (vector)
│   ├── ic_play.xml            # Play icon
│   ├── ic_pause.xml           # Pause icon
│   ├── ic_stop.xml            # Stop icon
│   ├── ic_delete.xml          # Delete icon
│   ├── ic_alarm.xml           # Reminder icon
│   ├── ic_share.xml           # Share icon
│   ├── ic_search.xml          # Search icon
│   ├── ic_calendar.xml        # Calendar icon
│   ├── ic_playlist.xml        # Playlist icon
│   ├── ic_backup.xml          # Backup icon
│   ├── ic_settings.xml        # Settings icon
│   └── widget_bg.xml          # Widget background drawable
├── layout/
│   └── widget_vocalize.xml    # Widget RemoteViews layout
├── mipmap-*/                  # Launcher icons (mdpi → xxhdpi)
├── raw/                       # Lottie JSON, notification sound
├── values/
│   ├── strings.xml            # All app strings (130+ entries)
│   ├── colors.xml             # Brand colours
│   ├── themes.xml             # Light theme
│   └── dimens.xml             # Spacing, sizes
├── values-night/
│   └── themes.xml             # Dark theme
└── xml/
    ├── file_provider_paths.xml # FileProvider for audio sharing
    └── widget_info.xml         # AppWidget metadata
```

---

## 🔐 Privacy & Data

- **No data is collected.** All recordings stay on-device in the app's private internal storage.
- **Backup export/import** uses a local `.voc` file package with all memos, audio, categories, playlists, and reminders.
- **Vosk transcription** runs 100% on-device. No audio is ever sent to a server.

---

## 🧪 Compatibility

| Android version | API level | Status |
|---|---|---|
| Android 8.0 Oreo | API 26 | ✅ Minimum supported |
| Android 9.0 Pie | API 28 | ✅ Supported |
| Android 10 | API 29 | ✅ Full-screen intent supported |
| Android 11 | API 30 | ✅ Supported |
| Android 12 | API 31 | ✅ Exact alarm permission |
| Android 13 | API 33 | ✅ Notification & media permissions |
| Android 14 | API 34 | ✅ Target SDK |

---

## 🤝 Contributing

1. **Fork** the repository and create a feature branch:
   ```bash
   git checkout -b feature/my-feature
   ```
2. Make your changes, keeping to the existing MVVM + Clean Architecture pattern.
3. Ensure the debug build passes: `./gradlew assembleDebug`
4. Run lint: `./gradlew lintDebug`
5. Open a **Pull Request** — the CI pipeline will build and lint automatically.

### Code style
- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- All UI in Jetpack Compose — no XML layouts (except widget)
- ViewModels must not reference Android `Context` directly; use `ApplicationContext` via Hilt

---

## 📄 License

```
MIT License

Open source repository: https://github.com/neet-ctrl/Vocalize

Copyright (c) 2024 ✨ shakti kumar ✨

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

<div align="center">

Built with ❤️ by ✨ shakti kumar ✨ using Kotlin & Jetpack Compose

</div>
