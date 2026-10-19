# Tasks

## 1. Create the capability

- [x] 1.1 Write `openspec/specs/spec-tooling/spec.md` with its Purpose and the four moved requirements, text and `[@test]` lines unchanged; verify `openspec show spec-tooling --type spec` lists all four.
- [x] 1.2 Remove the four requirements from `openspec/specs/code-structure/spec.md`; verify `openspec show code-structure --type spec` lists only the five tree requirements.
- [x] 1.3 Run `/diagram-sync` and decide the `cap:spec-tooling` tag: either model the checks as a development component or record why the warning stands; verify the gate's capability warning is the intended one.

## 2. Verify

- [x] 2.1 `node scripts/gates/gate-check.mjs` is green (nothing else in flight touches these spec files).
- [x] 2.2 `openspec validate spec-tooling-split --strict` is clean.

### Notes on 1.3 (apply of 2026-10-19)

- **Decided: modeled, not accepted-as-warning.** `ARCHITECTURE.dsl` gains a `Development checks` software system (sibling of the app and of the platform) tagged `cap:spec-tooling`, with one relationship into the app. The gate's tag check matches the tag anywhere in the DSL, so a non-app element is legitimate — and it tells the truth: the checks live in `app/src/test/…/structure/`, `scripts/gates/` and CI, none of which is an app component. `ARCHITECTURE.dsl` is declared as a target of this change for that reason.
- `/diagram-sync` itself was **not** run (this session cannot invoke the command); the edit was made by hand and the DSL was validated with `node scripts/gates/build-map.mjs` plus the gate's own tag check, which is the warning 1.3 asked about.
- The ADR in `DECISIONS.md` (2026-10-19) recorded this as an open point; its consequence bullet now carries the resolution.

## Workflow follow-up

- Archive after `node scripts/gates/gate-check.mjs --pre-archive spec-tooling-split` is OK.
