# Walkthrough — layer-compliance (tech brief)

> For the human: this brief explains the mechanism of the change. Answer the questions in
> `## Cognitive sign-off` yourself; the agent never fills them.

## Mechanism

Two relocations, zero behavior change. **(1)** `availableDays` is pure date math (it builds
the detail screen's day window: today plus the previous six days); it moves from `data/` to
`domain/` — the layer `code-structure` reserves for pure behavior — together with its test;
the only call site (`AppDetailScreen`) changes one import. **(2)** An app's icon and label
come from Android's `PackageManager`; today the dashboard composables call it directly. The
reads move behind a small data adapter (`AppPackageInfo`, context-scoped like the other data
sources), which keeps the exact same behavior: off-main-thread execution, `runCatching`
fallbacks (gray box for a missing icon, the package name for a missing label). The composables
keep their `produceState` + IO shape and remain the only place where the Compose conversion
(`asImageBitmap`) happens. Finally the structure test gains two tripwires so the boundaries
cannot silently rot again.

## Physical traceability

- `app/src/main/java/com/foco/app/domain/AvailableDays.kt` — pure function (moved).
- `app/src/main/java/com/foco/app/data/AppPackageInfo.kt` — the PackageManager adapter (new).
- `app/src/main/java/com/foco/app/ui/dashboard/AppIcon.kt` · `AppLabel.kt` — same composables, now calling the adapter.
- `app/src/test/java/com/foco/app/domain/DaySelectionTest.kt` — the behavior regression (moved).
- `app/src/test/java/com/foco/app/structure/LayerRulesTest.kt` — the tripwires.
- No state, no persistence, no network: nothing new to store or clear.

## Limits and failure modes

- Behavior must be identical: same threading (IO), same fallbacks, same rendering. The suite (JVM + instrumented) is the proof; any red is a bug of this change.
- The `ui/` tripwire is a **heuristic** (marker scan: `packageManager.`, `getSystemService(`, `queryEvents(`, `unsafeCheckOpNoThrow(`). A future raw read written differently could slip past the scan — the semantic pass (`/spec-verify`) remains the net, and the marker list is easy to extend.
- The `data/` purity check is mechanical: a file under `data/` without `android`/`androidx` imports fails. A legitimate platform-free data file belongs in `domain/` per the contract — that is the point.
- An app uninstalled between load and render: same fallbacks as today (inside the adapter).

## Touched APIs

- `PackageManager.getApplicationIcon` / `getApplicationLabel` / `getApplicationInfo` (android.content.pm) — same calls as today, relocated behind the adapter.
- Coroutines `withContext(Dispatchers.IO)` — unchanged; the adapter is called from the same IO context.
- `produceState` (Compose) — unchanged in the composables; `asImageBitmap` conversion stays in `ui/`.

## Implicit decisions

- **One change for both moves** (same triage origin, same contract); the ADR records both decisions.
- **Adapter shape**: context-scoped class with methods (matches `UsageRepository`); it returns platform types (`Bitmap?`, `String`) and the Compose conversion stays in `ui/`.
- **Test moves with its package** (structure convention: test package mirrors the source package).
- **Tripwire marker list** deliberately small and documented.

## Cognitive sign-off

Answer in your own words (the agent must not fill these):

1. Where do pure helpers live, where did `availableDays` live before — and why did it move? — TODO(human)
2. If someone calls `packageManager.getApplicationIcon(...)` straight from a composable again, what catches it — and what is that net's limit? — TODO(human)
3. To add a new platform read used by the dashboard, which files/functions do you touch? — TODO(human)

When satisfied, the sign-off is recorded in `/spec-verify` (`understood_by` + `understood_at` in
`.openspec.yaml`).
