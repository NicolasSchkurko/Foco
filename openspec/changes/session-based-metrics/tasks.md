# Tasks

## 1. Test infrastructure (first tests in the repo)

- [ ] 1.1 Add JUnit to the version catalog and the app module's test configuration; verify `gradlew.bat test` completes successfully.
- [ ] 1.2 Declare `test_command: "gradlew.bat test"` in `openspec/config.yaml`; verify `node scripts/gates/gate-check.mjs --run-tests` records green evidence.

## 2. Data layer: sessions as the primitive

- [ ] 2.1 Model the session primitive and the raw event type (e.g. `AppUsageSession(packageName, start, end)`, `UsageEvent(packageName, timestamp, type)`) and implement the pure session fold; verify with `SessionBuilderTest` covering: direct app switch, open session closed at query time, same-app transitions, no activity.
- [ ] 2.2 Implement the pure filtering policy over allowed/excluded package sets; verify with `SessionFilterTest` covering: non-launchable package dropped, home/self dropped.
- [ ] 2.3 Rewire `UsageRepository.loadToday()` to return the session list (map Android events to the raw event type, apply the policy with the sets it already resolves); verify on device that the dashboard still lists the same apps.

## 3. UI layer: derived metrics

- [ ] 3.1 Implement the pure derivations in the UI layer (day total, hourly distribution with hour-boundary splitting, ranking sorted by duration descending); verify with `UsageMetricsTest` covering: total, hour-boundary split, ranking order.
- [ ] 3.2 Rewire `DashboardScreen` to derive its data from the sessions (same visuals; label resolution mirrors the existing icon resolution); verify on device that the dashboard shows the same information as before.

## 4. Integration

- [ ] 4.1 Run the full gate with green evidence (`node scripts/gates/gate-check.mjs --run-tests`) and the app build (`gradlew.bat assembleDebug`); verify both pass.
- [ ] 4.2 On commit, update `gitref` in `.openspec.yaml` to the branch tip; verify it matches `git rev-parse --short HEAD`.

## Workflow follow-up

- Review the apply report, merge, and archive with `gate-check --pre-archive session-based-metrics`.
- `kind: architecture` → consider `/diagram-sync` before archiving.
