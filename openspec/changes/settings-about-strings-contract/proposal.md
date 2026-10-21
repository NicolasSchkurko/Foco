# Proposal — settings-about-strings-contract

## Why

The About block does not render what `app-settings` requires
(`openspec/specs/app-settings/spec.md:107-130`, from the archived
`2026-10-19-settings-about-and-pickers`):

| Spec | Shown today |
|---|---|
| credit `Open source project by Nicolas Schkurko` (`:110`) | `Made by Nicolas Schkurko` (`strings.xml:111`, `values-es:111`) |
| first line reads `Foco <version>` (`:121-122`) | `Version %1$s` (`strings.xml:112`, `values-es:112`) |
| link label `Git repository` (`:141`) | `Repository` (`strings.xml:114`, `values-es:114`) |

`SettingsScreenTest.kt:251-252` asserts the wrong text, which is why the suite never saw it.

**⚔ separate sub-item (declared in `.openspec.yaml`, not fixed here):** for an absent
`versionName`, the spec demands the app name alone, **no placeholder** (`:124-126`), while the code
follows `DECISIONS.md:394` ("falls back to an 'unknown' label") and shows `Version unknown`
(`strings.xml:113`). Either the code or the decision/spec must be reversed — the human's explicit
approval, never the agent's.

## What changes

- Align the credit, the first-line format and the link label (base + `es` resources, and the
  screen where the line is composed) with the spec; fix the test assertions so the suite pins the
  contract instead of the defect.
- The `unknown` placeholder string and its rendering stay **untouched** until the ⚔ conflict is
  resolved; the fix works with either outcome.

## Risks

- The test edit flips the direction of `SettingsScreenTest.kt:251-252`: it will fail until the
  strings change — that red is the regression evidence.
- `values-es` must keep the same key set as base (`app-settings:99-101`), so both files change
  together.

## Spec delta

`specs/app-settings/spec.md` MODIFIES *Settings shows a centered app identity block* — restated verbatim, including *The version is unavailable* (the ⚔'s subject, **not** touched), plus the scenario *The credit is not paraphrased* that pins what this fix aligns.

## Origin

Filed 2026-10-21 by the anchored bug-finder audit (finding 6, severity medium, 🔴 + ⚔);
`kind: fix` per /spec-triage with the conflict declared for the human. Awaiting human acceptance —
no code written.
