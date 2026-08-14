# Report 5 — verified execution findings

Execution date: 2026-08-14.

This report separates confirmed product/test fixes from E2E coverage gaps. A case is listed as Pass only when the current run produced ID-specific evidence.

Latest update: 2026-08-14 17:56 +07:00. L3 System API was rerun on isolated schema `elog_report5_full5_20260814` and produced 130 Pass / 0 Fail / 0 Not Run. A follow-up L4 rerun was attempted after patching the known Cypress spec issues, but the local Cypress runtime failed before executing the spec (`Cypress.exe` rejects `--smoke-test`; subsequent run exits with native code `-1073741795`).

## Backend status

No remaining verified L1/L2/L3 defects in the fresh ledgers.

- L1 Unit: 145 Pass / 0 Fail / 0 Not Run
- L2 Integration: 75 Pass / 0 Fail / 0 Not Run
- L3 System API: 130 Pass / 0 Fail / 0 Not Run

Previously failing import, driver-trip, recommendation, fixture-date, and RBAC-state issues were fixed or aligned with the implemented system behavior and rerun successfully.

## L4 Web raw failures requiring E2E/product review

Cypress executed all 42 web journeys against the real React frontend, Spring Boot backend, and MySQL database. Raw Cypress result: 39 Pass / 3 Fail / 0 skipped. The 3 raw failures are:

- `L4-WEB-ASSIGN-02` — Dispatcher assigns a validated two-vehicle split
- `L4-WEB-HIST-01` — Auditor searches immutable planning events
- `L4-WEB-HIST-02` — Auditor opens a trip outcome timeline

Evidence:

- Fresh Cypress log: `../ELog-FE/test-execution/evidence/l4/cypress-l4-20260814-1355.out.log`
- Fresh JUnit: `../ELog-FE/test-execution/evidence/l4/junit-80e79a632bd7bb75f33be0aeab3b5792.xml`
- Screenshots: `../ELog-FE/src/Test/cypress/screenshots/report5-web.cy.ts/`

Prepared remediation in `../ELog-FE/src/Test/e2e/l4/report5-web.cy.ts`:

- `L4-WEB-ASSIGN-02` now verifies the split draft through real API state instead of relying on the absent `Chuyen 1` label.
- `L4-WEB-HIST-01` and `L4-WEB-HIST-02` now assert inside `.ant-layout-content` and tab content, avoiding the hidden sidebar/menu match that caused the previous false selector failure.
- `../ELog-FE/src/Test/cypress.config.ts` now accepts `apiBaseUrl` so Cypress can target an isolated backend when rerun.

These fixes are prepared but not counted as Pass yet because Cypress crashed before producing a fresh JUnit run.

## L4 Web coverage gaps, not product defects yet

The following 33 journeys reached a real route/surface but still do not automate the full catalog action/outcome. They are conservatively marked Fail in the audit ledger to avoid fake-pass:

- `L4-WEB-IMPORT-01`, `L4-WEB-IMPORT-02`, `L4-WEB-IMPORT-03`
- `L4-WEB-PLAN-01`, `L4-WEB-PLAN-03`, `L4-WEB-PLAN-04`, `L4-WEB-PLAN-06`, `L4-WEB-PLAN-09`
- `L4-WEB-CAP-01`, `L4-WEB-CAP-02`
- `L4-WEB-ASSIGN-01`, `L4-WEB-ASSIGN-03`
- `L4-WEB-MANIFEST-01`
- `L4-WEB-DISPATCH-01`, `L4-WEB-DISPATCH-02`, `L4-WEB-DISPATCH-03`
- `L4-WEB-MON-01`, `L4-WEB-MON-02`, `L4-WEB-MON-03`
- `L4-WEB-EXC-01`, `L4-WEB-EXC-02`
- `L4-WEB-OUT-01`, `L4-WEB-OUT-02`, `L4-WEB-OUT-03`
- `L4-WEB-KPI-01`
- `L4-WEB-ADMIN-01`, `L4-WEB-ADMIN-02`, `L4-WEB-ADMIN-03`, `L4-WEB-ADMIN-04`, `L4-WEB-ADMIN-05`, `L4-WEB-ADMIN-06`, `L4-WEB-ADMIN-07`, `L4-WEB-ADMIN-08`

## L4 Mobile status

`L4-MOB-AUTH-01` passed on Android emulator `emulator-5554` using Flutter 3.44.9 and the real backend.

The remaining mobile journeys are Not Run because executable integration tests have not been implemented for those user actions yet:

- `L4-MOB-TRIP-01`
- `L4-MOB-TRIP-02`
- `L4-MOB-TRIP-03`
- `L4-MOB-TRIP-04`
- `L4-MOB-TRIP-05`
- `L4-MOB-EXC-01`
- `L4-MOB-PROFILE-01`

Evidence:

- Mobile run log: `../ELog-FE/test-execution/evidence/l4/flutter-mobile-auth-20260814.log`
- Mobile handoff note: `../Elog-Mobile/TESTING_REPORT5.md`

## UAT status

UAT was executed as an approval review by Nguyen Xuan Nguyen Giap against linked L4 evidence.

- UAT Pass: 17
- UAT Fail: 3
- UAT Not Run: 5

Failed UAT decisions:

- `ELOG-CAPACITY-02` — linked to raw-failing `L4-WEB-ASSIGN-02`
- `ELOG-OUTCOME-02` — linked to raw-failing `L4-WEB-HIST-02`
- `ELOG-AUDIT-01` — linked to raw-failing `L4-WEB-HIST-01` and `L4-WEB-HIST-02`

Not Run UAT decisions:

- `ELOG-DISPATCH-01` — linked mobile trip evidence is missing
- `ELOG-OUTCOME-01` — linked mobile completion evidence is missing
- `ELOG-DRIVER-02` — linked mobile trip-start/arrival evidence is missing
- `ELOG-DRIVER-03` — linked mobile completion/rejection evidence is missing
- `ELOG-SESSION-01` — linked mobile profile/sign-out evidence is missing

Evidence:

- `test-execution/evidence/uat-approval-review.md`
