# Proposal — test-suite-speed

## Why

The only concrete symptom in the repo is that the tests are slow. The instrumented suite contains
fixed `Thread.sleep` waits (Settings, ExcludedApps, AppDetailNavigation, UnlockCount, DashboardRefresh)
and generous timeouts. The repo records no wall-clock baseline; the test-speed recon found the waits
and a 33–74 s spread on one class, but no duration in the gate evidence.

## What

Replace fixed UI waits with event-driven ones (`composeRule.waitUntil`, `UiDevice.wait`, idling) and
keep the same assertions and coverage. Add a structural JVM check that fails when a test under
`app/src/androidTest` uses a fixed `Thread.sleep` to wait for UI state.

Do not move tests between suites: the living `[@test]` contracts pin many to androidTest, and changing
the suite is a spec change, not this refactor.

## Expected outcome

The same test cases pass with less dead wait time and the rule stays enforced. No production code,
dependency or behavior changes.

## Alternatives considered

| Option | Why yes | Why not |
| --- | --- | --- |
| Event-driven waits + a structural guard (chosen) | Targets the measured cost; keeps the suite contracts | A few device shell waits need careful replacements |
| Move pure tests to JVM | Less device time | Changes living `[@test]` contracts; a separate spec decision |
| Shorten the fixed sleeps only | Tiny diff | Flaky; the next addition goes back to the old pattern |
| Add build/caching flags | Build-time win | `org.gradle.parallel` and configuration cache are already in place; the test waits remain |
