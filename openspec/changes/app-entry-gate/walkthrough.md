# Walkthrough — app-entry-gate (tech brief)

> For the human: this brief explains the mechanism of the change. Answer the questions in
> `## Cognitive sign-off` yourself; the agent never fills them.

## Mechanism

The app opens on one of two screens depending on the special "usage access" permission — an
**AppOp**, not a runtime permission: the user flips it in system Settings and the OS persists it
per package. `App()` reads `UsageAccess.isGranted(context)` when it first composes and re-reads it
on every `ON_RESUME` (i.e., when returning from Settings), then shows the dashboard or the
onboarding. This change adds a **seam**: `App(isUsageAccessGranted = { ... })`, whose production
default is the real `UsageAccess::isGranted`. Two test layers (human decision — "both"):
(1) `AppEntrySeamTest` injects a fake predicate and moves the activity CREATED→RESUMED to prove
the re-evaluation logic deterministically; (2) `AppEntryGateTest` mutates the REAL AppOp from
instrumentation — `UiAutomation.executeShellCommand("appops set com.foco.app
android:get_usage_stats allow|ignore")`, the same switch Settings flips — launching `MainActivity`
manually (empty compose rule + `ActivityScenario`) so the swap happens BEFORE the cold start, and
restoring `allow` on cleanup.

## Physical traceability

- `app/src/main/java/com/foco/app/ui/App.kt` — the gate and the seam parameter.
- `app/src/androidTest/java/com/foco/app/ui/AppEntryGateTest.kt` — E2E; touches the DEVICE's AppOps
  state for `com.foco.app` via shell; cleanup restores it.
- `app/src/androidTest/java/com/foco/app/ui/AppEntrySeamTest.kt` — deterministic; no device state.
- The permission itself lives in the OS (per package); **uninstalling wipes it** — reinstalling
  starts un-granted, which once masqueraded as a bug ("I granted it yesterday").

## Limits and failure modes

- The E2E layer requires the API ≤ 36 emulator (Espresso breaks on API 37) and generates usage by
  opening Settings; waits are generous to absorb load.
- If a test crashes before cleanup, the emulator can be left with the permission revoked (onboarding
  at the next app open) — the exact misreading that motivated this change. Cleanup plus the manual
  checklist cover it.
- The seam proves the composition logic but NOT the real AppOp read; the E2E covers that half.
  Each layer alone would leave a blind spot — hence both.
- The permission check is read-only (`unsafeCheckOpNoThrow`); the app never writes permission state.

## Touched APIs

- `AppOpsManager.OPSTR_GET_USAGE_STATS` + `unsafeCheckOpNoThrow` (platform; read-only).
- `UiAutomation.executeShellCommand` (instrumentation → shell; tests only).
- `ActivityScenario`, `createEmptyComposeRule`, `LocalContext`, `Lifecycle`/`DisposableEffect`.

## Implicit decisions

- **Seam + E2E, not either alone** (human decision): deterministic logic coverage plus real
  integration proof.
- **No intended behavior change**: the seam's default preserves production behavior; if a test goes
  red during the apply, the cause is fixed inside this change (the regression test already exists).
- **Cleanup discipline**: the tests restore `allow`; nothing should leave the emulator lying to the
  next human tester.

## Cognitive sign-off

Answer in your own words (the agent must not fill these):

1. Why do the tests need BOTH a fake predicate and real `appops` shell commands — what does each
   layer prove that the other cannot?
   > porque los reales son fragiles y los fakes no sabemos si el permiso real se lee correctamente.
   >
   > — the human, 2026-10-16
2. Why can reinstalling the app suddenly show the onboarding even though "you granted the
   permission yesterday"?
   > porque se reinicia el permiso
   >
   > — the human, 2026-10-16
3. What would you look at first if the emulator shows the onboarding right after the tests ran?
   > el estado real del permiso en el emulador y si vemos que esta allow e igual salio onboarding
   > revisamos el codigo
   >
   > — the human, 2026-10-16

When satisfied, the sign-off is recorded in `/spec-verify` (`understood_by` + `understood_at` in
`.openspec.yaml`).
