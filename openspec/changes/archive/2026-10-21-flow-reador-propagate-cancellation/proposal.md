# Proposal

## Why

The Flow extension of `readOr` catches `CancellationException` and emits a fallback, breaking structured cancellation.

## What Changes

- Re-throw `CancellationException` in the Flow extension, matching the suspend version.
- `[@test] app/src/androidTest/java/com/foco/app/ui/detail/AppDetailReadTest.kt`

## Capabilities

### Modified Capabilities
- (none - skip_specs)

## Impact

- `app/src/main/java/com/foco/app/ui/common/ReadOr.kt`

## Expected outcome

Cancellation propagates instead of emitting a fallback.