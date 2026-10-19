# Proposal — The repo's declared rules survive their own YAML

## Why

`openspec/config.yaml` declares the artifact rules the tooling enforces (`rules.proposal`, `rules.specs`, `rules.tasks`). Two of the seven items are fine; the third one of `proposal` is not:

```yaml
    - Persistence/state, concurrency, networking, platform SDKs, security or a new dependency → tech_brief: true → walkthrough + comprehension sign-off before archiving
```

An unquoted `: ` inside a YAML sequence item makes the parser read that item as a **mapping**, so OpenSpec discards **the whole artifact's rules** and says so on every `openspec new change`:

> `Rules for 'proposal' must be an array of strings, ignoring this artifact's rules. rules.proposal[2] is a mapping — an unquoted ": " makes YAML read the item as a key/value pair; quote the whole scalar to keep it a string.`

The defect is silent in every other path: `gate-check` does not read the rules, `openspec validate` does not check them, and the notice appears only when a change is created (scrolled past, as it was here). The consequence is real: the tech-brief rule the repo cares most about — *the brief is required before archiving when the change touches persistence, concurrency, networking, platform SDKs, security or a new dependency* — is announced to nobody, exactly because it is the sentence that contains the colon.

## What changes

- Every item under `rules:` in `openspec/config.yaml` becomes a **fully quoted** scalar (the failure mode is a `: ` anywhere in the text, and quoting is the fix that does not depend on remembering which items contain one). Text stays identical otherwise: the rule's wording is not touched.
- A JVM check, `SpecConfigRulesTest`, makes the property machine-checkable: for every item in every `rules.<artifact>` list, the raw line SHALL be a quoted scalar; otherwise the check fails naming `file:line` and the item. **It is red on today's tree** (the `proposal[2]` item) — that red is this `fix`'s regression proof.

No behavior, no state, no dependency: configuration text only.

## Regression

`kind: fix` requires a regression test and the property here is checkable from the file itself, so the check *is* the test (`SpecConfigRulesTest`). It is deliberately narrow — it does not try to reproduce YAML parsing, only the failure mode the tool reported: an item that starts unquoted and contains `: `. A quoted scalar is accepted, an unquoted `- key: value`-shaped item is not.

## Alternatives considered

| Option | Why yes | Why not / status |
|---|---|---|
| **Quote every `rules:` item + a JVM check for the quoting rule** | Fixes the defect for good, uniformly (any future `: ` cannot reintroduce it silently), and the check is what proves the fix | **Chosen (human, 2026-10-18)** |
| Quote only the offending item | One-line diff | The next item with a `: ` re-breaks it silently, and there is nothing to prove the fix with — **rejected** |
| Drop the `: ` from the text instead of quoting | No quotes needed | Rewrites the rule's wording to please a parser; the colon there is meaningful — **rejected** |
| Put the requirement in a new capability (`spec-tooling`) | The rule governs the repo's own tooling, not a feature | The human chose to keep it inside `code-structure` (2026-10-18) — **not chosen**; recorded because `code-structure`'s Purpose does not mention tooling configuration |

## Impact

- `openspec/config.yaml` (`rules:` items quoted), `app/src/test/java/com/foco/app/structure/SpecConfigRulesTest.kt` (new), and the delta lands in `openspec/specs/code-structure/spec.md` at archive time.
- Overlap: the new check file sits inside `app/src/test/**`, a glob three active drafts claim — today a glob-level warning (no shared file), exactly as recorded for `english-default-resources` in `DECISIONS.md` (2026-10-18: a small fix is not queued behind drafts). No `deps` for that; the only dep is the ordering below.
- `deps: [spec-requirement-budget]`: both changes land a delta in `code-structure`, so they must be applied one at a time — that declaration keeps the pair *sequenced* instead of letting the gate escalate when one of them is `applying`.
- Not a `tech_brief`: no code path, no state, no API, no dependency.
