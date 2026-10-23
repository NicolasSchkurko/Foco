# Foco

An Android app (personal project) to:

1. **Track** how much and *when* you use each app (Digital Wellbeing style, but with your own rules).
2. Set **per-app daily limits** and optional popup alerts.
3. Add **optional friction at app open** — wait, breathe, solve a simple math problem — built as independent, composable modules.

Stack: **Kotlin + Jetpack Compose**. Everything local — no accounts, no servers.

---

## Current status

- ✅ **Day tracker**: total usage, hourly activity, app ranking and the "Usage Access" onboarding.
- ✅ **History**: Room-backed sessions, week view, app detail and the consolidation markers.
- ⏭️ **App limits, optional alerts and modular friction** (Phases 3–4): see **[PLAN.md](docs/PLAN.md)**.

## Requirements

- [Android Studio](https://developer.android.com/studio) (latest version — includes the JDK and the SDK).
- An Android phone running Android 10 (API 29) or higher, with **USB debugging** enabled.
  - The emulator works for viewing the UI, but **not** for real usage data.

## How to open and run it

1. Android Studio → **Open** → select this folder (`Foco`).
2. Wait for the first Gradle *sync* (it downloads Gradle 9.6 and the dependencies; the first time takes a while).
3. Connect the phone via USB and accept "Allow USB debugging" when it appears.
4. Press **Run ▶**.

> The project already uses the same stack that ships with Android Studio 2026.2: AGP 9.4, Kotlin 2.4 (built-in), Gradle 9.6 and Compose BOM 2026.06. You should not see "Upgrade" requests.

## First use

1. When it opens, Foco asks for the **Usage Access** permission (it is a special permission: Android does not allow requesting it through a dialog, only enabling it by hand).
2. Tap **"Open Settings"**, find **Foco** in the list and enable the permission.
3. Go back to the app: the dashboard updates by itself.

If your manufacturer is slow to show the app in that list, leave Settings and come back in.

## Project structure

```
Foco/
├── app/
│   ├── build.gradle.kts              # module: SDK, dependencies
│   └── src/
│       ├── main/                     # app source (java, res, manifest)
│       ├── test/                     # JVM unit tests
│       └── androidTest/              # instrumented tests
│           └── schemas/              # Room migration baselines
├── docs/                             # documentation, plan, mechanisms
│   └── PLAN.md                       # phased implementation plan
├── openspec/                         # living specs, changes and archive
├── gradle/libs.versions.toml         # version catalog
└── gradlew / gradlew.bat             # Gradle wrapper
```

## Useful commands (with the wrapper)

From the project root:

```powershell
.\gradlew.bat assembleDebug     # build -> app/build/outputs/apk/debug/app-debug.apk
.\gradlew.bat installDebug      # build and install on the connected phone
```

`adb` ships with the SDK (`%LOCALAPPDATA%\Android\Sdk\platform-tools`), or install *platform-tools* from the SDK Manager.

CI (`.github/workflows/ci.yml`) runs `./gradlew test` on Linux for every push to `main` and every pull request — that is where the Windows-excluded DataStore tests execute.

## How the tracker works (technical summary)

- `UsageStatsManager.queryEvents()` returns system events (`ACTIVITY_RESUMED` / `ACTIVITY_PAUSED`).
- `UsageRepository` turns them into **sessions** (app, start, end), drops apps without a launcher icon, the home screen and Foco itself, and builds two views: ranking by app and distribution by hour.
- The `PACKAGE_USAGE_STATS` permission is declared in the manifest and checked as an **AppOp** (`UsageAccess.isGranted`).
- Today is read live from the platform; finished days are consolidated into the app's Room history (`session_record`, `daily_aggregate`, `consolidated_day`) so they survive the platform's retention window.

## Common issues

| Symptom | Cause / fix |
|---|---|
| "No activity recorded" | The usage permission is not enabled, or it is a freshly reset phone (the system takes a while to accumulate events). |
| Gradle sync fails over the network | Retry: Tools → Sync Project. If it persists, check proxy/firewall. |
| `SDK location not found` | Open the project with Android Studio (it generates `local.properties`) or set `ANDROID_HOME`. |
| It builds but does not install | USB debugging accepted? Conflict with an already installed version? `.\gradlew.bat installDebug -r`. |
