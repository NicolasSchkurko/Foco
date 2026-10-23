# PLAN — Foco (tracker + app limits + friction)

Phased implementation plan. Each phase ends with something usable and a concrete
"done" criterion. The goal is to learn Android by building, not the other way around.

## Core idea

Beating infinite scroll is not done with walls (people bypass them), but with
**friction**: when you enable it, every decision to open the app costs a few
seconds and requires intention. The tracker is the mirror; friction is the tool.
A full stop appears only when you turn on a daily limit.

## Design principles

1. **Friction by default; a wall only when you ask for it.** Waits, breathing
   pauses, math — change the decision, do not declare war on the user. A daily
   limit is the one explicit wall, and only while it is active.
2. **Everything local.** No accounts or servers: usage data stays on the phone.
3. **The platform rules.** Android has no public API to block part of another
   app. App-level limits, alerts and overlays need UsageStats plus
   accessibility/overlay. Build a modular launch signal and intervention engine,
   not one-off rules tied to one app's UI.
4. **Usable phases.** No three months without trying anything on the phone.

## Target architecture (where everything goes)

| Component | Responsibility | Phase |
|---|---|---|
| Compose UI (`ui/`) | Dashboard, limits, settings | 1+ |
| `UsageRepository` (`data/`) | Read UsageStatsManager → sessions | 1 ✅ |
| Room (`data/db/`) | Own history, budgets, rules | 2 |
| `UsageMonitorService` (FGS) | Track ongoing usage, trigger limits | 3 |
| `LimitEngine` | Per-app daily budgets + optional alerts | 3 |
| `AppLaunchSignal` | Detect the foreground app (UsageStats/accessibility) | 3 |
| `FrictionHost` | Overlay foundation; full block at an active limit | 3 |
| `InterventionEngine` + `InterventionRegistry` | Ordered wait/breathe/math modules at open | 3-4 |

---

## Phase 0 — Environment

- [x] Install Android Studio; open `Foco`; first build on the phone.
- [x] USB debugging on the phone; accept the prompt from the PC.
- [ ] (For Phase 3) Disable battery optimization for Foco.

**Done when:** the dashboard runs on your phone and you see your real usage.

## Phase 1 — Day tracker ✅ (scaffold included)

What's there: permission onboarding, day total, hourly activity (24 bars),
app ranking with icon, duration and time of the last use.

Key files:

- `MainActivity.kt` — decides onboarding ↔ dashboard and re-checks the permission on `ON_RESUME`.
- `UsageAccess.kt` — the `PACKAGE_USAGE_STATS` permission is checked as an AppOp.
- `data/UsageRepository.kt` — events → sessions → ranking + hourly buckets.
- `ui/DashboardScreen.kt` — the whole dashboard.

Natural extensions (whenever you feel like playing): date picker, comparison
with yesterday, CSV export, "today" widget.

**Done when:** ✅ already working.

## Phase 2 — History and persistence (Room)

Goal: stop depending on what the system retains (limited) and get a base for limits.

- [x] Entities: `SessionRecord` (pkg, start, end) and `DailyAggregate` (pkg, day, total).
- [x] `WorkManager`: nightly job that consolidates the day and cleans up old raw data.
- [x] **Week** screen: bars per day + totals per app.
- [x] Schema migration from day one (good habit).

New concepts: Room (DAO + Flow), WorkManager, migrations.

**Done when:** 7+ days of history visible, survives reboots.

## Phase 3 — App limits, alerts and the friction host

Goal: per-app daily limits, optional popup alerts, and the overlay foundation for
the full block. **This is where the foreground service appears.**

- [ ] Per-app daily limit (e.g. Instagram 30 min/day), configurable in Settings;
      an app with no limit is never blocked.
- [ ] `UsageMonitorService` (foreground, `specialUse` type — mandatory since Android 14):
      tick every 15-30 s with UsageStats + accessibility for real time.
      Lightweight alternative: `WorkManager` every 15 min (enough for limits, not for instantaneity).
- [ ] `LimitEngine`: when an app crosses its active daily limit, show a full-block
      screen; when the limit is disabled or absent, do not block.
- [ ] Alerts: optional popup at the configured threshold (notification or small
      overlay), only while alerts are enabled for that app; `POST_NOTIFICATIONS`
      on Android 13+.
- [ ] **FrictionHost** (`SYSTEM_ALERT_WINDOW` + `TYPE_APPLICATION_OVERLAY` or an
      accessibility overlay): the full-screen host for the limit block. The
      intervention modules arrive in Phase 4; no content-specific UI reading.
- [ ] UX reference patterns: **one sec**, **ScreenZen**, **Opal**.

**Done when:** if you reach an active Instagram limit, the phone really stops you;
if the limit is off, nothing happens; an alert appears only when you enabled it.

## Phase 4 — Modular friction at app open

Goal: optional friction **at app open**, built from independent modules, without
reading or hardcoding another app's UI.

- [ ] `Intervention` contract and `InterventionRegistry`: a module knows how to
      present itself and returns whether it was completed or abandoned.
- [ ] `InterventionPlan` per app: order and optional chain; enabled/disabled per
      app, with a global default.
- [ ] First modules: wait X seconds, breathe, simple math. They are independent:
      adding one is a class + registry entry + strings, not a core change.
- [ ] At app open, if friction is enabled for that app, run the plan in the
      `FrictionHost`; if friction is off, the app opens normally.
- [ ] No hidden friction: the settings screen names which modules are active and
      the user can turn each one off.
- [ ] Anti-impulse options: disabling the host or the limit may require a PIN;
      the behavior is recorded locally.
- [ ] Sideloaded (Android 13+): a manually installed app needs
      "Allow restricted settings" to enable accessibility.

**Done when:** wait, breathe and math work independently; enabling friction at
open runs the chosen plan; adding another module does not touch the engine.

## Phase 5 — Hardening (optional)

- [ ] **Device owner** (once, via adb: `adb shell dpm set-device-owner`):
      `DevicePolicyManager.setPackagesSuspended()` is an optional, stronger version
      of the full block; it has no heuristics. Careful: your app must be able to
      deactivate itself afterwards (`clearDeviceOwnerApp()`).
- [ ] Distribution: `adb install` or build a signed APK. Google's developer
      verification (in progress 2026-2027) **does not affect** adb installs or
      direct sideloading for now; to share with up to 20 devices there is a free
      "limited distribution account".

---

## Risks and things this plan does not promise

- **OEMs:** some manufacturers kill background services relentlessly; that is why
  Foco is excluded from battery optimization and the limits path does not depend on exact real time.
- **Permissions can disappear:** if accessibility/overlay access is revoked, the
  full block stops working; the plan must say so in Settings and keep the app
  usable as a tracker.
- **Friction fatigue:** if friction is always on it becomes noise. It is opt-in per
  app, and the settings say exactly which modules run.
- **Play Store:** accessibility + overlay together is a high-scrutiny area. This
  project is for personal use: it is installed over USB, it is not published.
- **Battery:** the cost is paid by the Phase 3 foreground service; it is measured and tuned there.

## Resources

- Free official course: *Android Basics with Compose* (developer.android.com).
- Key docs: `UsageStatsManager`, `AppOpsManager`, `AccessibilityService`,
  `TYPE_APPLICATION_OVERLAY`, `DevicePolicyManager.setPackagesSuspended`,
  foreground service types (`specialUse`).
- Apps to study their UX: **one sec**, **ScreenZen**, **Opal**.
