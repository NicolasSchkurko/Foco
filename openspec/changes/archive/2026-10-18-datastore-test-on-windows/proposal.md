# Proposal — Green JVM suite on Windows: declare the DataStore platform exclusion

## Why

`gradlew.bat test` is red on Windows, and it is red for a platform reason that has nothing to do with the app code.

1. `SettingsMigrationTest` writes the same file twice: it seeds the legacy store, then the migration clears the migrated keys from it.
2. On that second write, the **Android** artifact of `androidx.datastore` calls `FileMoves_androidKt.atomicMoveTo`, which branches on the SDK level:

   ```java
   Build.VERSION.SDK_INT >= 26 -> Api26Impl.move(...)   // java.nio Files.move — replaces the target
   Build.VERSION.SDK_INT <  26 -> from.renameTo(to)     // Windows: cannot replace an existing file
   ```

3. In an AGP unit test `android.jar` is a mock, so `Build.VERSION.SDK_INT` is `0`: the `renameTo` branch is always taken.
4. `File.renameTo` on Windows uses `MoveFile`, which does **not** overwrite an existing destination (POSIX `rename(2)` does). So the second write always throws `IOException: Unable to rename ...`.

Measured on this machine (2026-10-18) with a standalone probe: `File.renameTo` over an existing target → `false`; `Files.move(REPLACE_EXISTING + ATOMIC_MOVE)` → OK. Reproduced identically against datastore `1.1.7` and `1.2.1`, so this is **not** a version regression. It is the reported upstream case (robolectric issue #7919, `"Unable to rename"` with a `C:\Users\...` path).

The app is unaffected: on a device or emulator `SDK_INT >= 26`, so DataStore takes the `Files.move` path — verified by installing and running Foco on the API 36 AVD. The cost of leaving it red is real: `gate-check --run-tests` reports `GATE RED`, and `structure-housekeeping`'s `verified` has no local green backing.

## What Changes

- `SettingsMigrationTest` declares the Windows exclusion explicitly (JUnit `Assume`), naming the DataStore `renameTo` branch as the reason. On Windows the two affected tests are skipped with a visible reason; on every other platform they run unchanged.
- `code-structure` gains one requirement: a JVM test SHALL NOT fail because of a platform-dependent framework path; a platform exclusion SHALL be declared and named, never silent.
- Nothing else changes: no production code, no dependency, no stored data.

## Alternatives Considered

| Option | Why yes | Why not / status |
|---|---|---|
| Robolectric (`@RunWith` + `@Config(sdk = [36])`) | The test really executes on Windows with `SDK_INT = 36`, so the migration keeps full coverage there | Adds a test dependency (plus its `android-all` jars) and slows the suite — heavier than the problem it solves — **not chosen** |
| Run the JVM suite on Linux/WSL/CI only | Zero repository changes | Leaves the default developer loop red on Windows, which is where this project is developed — **not chosen** |
| **Declare the Windows exclusion in the test** — chosen (human, 2026-10-18) | Three lines, no dependency, deterministic on every platform, and the exclusion is named rather than silent | On Windows the migration is verified only for the paths that can run there; the skip makes the gap visible instead of hiding it |

## Impact

- `app/src/test/java/com/foco/app/data/SettingsMigrationTest.kt` only.
- Ordering: waits for `structure-housekeeping` (declared dep — it owns the same `[@test]` and the same capability delta).
- No protected zones, no new dependencies. Tech brief: **yes** — the justification rests on a platform-SDK mechanism (`Build.VERSION.SDK_INT` under a mocked `android.jar`), so a walkthrough records it before archiving.
