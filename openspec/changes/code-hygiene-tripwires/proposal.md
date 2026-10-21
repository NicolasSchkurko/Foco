# Proposal — code-hygiene-tripwires

## Why

Three violations of `code-structure` that the current tripwires cannot see (findings 17-19 of the
anchored audit, severity low, contract-violating):

1. **Mojibake outside the gate's scope** — one U+FFFD in
   `openspec/changes/archive/2026-10-20-code-quality-hardening/tasks.md:4` (the only one in the
   repository, ripgrep-verified) against `code-structure:133-146` ("All source and documentation
   files … SHALL NOT contain replacement characters"). The gate misses it because
   `SourceEncodingTest` scans `app/src` + top-level `*.md`, and `spec-tooling:81-83` **declares**
   that scope — the fix removes the character **and** widens the declared scan to every Markdown
   file in the repo, so the rule the requirement states is the rule that runs.
2. **Spanish test failure message** — `SettingsMigrationTest.kt:44` ("En Windows DataStore no
   puede…") against `code-structure:111-112` (test failure messages SHALL be English **and a
   machine check SHALL enforce it**). `SourceProseTest` only inspects comments, so enforcement is
   narrower than the requirement: the fix translates the message and extends the check to failure
   messages — inside the existing contract, no delta needed for this part.
3. **Missing physical-resource headers** — `AppLanguage.kt:7-9`, `MotionScale.kt:6`,
   `UsageAccess.kt:9-12` (and `SettingsMigration.kt:9-12`, to be judged at apply if its header
   already names resource + place + lifecycle) do not carry the header the constitution demands,
   against the scenario at `code-structure:99-101`, inside the four-line budget
   (`code-structure:89-101`).

## What changes

- Replace the U+FFFD with the intended character; widen `SourceEncodingTest` (and the two spec
  clauses that declare its scope) to all tracked Markdown.
- Translate the failure message; extend `SourceProseTest` to failure messages.
- Add the missing headers (≤4 lines each) and a `ResourceHeaderTest` tripwire with its own
  failure-branch fixture (`spec-tooling:8-22` requires tripwires to prove they fire).

## Alternatives

| Option | Effect | Verdict |
|---|---|---|
| Widen the encoding scan to every tracked `.md` (incl. `openspec/`) + fix the character | the rule as written becomes the rule that runs; recurrence is caught | **Proposed** |
| Fix only the character, leave the declared scope | the same defect can return invisibly in any archive/change doc | not proposed |

## Risks

- The wider scan must be clean repo-wide at apply (verified: this is the only U+FFFD today).
- `ResourceHeaderTest`'s fixture must not trip `OneTypePerFileTest`/`LayerRulesTest` — synthetic
  trees stay under the test source set as the existing tripwires do.

## Origin

Filed 2026-10-21 by the anchored bug-finder audit (findings 17-19, severity low); `kind: fix` per
/spec-triage, carrying the scope deltas because the tooling spec currently licenses the gap.
Sequenced after `settings-migration-no-overwrite` (shares `SettingsMigration.kt` /
`SettingsMigrationTest.kt`). Awaiting human acceptance — no code written.
