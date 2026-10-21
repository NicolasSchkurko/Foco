# Walkthrough — monitor-input-method-not-foreground

## Mechanism

The accessibility service classifies every `TYPE_WINDOW_STATE_CHANGED` through
`ForegroundAppState`, which holds three sets: ignored packages, input methods, and the current
tracked package. The IME set is built once, lazily, at service start
(`UsageMonitorAccessibilityService.kt:45-55`). A keyboard installed or enabled afterwards misses
the set, so its window event is classified as a normal app: `current` becomes the IME package, the
limit read finds no row for it, and the real foreground app's block request fails
`foreground.accepts`. The fix re-resolves the IME set on the unknown-package path (and can drop a
package that stopped being an IME), so classification always consults the current input-method
list instead of the one from service start.

## Physical traceability

- `app/src/main/java/com/foco/app/UsageMonitorAccessibilityService.kt:45-55,54` — the frozen set
  and its injection into the state object.
- `app/src/main/java/com/foco/app/domain/ForegroundAppState.kt:40,46` — the classification that
  consults the set.
- `app/src/test/java/com/foco/app/domain/ForegroundAppStateTest.kt` — JVM proof of the
  late-IME case.

## Limits and failure modes

- Refresh happens on the unknown-package path: the first event of a brand-new package costs one
  `PackageManager` lookup; subsequent events hit the cached classification.
- The state object stays pure: the service passes the resolved set in, so no Android type enters
  `domain/` and the JVM suite keeps covering the rule.
- If the platform reports an IME as enabled-but-never-used, its events still never replace the
  foreground — the conservative side: typed-in apps keep their limit.

## Touched APIs

`PackageManager` input-method query through the existing service context; the accessibility event
stream it already consumes. No new permission, storage or network.

## Implicit decisions

- Refresh-on-unknown instead of a per-event full query: cheapest path that fixes the defect, one
  line of code at the classification seam.
- A package that stops being an IME is dropped on refresh: the set reflects the current system
  state both ways, so the "keyboard" exemption cannot become a permanent hole.
