# Code Structure - UI Organization

## Requirements

### Modular UI Common and Metrics Packages
The UI layer SHALL organize shared components and metrics data models into logical domain subpackages.
[@test] `app/src/test/java/com/foco/app/structure/LayerRulesTest.kt`
[@test] `app/src/test/java/com/foco/app/ui/common/ring/RingMotionTest.kt`
[@test] `app/src/test/java/com/foco/app/ui/common/util/ReadOrTest.kt`
[@test] `app/src/test/java/com/foco/app/ui/metrics/AppDetailMetricsTest.kt`
[@test] `app/src/test/java/com/foco/app/ui/metrics/UsageMetricsTest.kt`
[@test] `app/src/test/java/com/foco/app/ui/metrics/WeekMetricsTest.kt`

#### Scenario: Metrics subpackages
- **WHEN** reading or modifying metrics models
- **THEN** app detail metrics reside in `ui/metrics/detail/`, daily metrics in `ui/metrics/day/`, and weekly metrics in `ui/metrics/week/`

#### Scenario: Shared UI subpackages
- **WHEN** using reusable UI components
- **THEN** components are grouped under `ui/common/` by function (`ring`, `apps`, `charts`, `time`, `layout`, `limits`, `util`)
