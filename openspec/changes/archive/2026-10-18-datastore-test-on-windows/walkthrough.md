# Walkthrough — DataStore's platform branch, and why the migration test skips on Windows

## What this change touches

One JVM unit test. No production code, no stored data, no dependency. What needs explaining is the mechanism behind the skip: it rests on how a platform constant behaves inside a unit test.

## Mechanism

1. `SettingsMigrationTest` runs on the JVM (it is a local unit test), not on Android — but it still links against the **Android** build of `androidx.datastore`, because that is what the app module resolves onto the unit-test classpath.
2. The test writes the same preferences file twice: first to seed the legacy store, then the migration clears the migrated keys out of that same file.
3. Every DataStore write first lands in a scratch file (`<name>.tmp`) and is then moved over the real file by `FileMoves_androidKt.atomicMoveTo`.
4. `atomicMoveTo` branches on `android.os.Build.VERSION.SDK_INT`:

   ```java
   SDK_INT >= 26 -> Api26Impl.move(...)   // java.nio Files.move(REPLACE_EXISTING, ATOMIC_MOVE)
   SDK_INT <  26 -> from.renameTo(to)     // java.io.File.renameTo
   ```

5. In an AGP unit test `android.jar` is a stub, so `Build.VERSION.SDK_INT` reads `0`: the `renameTo` branch is always the one taken.
6. `File.renameTo` on Windows is `MoveFile`, which refuses to replace an existing destination; `Files.move` with `REPLACE_EXISTING` does not. The skip exists because of that difference — not because of antivirus, file locks, or a DataStore defect.

## Physical traceability

| What | Where | When |
|---|---|---|
| The preferences files | A JUnit `TemporaryFolder` root under the system temp directory (`legacy.preferences_pb`, `foco_settings.preferences_pb`) | Created and deleted per test method |
| The scratch file | Same folder, `<name>.preferences_pb.tmp` | Written on each DataStore write, then moved over the target |
| The platform guard | `SettingsMigrationTest` | Evaluated before each test method; a guarded test is reported as skipped, never as passed |

Lifecycle: nothing survives a run. The temp folder belongs to one test method and the JUnit rule deletes it afterwards.

## Limits and failure modes

- On Windows the two guarded tests do not execute, so the migration does not run there. It is still exercised on Linux/CI and on a device or emulator, where `SDK_INT >= 26` and DataStore takes the `Files.move` path.
- The guard keys off the operating system, not off `Build.VERSION.SDK_INT`, because the value a unit test observes is a stub and cannot be trusted. If the environment ever reports a real level (for example after adopting Robolectric), the skip is no longer needed and should be removed — it is a workaround, not a rule.
- `aFreshInstallIsUntouched` deliberately keeps running on Windows: it writes once, so the destination does not exist yet and `renameTo` succeeds.

## Touched APIs

- `android.os.Build.VERSION.SDK_INT` — the platform level. Under a unit test it is a stubbed `0`, which is what selects the failing branch.
- `java.io.File.renameTo` — a filesystem rename; on Windows it will not replace an existing file.
- `java.nio.file.Files.move(..., REPLACE_EXISTING, ATOMIC_MOVE)` — the replace-capable move DataStore uses on API 26+.
- JUnit `Assume` — converts an unmet precondition into a skip, so the exclusion is reported instead of hidden.

## Implicit decisions

- The guard is keyed on the operating system rather than on `SDK_INT`, because the stubbed value cannot distinguish "old platform" from "unit test".
- The requirement is added to `code-structure` (where the suite's own guarantees already live) rather than to `app-settings` (where settings behavior lives): the subject is test portability, not the settings feature.

## Cognitive sign-off

Answered by the human on 2026-10-18 — answers transcribed verbatim (repo convention). The sign-off was recorded on 2026-10-19 (`understood_by`/`understood_at` in `.openspec.yaml`), when the human closed the change: the answers above were re-validated against the mechanism before recording.

1. The device and the unit test run the same code — why does one pass and the other fail?
   - "Ok, entiendo. como nuestro test cae en el fallback de sdk = 0 se busca hacer un file rename
     al archivo x.preferences_pv.temp una vez editado para logar completar el test. como windows
     no puede renombrar un archivo temp por sobre el real rompe y da eror" — the human, 2026-10-18
   - Noted: this covers the failing side (the test's `SDK_INT = 0` fallback, and Windows refusing
     to move a temp file over the existing one). The passing side is its mirror — on a device
     `SDK_INT = 36` takes `Files.move(REPLACE_EXISTING)`, which does replace. Question 2 covers it.
2. What does `Build.VERSION.SDK_INT` report inside a unit test, and why does that decide which branch runs?
   - "ok reporta 0 por el stub del android jar y como se cumple la condicion de ser menor a 26 cae en file rename to" — the human, 2026-10-18
3. What exactly is given up by skipping on Windows, and where is that gap covered instead?
   - "se pierden los tests copiesTheSettingsClearsTheLegacyAndLeavesTheExclusionsAlone y runningItAgainIsANoOp. se cubre en linux o los CI y en el emulador" — the human, 2026-10-18
   - Noted: the emulator/device side is the manual upgrade pass (there is no instrumented test
     for the migration), so on Windows the migration is unverified locally — and it shows as
     *skipped*, not green.
