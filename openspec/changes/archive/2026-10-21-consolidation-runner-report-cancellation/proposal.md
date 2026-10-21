# Proposal

## Why

If `consolidate` throws `CancellationException`, the runner propagates it instead of reporting `Failed`, so a nightly worker receives cancellation rather than a retry.

## What Changes

- Treat a `CancellationException` from `consolidate` as `Failed` in `ConsolidationRunner.run`.

## Capabilities

### Modified Capabilities
- `usage-tracking`: cancellation outcome of the shared run.

## Impact

- `app/src/main/java/com/foco/app/data/ConsolidationRunner.kt`

## Expected outcome

A cancelled underlying run is reported as `Failed` and retried.