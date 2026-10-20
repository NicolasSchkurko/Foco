# Tasks

## 1. The decision first (pure, both branches, red before green)

- [x] 1.1 `data/db/SchemaDrift.kt`: a pure decision taking the declared version, the versions that have a committed schema, the committed identity hash and the runtime hash; returns a drift description or none. No Android types, no I/O.
- [x] 1.2 `SchemaDriftTest` (JVM): an edit without a bump (hashes differ), a bump without an export (missing file), and the consistent baseline. Write them **before** 1.1 and watch the always-fine version fail them.

## 2. Wiring to the real database

- [x] 2.1 `app/build.gradle.kts`: expose `app/schemas/` as androidTest assets (`sourceSets["androidTest"].assets.srcDir("$projectDir/schemas")`) so the test reads the same committed files the build exports.
- [x] 2.2 `SchemaBaselineTest` (instrumented): open the real database, read `room_master_table.identity_hash`, parse the committed `<version>.json`, and assert the decision reports no drift.
- [x] 2.3 Confirm it can fail: temporarily point the test at a copy of the json with a different hash and watch it report drift — then revert (the fixture stays out of the suite; 1.2 already owns the failure branch).

## 3. The destructive-fallback scan

- [x] 3.1 `NoDestructiveMigrationTest` (JVM): walk the production sources, fail on any `fallbackToDestructiveMigration` call, naming the file.
- [x] 3.2 Prove the branch: the same scan against a `TemporaryFolder` fixture containing the call must fail — the pattern `LayerRulesTest` already uses for its own check.

## 4. Verify

- [x] 4.1 `openspec validate --all --strict` and `node scripts/gates/gate-check.mjs --run-tests`.
- [x] 4.2 Instrumented run for 2.2 (the known `UnlockCountTest` flake stays unrelated).
- [x] 4.3 By hand: edit an entity field without bumping the version, rebuild, and confirm the instrumented test fails with the drift message; revert.

## Workflow follow-up

- `kind: refactor`: the green suite plus the regression tests is the proof; no tech brief.
- Depends on `usage-history-foundation`; lands after it is `verified` (it needs the committed `1.json`). Leave `status: draft` until the human accepts.
- The first change that bumps the version owes the migration **and** its data-survival test (`MigrationTestHelper` arrives there).
