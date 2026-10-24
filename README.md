# Foco

An Android app (personal project) to:

1. **Track** how much and *when* you use each app (Digital Wellbeing style, but with your own rules).
2. Set **per-app daily limits**, time windows, and optional alerts.
3. Add **optional friction at app open** — wait, breathe, solve a simple math problem — built as independent, composable modules.

Stack: **Kotlin + Jetpack Compose (Material 3)**. Everything local — no accounts, no servers.

---

## Current status

- ✅ **Day tracker**: total usage, hourly activity, app ranking and the "Usage Access" onboarding.
- ✅ **History & Navigation**: Room-backed sessions, week view, app detail, cyclic position ring navigation and smooth transitions.
- ✅ **App limits & Alerts**: per-app daily limits, time ranges, and alert notifications.
- ⏭️ **Modular friction at app open** (Phase 4): see **[PLAN.md](docs/PLAN.md)**.

## Requirements

- [Android Studio](https://developer.android.com/studio) (latest version — includes the JDK and the SDK).
- An Android phone running Android 10 (API 29) or higher, with **USB debugging** enabled.
  - The emulator works for viewing the UI, but **not** for real usage data.

## How to open and run it

1. Android Studio → **Open** → select this folder (`Foco`).
2. Wait for the first Gradle *sync* (it downloads Gradle and dependencies; the first time takes a while).
3. Connect the phone via USB and accept "Allow USB debugging" when it appears.
4. Press **Run ▶**.

> The project uses AGP 9.4, Kotlin 2.4, Gradle 9.6, and Compose BOM 2026.06.

## First use

1. When it opens, Foco asks for the **Usage Access** permission (Android requires enabling this manually in Settings).
2. Tap **"Open Settings"**, find **Foco** in the list and enable the permission.
3. Return to the app: the dashboard updates automatically.

## Project structure

```
Foco/
├── app/
│   ├── build.gradle.kts              # module: SDK, dependencies
│   └── src/
│       ├── main/                     # app source (java, res, manifest)
│       ├── test/                     # JVM unit tests
│       └── androidTest/              # instrumented tests
├── docs/                             # documentation and implementation plan
│   └── PLAN.md                       # phased implementation plan
├── openspec/                         # living specifications, changes and archive
├── gradle/libs.versions.toml         # version catalog
└── gradlew / gradlew.bat             # Gradle wrapper
```

## Useful commands (with the wrapper)

From the project root:

```powershell
.\gradlew.bat assembleDebug     # build -> app/build/outputs/apk/debug/app-debug.apk
.\gradlew.bat installDebug      # build and install on the connected phone
.\gradlew.bat test              # run all JVM unit tests
```

`adb` ships with the SDK (`%LOCALAPPDATA%\Android\Sdk\platform-tools`), or install *platform-tools* from the SDK Manager.

CI (`.github/workflows/ci.yml`) runs `./gradlew test` on Linux for every push to `main` and every pull request.

## How the tracker & UI work (technical summary)

- `UsageStatsManager.queryEvents()` returns system events (`ACTIVITY_RESUMED` / `ACTIVITY_PAUSED`).
- `UsageRepository` turns them into **sessions** (app, start, end), filtering launcherless apps, home screen, and Foco itself.
- Today's data is read live; finished days are consolidated into Room (`session_record`, `daily_aggregate`, `consolidated_day`) so they survive system retention limits.
- **Architecture**: `DashboardViewModel` and `AppDetailViewModel` manage data fetching, auto-refreshes, and session summaries off the main thread. Screens (`DashboardScreen`, `AppDetailScreen`) are stateless Composables reusing `ScreenHeader`, `StepSelectorRow`, and `PositionRingContainer` for uniform headers, step navigation, and smooth horizontal slide animations.

## Common issues

| Symptom | Cause / fix |
|---|---|
| "No activity recorded" | The usage permission is not enabled, or it is a freshly reset phone (system takes time to aggregate events). |
| Gradle sync fails over network | Retry: Tools → Sync Project. Check proxy/firewall if issue persists. |
| `SDK location not found` | Open the project in Android Studio (generates `local.properties`) or set `ANDROID_HOME`. |
| Builds but does not install | Verify USB debugging is accepted. Clear existing version with `.\gradlew.bat installDebug -r`. |
