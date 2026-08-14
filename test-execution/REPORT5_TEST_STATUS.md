# Report 5 — execution status

Generated: 2026-08-14T17:56:03+07:00

| Level | Pass | Fail | Not Run | Total |
|---|---:|---:|---:|---:|
| L1 | 145 | 0 | 0 | 145 |
| L2 | 75 | 0 | 0 | 75 |
| L3 | 130 | 0 | 0 | 130 |
| L4 | 7 | 36 | 7 | 50 |
| UAT | 17 | 3 | 5 | 25 |

Pass is based on a completed execution with ID-specific evidence. Fail records a specification/production mismatch. Not Run is used only where technical execution or business sign-off could not be completed.

## Failures requiring development decision

- L1/L2/L3: no remaining failing catalog IDs in the fresh executed ledgers.
- L3 was rerun on 2026-08-14 against isolated MySQL schema `elog_report5_full5_20260814`; result: 130 Pass / 0 Fail / 0 Not Run. Evidence: `test-execution/evidence/l3-rerun-results.json`.
- L4 Web: Cypress executed all 42 web journeys against the real FE/BE. Raw Cypress result is 39 pass / 3 fail; audit-conservative ledger keeps only fully asserted journeys as Pass and marks incomplete surface-only journeys as Fail to avoid fake-pass.
- L4 rerun attempt on 2026-08-14 was blocked by local Cypress runtime before spec execution: `Cypress.exe` rejects `--smoke-test`; after reinstall/pin attempts the runner exits with Windows native code `-1073741795`. The L4 spec has been patched for the known `ASSIGN-02` and activity-history selector issues, but no new Cypress JUnit can be claimed until the local Cypress binary/runtime opens successfully.
- L4 Mobile: `L4-MOB-AUTH-01` passed on Android emulator; the remaining mobile journeys are Not Run because only the auth integration journey has executable automation so far.

## UAT disposition

UAT was executed as an approval review by Nguyen Xuan Nguyen Giap against linked L4 evidence. Pass requires executed linked evidence without raw execution failure; Fail means linked L4 failed; Not Run means linked mobile/e2e evidence is still missing.
