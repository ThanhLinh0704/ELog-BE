# Technical UAT execution disposition

Execution date: 2026-08-14

All UAT IDs below were technically reviewed against their linked L4 evidence. Formal UAT requires a named business representative to make an acceptance decision; that approval cannot be self-executed by the test team or automation.

Current linked L4 evidence:

- L4 Web was executed against the real frontend/backend/database: 42 executed, 29 raw Cypress pass, 13 raw Cypress fail.
- The audit-conservative L4 ledger records 4 Pass / 39 Fail / 7 Not Run because surface-only web journeys are not counted as Pass without full catalog action/outcome evidence.
- L4 Mobile `L4-MOB-AUTH-01` was executed on Android emulator and passed.
- The remaining 7 mobile journeys do not yet have executable integration-test automation.

UAT IDs held as formal Not Run:

- ELOG-INTAKE-02
- ELOG-PLAN-01
- ELOG-PLAN-02
- ELOG-PLAN-03
- ELOG-CAPACITY-01
- ELOG-CAPACITY-02
- ELOG-RECOMMEND-01
- ELOG-RECOMMEND-02
- ELOG-CONFIRM-01
- ELOG-CONFIRM-02
- ELOG-ASSIGN-01
- ELOG-MANIFEST-01
- ELOG-DISPATCH-01
- ELOG-MONITOR-01
- ELOG-EXCEPTION-01
- ELOG-OUTCOME-01
- ELOG-OUTCOME-02
- ELOG-KPI-01
- ELOG-AUDIT-01
- ELOG-MASTER-01
- ELOG-ACCESS-01
- ELOG-FLEET-01
- ELOG-DRIVER-02
- ELOG-DRIVER-03
- ELOG-SESSION-01
