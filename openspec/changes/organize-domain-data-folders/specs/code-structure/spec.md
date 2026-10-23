# Code Structure - Domain and Data Packages

## Requirements

### Modular Domain and Data Packages
The `domain/` and `data/` layers SHALL organize their logic and adapters into domain subpackages.
[@test] `app/src/test/java/com/foco/app/structure/LayerRulesTest.kt`
[@test] `app/src/test/java/com/foco/app/domain/ring/DayRingTest.kt`
[@test] `app/src/test/java/com/foco/app/data/stores/SettingsMigrationTest.kt`

#### Scenario: Domain subpackages
- **WHEN** reading or adding domain rules
- **THEN** classes reside in their respective domain subpackages (`ring`, `limits`, `history`, `sessions`, `monitor`, `consolidation`, `export`)

#### Scenario: Data subpackages
- **WHEN** reading or adding data adapters or stores
- **THEN** classes reside in their respective data subpackages (`stores`, `monitor`, `consolidation`, `alerts`, `system`, `export`, `db`)
