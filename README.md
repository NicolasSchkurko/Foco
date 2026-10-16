# Foco

An Android app (personal project) to:

1. **Track** how much and *when* you use each app (Digital Wellbeing style, but with your own rules).
2. Add **limits and friction** (reminders, forced waits) to reduce automatic usage.
3. **Block Reels / Shorts** (infinite-scroll content) inside Instagram, TikTok and YouTube.

Stack: **Kotlin + Jetpack Compose**. Everything local — no accounts, no servers.

---

> **🧪 Note:** this repository is a **development experiment** for the **Spec Growth** method — a spec-anchored workflow where specs are the living contract (`openspec/specs/`), in-flight changes live in `openspec/changes/`, the rationale is recorded in `DECISIONS.md`, status is generated into `BOARD.md`, and verification runs in `scripts/gates/` (pre-commit and CI). The app is the method's test case.

---

## Current status

- ✅ **Phase 1 (this scaffold)**: day tracker — total usage, hourly activity and app ranking, with onboarding for the "Usage Access" permission.
- ⏭️ Phases 2 to 5 (limits, friction, Reels blocking): see **[PLAN.md](PLAN.md)**.

## Requirements

- [Android Studio](https://developer.android.com/studio) (latest version — includes the JDK and the SDK).
- An Android phone running Android 10 (API 29) or higher, with **USB debugging** enabled.
  - The emulator works for viewing the UI, but **not** for real usage data.

## How to open and run it

1. Android Studio → **Open** → select this folder (`Foco`).
2. Wait for the first Gradle *sync* (it downloads Gradle 9.6 and the dependencies; the first time takes a while).
3. Connect the phone via USB and accept "Allow USB debugging" when it appears.
4. Press **Run ▶**.

> The project already uses the same stack that ships with Android Studio 2026.2: AGP 9.4, Kotlin 2.4 (built-in), Gradle 9.6 and Compose BOM 2026.09. You should not see "Upgrade" requests.

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
│   └── src/main/
│       ├── AndroidManifest.xml       # special permission + queries + activity
│       ├── res/                      # icon, base theme, strings
│       └── java/com/foco/app/
│           ├── MainActivity.kt       # entry point: onboarding ↔ dashboard
│           ├── UsageAccess.kt        # "Usage Access" permission check
│           ├── data/
│           │   ├── UsageModels.kt    # models (day, app, hour)
│           │   └── UsageRepository.kt# queries UsageStatsManager, builds sessions
│           └── ui/
│               ├── OnboardingScreen.kt
│               ├── DashboardScreen.kt# hourly chart + app ranking
│               ├── Formatting.kt     # duration "3 h 24 min", time "14:32"
│               └── theme/Theme.kt    # Material 3 (+ dynamic color)
├── gradle/libs.versions.toml         # version catalog
├── PLAN.md                           # phased implementation plan
└── gradlew / gradlew.bat             # Gradle wrapper
```

## Useful commands (with the wrapper)

From the project root:

```powershell
.\gradlew.bat assembleDebug     # build -> app/build/outputs/apk/debug/app-debug.apk
.\gradlew.bat installDebug      # build and install on the connected phone
```

`adb` ships with the SDK (`%LOCALAPPDATA%\Android\Sdk\platform-tools`), or install *platform-tools* from the SDK Manager.

## How the tracker works (technical summary)

- `UsageStatsManager.queryEvents()` returns system events (`ACTIVITY_RESUMED` / `ACTIVITY_PAUSED`).
- `UsageRepository` turns them into **sessions** (app, start, end), drops apps without a launcher icon, the home screen and Foco itself, and builds two views: ranking by app and distribution by hour.
- The `PACKAGE_USAGE_STATS` permission is declared in the manifest and checked as an **AppOp** (`UsageAccess.isGranted`).
- Data is queried on the fly (the system already stores it); own persistence arrives in Phase 2 with Room.

## Common issues

| Symptom | Cause / fix |
|---|---|
| "No activity recorded" | The usage permission is not enabled, or it is a freshly reset phone (the system takes a while to accumulate events). |
| Gradle sync fails over the network | Retry: Tools → Sync Project. If it persists, check proxy/firewall. |
| `SDK location not found` | Open the project with Android Studio (it generates `local.properties`) or set `ANDROID_HOME`. |
| It builds but does not install | USB debugging accepted? Conflict with an already installed version? `.\gradlew.bat installDebug -r`. |
