# Workspace

## Overview

This workspace contains a native Android application called **Vocalize** — a production-ready, feature-complete advanced voice reminder app built with Kotlin and Jetpack Compose. It follows MVVM + Clean Architecture principles.

## Android App: Vocalize

**Location**: `vocalize-android/`
**Package**: `com.vocalize.app`
**Min SDK**: 26 (Android 8.0)
**Target SDK**: 34 (Android 14)
**README**: `vocalize-android/README.md`

### Tech Stack
- **UI**: Jetpack Compose (fully declarative, Material 3)
- **Architecture**: MVVM + Clean Architecture
- **DI**: Dagger Hilt
- **Database**: Room (SQLite)
- **Audio**: MediaRecorder (recording), MediaPlayer (playback)
- **Transcription**: Vosk (offline speech-to-text, ~40MB model, no API key)
- **Notifications**: WorkManager + AlarmManager (exact alarms)
- **Backup**: Google Drive AppDataFolder API (free, no backend cost)
- **Animations**: Lottie + Compose animations (shared transitions, spring)
- **Navigation**: Jetpack Navigation Compose
- **Widget**: AppWidgetProvider + RemoteViewsService (scrollable list)

### Screens (8 screens)
- **SplashScreen** — branded loading with animated pulse rings
- **HomeScreen** — tabs (Recents, All Memos, Playlists), animated FAB, batch select, pinned section
- **RecorderScreen** — real-time waveform, amplitude visualization, voice-to-text
- **MemoDetailScreen** — playback controls, seekbar, speed selector, reminder setup
- **CalendarScreen** — month calendar grid with memo/reminder dots per day
- **SearchScreen** — full-text search with category/reminder/date filters + voice input
- **SettingsScreen** — Google Drive backup, Vosk model, dark mode, accent colour, categories link
- **PlaylistScreen** — playlist playback with drag-to-reorder, mini player bar
- **CategoryManageScreen** — create, edit, delete categories with colour picker

### Key Source Files
- `app/src/main/java/com/vocalize/app/`
  - `presentation/` — all Compose screens and ViewModels
  - `data/local/` — Room entities, DAOs, AppDatabase (with static getDatabase factory)
  - `data/repository/` — MemoRepository
  - `util/` — AudioRecorderManager, AudioPlayerManager, VoskTranscriber, BackupManager,
    FileCompressor, PermissionsHelper, Utils, ReminderAlarmScheduler, NotificationHelper,
    DailyDigestWorker, WorkManagerInitializer, BootReceiver, Constants
  - `service/` — PlaybackService (foreground), VoskService (transcription)
  - `widget/` — VocalizeWidget (advanced scrollable list), WidgetListService (RemoteViewsService)
  - `di/` — Hilt AppModule

### Resources
- `res/values/strings.xml` — 130+ string entries
- `res/values/colors.xml` — brand colour palette
- `res/values/themes.xml` + `res/values-night/themes.xml` — light/dark themes
- `res/values/dimens.xml` — spacing, sizing constants
- `res/drawable/` — 15+ vector icons + adaptive launcher icons (ic_launcher_foreground/background)
- `res/mipmap-anydpi-v26/` — adaptive launcher icons for API 26+
- `res/layout/widget_vocalize.xml` — advanced widget layout (ListView + header + record FAB)
- `res/layout/widget_memo_item.xml` — widget list row layout
- `res/xml/widget_info.xml` — widget metadata (4×4 cells, resizable)
- `res/xml/file_provider_paths.xml` — FileProvider paths for sharing audio

### Build
To build the debug APK locally:
```bash
cd vocalize-android
./gradlew assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

### ProGuard
`app/proguard-rules.pro` contains rules for: Vosk, Google Drive, Room, Hilt, WorkManager,
Lottie, Coil, Gson, MediaSession, AppWidget, BroadcastReceivers, and Services.

### GitHub Actions CI/CD
`.github/workflows/build-debug-apk.yml` — three jobs:
1. **build-debug** — builds debug APK on every push; uploads artifact (30-day retention)
2. **build-release** — builds signed (or unsigned) release APK on main/master push; uploads artifact (90-day retention); creates GitHub Release on tagged commits
3. **lint** — runs lintDebug on pull requests; uploads lint HTML report

Release signing requires these repository secrets:
- `KEYSTORE_BASE64` — base64-encoded .jks keystore
- `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`

## Total Feature Count
- Core features: 38 (voice recording, playback, reminders, categories, playlists, search, calendar, import/export, backup)
- Advanced features: 22 (batch ops, pin to top, widget, cross-fade, lock screen player, daily digest, custom snooze patterns, etc.)
- Total: 60 unique features — all fully implemented, no placeholders

## Also in this workspace
- `artifacts/` — web artifact scaffolding (secondary, Android app is primary)
- `lib/` — shared TypeScript library (unused for Android)
