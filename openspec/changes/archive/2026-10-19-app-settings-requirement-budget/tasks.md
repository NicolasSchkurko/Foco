# Tasks — app-settings-requirement-budget

- [x] 1. Rewrite the appearance requirement in the change's delta, under the strict budget, copying all
      five scenarios and both `[@test]` lines unchanged.
- [x] 2. `openspec validate app-settings-requirement-budget --strict` clean; `gate-check --run-tests`
      green (the suite proves nothing else moved).
- [x] 3. After the archive: `openspec validate --all --strict` back to 8/8.
