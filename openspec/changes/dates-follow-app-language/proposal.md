# Proposal — dates-follow-app-language

## Why

Date labels do not follow the app's language: `Formatting.kt:24-35` formats with the JVM's default
locale (the **device** locale). The UI can be in English while the dates read `lun 21 oct` in
Spanish — or in French on a device without resources, since the app falls back to English text
(`app-settings:86-105`) but not for dates. The contract pins the formats to `Formatting.kt`
(`usage-tracking:45`: "Presentation formats SHALL remain those of `Formatting.kt`") yet never says
**which locale** those formats are rendered with, and `app-settings` (the language capability)
never extends the language choice to date labels. 🟡 spec-silent → /spec-add.

## What changes (proposed contract)

- New requirement in `app-settings`: dates and times the app shows are formatted with the app's
  active language — the same language the strings resolve in — not the device default.
- Implementation: `Formatting.kt` formats against the active app locale (the resolved
  Automático/Español/Inglés value), keeping every pattern the spec already pins.

## Alternatives

| Option | Effect | Verdict |
|---|---|---|
| Format with the app's resolved locale (explicit parameter or active-locale source) | dates match the UI everywhere; testable with plain JVM locales | **Proposed** |
| Force `Locale.setDefault()` at startup | process-global side effect, breaks other libraries and tests | not proposed |
| Leave device-locale dates (status quo) | the reported mismatch stays | not proposed |

## Risks

- Every call site must pass the active locale (or read it from one source): a missed call site
  keeps device-locale output — the JVM test covers the helpers; the device check covers the views.

## Origin

Filed 2026-10-21 by the anchored bug-finder audit (finding 15, severity low, 🟡 spec-silent);
`kind: feature` via /spec-add. Awaiting human acceptance — no code written.
