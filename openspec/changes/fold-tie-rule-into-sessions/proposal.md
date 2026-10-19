# Proposal — The transition rule has one owner

## Why

`/spec-verify` (2026-10-19) filed one medium finding on `same-millisecond-transitions`: it **ADDED** a requirement (*A transition settles by event order, not by strict inequality*) that restates a fact the living spec already owns — `Usage sessions are built from system events` says *"switching directly between apps closes the previous session at the new start"* (`openspec/specs/usage-tracking/spec.md:9`). After that change archived, one fact had two owners: edit one sentence and the other goes stale silently.

Two smaller drifts came out of reading those blocks side by side:

- The first requirement's `[@test]` points at `app/src/test/java/com/foco/app/data/SessionBuilderTest.kt`, **a file that does not exist** — the test lives in `domain/`. The gate's existence check only reads *deltas*, so a wrong path in a living spec is never caught.
- The zero-length rule and the *stop closes its own package* half only existed in the added requirement, so folding them in is what makes the surviving requirement the whole truth.

## What Changes

- `Usage sessions are built from system events` is **MODIFIED** to carry every clause: the direct switch (ties in the same millisecond included), the pause/stop half, the open-at-query rule, same-app transitions, and the zero-length rule. Its three scenarios stay and the two scenarios from the duplicate requirement are folded in, so nothing is lost.
- The duplicate requirement is **REMOVED**, with its reason (one fact, one owner) and its migration (same `[@test]`, same scenarios, folded above).
- The broken `[@test]` path is corrected to `app/src/test/java/com/foco/app/domain/SessionBuilderTest.kt`.
- No code, no test and no behavior changes: the contract is only re-homed.

## Proof (a refactor changes no behavior)

The suite is the proof, as the repo defines for a refactor: `SessionBuilderTest` (the fold's results, 13 cases) and every consumer of the sessions are untouched and green, and `openspec validate --all --strict` stays clean. No red-before to show — nothing was broken, a fact was written twice.

## Alternatives considered

| Option | Why yes | Why not / status |
|---|---|---|
| **Fold the duplicate into the sessions requirement (MODIFIED) and REMOVE it** | One owner per fact; keeps both scenario sets; fixes the wrong `[@test]` in the same pass | **Chosen** |
| Leave it as the finding records it (a small follow-up, unwritten) | Zero work | The duplication is exactly what the finding flagged; leaving it means the next edit to either sentence drifts — **rejected** |
| Keep both requirements and make the added one point to the other | Minimal edit | Two blocks still state the same rule, and the reader has to reconcile them — **rejected** |
| Tighten the *added* one and leave the original silent on ties | No MODIFIED to justify | The original keeps saying "closes at the new start" without the tie clause, so the tie rule would live only in the newcomer — backwards — **rejected** |

## Impact

- `openspec/specs/usage-tracking/spec.md` only (the delta lands there at archive time; the branch carries just this change's folder).
- No deps, no overlap: no active change claims the living spec file. No protected zones, no dependency, **no tech brief**.
