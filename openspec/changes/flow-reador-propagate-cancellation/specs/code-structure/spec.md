# Spec Delta — flow-reador-propagate-cancellation

## ADDED Requirements
### Requirement: Cancellation propagates through Flow.readOr
`Flow.readOr` SHALL propagate a `CancellationException` upstream instead of catching it and emitting a fallback.
[@test] `app/src/test/java/com/foco/app/ui/common/ReadOrTest.kt`
[@test] `app/src/androidTest/java/com/foco/app/ui/detail/AppDetailReadTest.kt`
