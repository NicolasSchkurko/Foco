# Proposal — The schema baseline cannot drift silently

## Why

`PLAN.md` Phase 2 closes with "schema migration from day one (good habit)". The foundation installs the habit's **materials**: `@Database(version = 1)`, `exportSchema = true`, `app/schemas/<db>/1.json` committed, and `fallbackToDestructiveMigration` banned in `design.md`. None of that *watches* anything. The failure it leaves open is the quiet one:

> someone adds a field to `SessionRecord`, forgets to bump the version, and KSP quietly **rewrites** `1.json` with the new identity hash. The build is green, the committed baseline now lies, and the first real upgrade has no migration to run.

The other quiet failure is cheaper to spot but also unguarded: a version bumped to 2 with no `2.json` committed, so nobody can prove what 1 looked like.

Today neither case has a test. This change turns the habit into a tripwire.

## What Changes

- `data/db/SchemaDrift.kt`: a pure decision — given the declared version, the versions that have a committed schema, the committed identity hash and the hash the open database reports, return the drift (or none). No Android types, so both branches are JVM-testable.
- `SchemaDriftTest` (JVM): the two failure branches (an edit without a bump; a bump without an export) and the consistent baseline. Red first, against a decision that always returns "fine".
- `SchemaBaselineTest` (instrumented): wires the decision to reality — opens the real database, reads its `room_master_table.identity_hash`, reads the committed `app/schemas/<db>/<version>.json`, and asserts no drift. This is the test that fails the day the model and the committed schema disagree.
- `NoDestructiveMigrationTest` (JVM): scans the production sources for `fallbackToDestructiveMigration` and fails if it appears. Its failure branch is proven against a `TemporaryFolder` fixture, the way `LayerRulesTest` proves its own.
- `app/build.gradle.kts`: expose `app/schemas/` as **androidTest assets**, so the instrumented test reads the same committed files the build exports.

## State and persistence mechanism

- **Nothing new is stored and no schema changes**: the database stays at version 1, with the same two tables the foundation defines.
- **What the guard reads**: `room_master_table.identity_hash` — the hash Room writes into the database when it creates it, derived from the entities — and `identityHash` inside the committed `app/schemas/<db>/<version>.json`. Both are metadata about the shape, never usage data.
- **Reused, not rebuilt**: the exported schemas, the committed `1.json` and the ban on destructive fallback all come from `usage-history-foundation`. This change adds no dependency and no database.
- **Deliberately not added**: `androidx.room:room-testing` / `MigrationTestHelper`. With a single version it has nothing to validate; it arrives with the first migration, together with that migration's data-survival test. Adding it now would be a dependency nobody uses.

## Expected outcome

The first schema change cannot land without a migration and its test: the suite names the missing piece before anyone decides to skip it. We will know it was worth it the first time a version bump turns the build red instead of erasing a week of history on someone's phone.

## Alternatives considered

Option 0 is the substrate this change reuses rather than rebuilds: the foundation's `exportSchema` + committed `1.json` + the no-destructive rule.

| Option | Why yes | Why not / status |
|---|---|---|
| **Pure drift decision + real-database wiring + a source scan for destructive fallback (chosen)** | Both silent failures get a failing test, no new dependency, both branches proven | The instrumented half only runs on a device; the scan only proves absence, not that a migration exists for a bump — **chosen by the human (2026-10-19)** |
| `room-testing` and `MigrationTestHelper` now | The full harness, ready for the first migration | With only version 1 there is nothing to migrate: an unused dependency plus androidTest schema assets for a test that cannot fail — **deferred to the first real migration** |
| Invent a version 2 to exercise a migration (an index the Week read would use) | Proves the whole path end to end | `daily_aggregate` holds roughly 350 rows after a week: the index would be noise, and the migration would exist to be tested — **rejected by the human (2026-10-19)** |
| `fallbackToDestructiveMigration` and no tests | Zero cost, never crashes on upgrade | It deletes the stored history precisely when the schema changes — the opposite of what the foundation protects — **rejected** |
| Trust review and the written policy | No code | Nothing fails when someone forgets; that is exactly the failure being addressed — **rejected** |

## Non-goals

- **No schema bump**: version 1 stays; the first migration's data-survival test belongs to the change that bumps.
- **No new dependency** and no `MigrationTestHelper` yet (see above).
- **No migration of user data**, no backup/restore, no export: nothing about the stored content changes.
- **No runtime cost**: the guard is tests only; the app ships the same bytes.

## Impact

- **Test-only (`kind: refactor`)**: no observable behavior changes; the proof is the green suite plus the three regression tests, each of which fails when its invariant is broken.
- Depends on `usage-history-foundation` (archived): it shares `app/build.gradle.kts` with it, so the gate treats the overlap as sequenced; `app/schemas/` must exist before the baseline test can read it.
- Also declares `nightly-consolidation` as a dep for **sequencing only**: both edit `app/build.gradle.kts` (its WorkManager dependency against the androidTest assets here), and there is no code dependency in either direction. It lands last of the two.
- No protected zone (`openspec/config.yaml` → `protected: []`).
- `tech_brief: false`: no dependency, no storage change, no concurrency; the mechanism above is the whole story.
