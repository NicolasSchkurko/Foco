# Tasks

## 1. The check first (it is the regression proof)

- [x] 1.1 `app/src/test/java/com/foco/app/structure/SpecConfigRulesTest.kt` (JVM, module dir as cwd like `LayerRulesTest`): read `../openspec/config.yaml`, take the `rules:` block, and for every `- <item>` line fail when the item is not a **fully quoted** scalar (starts with `"` or `'`), naming `file:line` and the item. Line-based on purpose: no YAML library, so it tests the shape the parser trips on and nothing else.
- [x] 1.2 Run it on the **pre-change tree**: it must fail exactly on `rules.proposal[2]` (the tech-brief rule) — that red is this fix's proof, and it names the item the tool warned about.
- [x] 1.3 Sanity: the check must accept the other six items once they are quoted, and must not read anything outside the `rules:` block.

## 2. The fix

- [x] 2.1 Quote all seven items under `rules:` (`proposal` × 3, `specs` × 2, `tasks` × 1 — six lines today, one of them already quoted): wrap the scalar in `"`, wording byte-identical apart from the quotes. The tech-brief rule keeps its colon.
- [x] 2.2 Confirm the tool now takes the rules: `openspec new change tmp-rules-notice` must **not** print *"Rules for 'proposal' must be an array of strings, ignoring this artifact's rules"*; delete the throwaway folder afterwards (`Remove-Item -Recurse openspec/changes/tmp-rules-notice`).
- [x] 2.3 `openspec validate --all --strict` still reports only what it reported before (nothing new from the config).

## 3. Verify

- [x] 3.1 `gradlew.bat test` green — `SpecConfigRulesTest` (now clean) plus the untouched suite.
- [x] 3.2 `node scripts/gates/gate-check.mjs --run-tests` (green evidence); update `gitref` to the branch tip.

## Workflow follow-up

- Review the apply report, merge, then archive with `gate-check --pre-archive spec-config-rules-quoted` (the delta lands in `code-structure` at archive time).
- Order: `deps: [spec-requirement-budget]` — both changes touch `code-structure`, so they are applied one at a time, in that order.
- Known caveat, reported in the proposal: the requirement lands under `code-structure` although it governs the repo's tooling configuration, and an OpenSpec delta cannot edit a Purpose. If that reads wrong later, moving it to its own capability is a small change.

### Notes on the apply (2026-10-19)

- **Red before / green after, both recorded.** With the config as it was (five items unquoted), `testDebugUnitTest --tests "*SpecConfigRulesTest*"` reported `everyRulesItemIsAQuotedScalar FAILED` naming `openspec/config.yaml` lines 17, 19, 21, 22 and 24. After quoting all six `rules:` items (wording byte-identical apart from the quotes) the test passes and the whole suite is green.
- **The tool's own notice is gone**: `openspec new change <tmp>` no longer prints *"Rules for 'proposal' must be an array of strings, ignoring this artifact's rules"* — checked with a throwaway change that was deleted straight away.
- The lint reads `../openspec/config.yaml` (Gradle runs unit tests with the module dir as cwd), line-based on purpose: no YAML library, so it tests the shape the parser trips on and nothing else.
