# Proposal

## Why

The periodic refresh loop in `DashboardScreen` calls `load()` without handling exceptions. If it throws, the `LaunchedEffect` coroutine cancels and automatic refresh stops until process restart, violating the refresh requirement.

## What Changes

- Wrap the periodic `load()` in a try/catch that logs and keeps the loop running, preserving the last good data.

## Capabilities

### Modified Capabilities
- (none - code-only fix against the existing refresh requirement)

## Impact

- `app/src/main/java/com/foco/app/ui/dashboard/DashboardScreen.kt`

## Expected outcome

The refresh loop survives a failed read and keeps retrying on the next interval.