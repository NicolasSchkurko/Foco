# Proposal — The lookback requirement states its bound

## Why

`/spec-verify` on `cross-midnight-sessions` (archived 2026-10-18) filed this as finding 1: the requirement it landed says the query SHALL look back *"far enough to find the event that opened the session"*, while the code bounds the lookback at the **start of the previous day** (`app/src/main/java/com/foco/app/data/UsageRepository.kt:67`). A reader of the living contract would conclude that any session is recovered; in fact an app in the foreground for **more than 24 hours** has no opening event inside the window and its stop is still discarded — the limit is written in that change's walkthrough but not in the contract.

The human chose **route A** (2026-10-19): state the bound in the requirement, so the living spec stops promising more than the code does. Route B (accept it as documented) was the alternative, and widening the code was never on the table: an unbounded query would pay for the whole history.

## What Changes

- The requirement *A session already open at the day's start counts from the day's start* (living `usage-tracking`) is reworded: the query SHALL look back **to the start of the previous day — one day, not further**; the clamp clause is unchanged.
- The two existing scenarios stay, and a third one states the bound as behavior: an app in the foreground for more than a day yields no session for the new day.
- Nothing else: no code, no test, no new requirement, no state.

## Regression (why this `[@test]`)

`kind: fix` needs a regression test, and for a **wording** fix the honest proof is the test the requirement already declares — `SessionBuilderTest` is what would break if the meaning had drifted (it pins the clamp arithmetic and the crossing cases). The second oracle is the validator: `openspec validate --all --strict` must stay clean, which is why the reworded text is kept inside the tool's text budget.

## Alternatives considered

| Option | Why yes | Why not / status |
|---|---|---|
| **Route A — reword the requirement to state the one-day bound, plus a scenario for the long case** | The contract tells the truth; the limit becomes readable where it matters and cannot be forgotten | **Chosen (human, 2026-10-19)** |
| Route B — accept the bound as documented in the walkthrough | Zero work | The living requirement keeps over-promising, which is exactly what the finding flagged — **rejected** |
| Widen the code (a longer lookback, or persist "what was in the foreground at midnight") | Removes the limit | An unbounded query pays for the whole history, and the persisted flag was already rejected when the fix was designed — **rejected** |
| A new requirement "sessions older than one day are not recovered" | States the limit as its own rule | Two requirements for one behavior; the bound belongs in the sentence that creates it — **rejected** |

## Impact

- `openspec/specs/usage-tracking/spec.md` only (the delta lands there at archive time; the branch carries just this change's folder).
- No deps and no overlap: no active change claims a living spec file for its targets. No protected zones, no new dependency, **no tech brief** (contract text only).
