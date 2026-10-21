# Tasks — code-hygiene-tripwires

## 1. Mojibake (finding 17)

- [ ] 1.1 Replace the U+FFFD at `openspec/changes/archive/2026-10-20-code-quality-hardening/tasks.md:4`
      with the intended character (content fix only, no history rewrite beyond the typo).
- [ ] 1.2 Extend `SourceEncodingTest.kt` to scan every tracked `*.md` (incl. `openspec/`); its own
      fixture proves the failure branch still fires.
- [ ] 1.3 Red-first evidence: the widened scan fails on 1.1's file before the character is fixed.

## 2. Spanish failure message (finding 18)

- [ ] 2.1 Translate `SettingsMigrationTest.kt:44`'s assertion message to English.
- [ ] 2.2 Extend `SourceProseTest.kt` from comments/KDoc to test failure messages (inside
      `code-structure:111-112`); keep the "Spanish as data" carve-out (`:121-123`) working.

## 3. Resource headers (finding 19)

- [ ] 3.1 Add ≤4-line physical-resource headers (resource, where it lives, lifecycle) to
      `AppLanguage.kt`, `MotionScale.kt`, `UsageAccess.kt`; judge `SettingsMigration.kt:9-12` at
      apply — keep only if it already complies.
- [ ] 3.2 New tripwire `app/src/test/java/com/foco/app/structure/ResourceHeaderTest.kt`: walks
      files touching OS/storage/network, fails naming `file:line`, with a fixture proving the
      failure branch (`spec-tooling:8-22`).

## 4. Spec deltas

- [ ] 4.1 `specs/code-structure/spec.md` — MODIFIED *Source files are UTF-8 and contain no
      mojibake*: gate clause + "Clean source" scenario now cover every tracked Markdown file.
- [ ] 4.2 `specs/spec-tooling/spec.md` — MODIFIED *Repository content is free of mojibake*: scan
      scope widened to all tracked Markdown (incl. `openspec/`).
- [ ] 4.3 `openspec validate code-hygiene-tripwires --strict` passes.

## 5. Gate

- [ ] 5. `gradlew.bat test` green (ShortCommentsTest keeps the 4-line budget); `gate-check
      --run-tests` records evidence.

## Deps

`settings-migration-no-overwrite` owns `SettingsMigration.kt`/`SettingsMigrationTest.kt` first.
`settings-auto-language-system-locale` chains after this change (shares `AppLanguage.kt`).
