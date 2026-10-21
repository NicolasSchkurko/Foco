# Spec Delta

## ADDED Requirements

### Requirement: A stored day keeps one calendar
A consolidated day SHALL be read with the calendar it was written under: the store SHALL slice the day's stored sessions with the day's own recorded zone offset, so its stored totals and its stored sessions always answer the same day. A device zone change SHALL move only the days still being built; stored rows SHALL NOT be re-assigned to another day.
[@test] `app/src/test/java/com/foco/app/data/HistoryDayZoneTest.kt`

#### Scenario: The device zone changes after consolidation
- **WHEN** a day consolidated under one zone is read while the device is in another zone
- **THEN** its sessions fall in the same day as its stored totals, and no session appears in the neighbouring day

#### Scenario: The week bar and the detail agree
- **WHEN** a stored day is shown as a week bar and opened in the detail
- **THEN** both answer the same total for that day

#### Scenario: Days still being built
- **WHEN** the zone changes while today is not yet consolidated
- **THEN** today's live read follows the current zone, as it does today

#### Scenario: The prune window is unchanged
- **WHEN** a consolidation run prunes rows outside the retention window
- **THEN** pruning and the read window use the current system zone, exactly as decided in DECISIONS.md (2026-10-19, history-storage-followups)
