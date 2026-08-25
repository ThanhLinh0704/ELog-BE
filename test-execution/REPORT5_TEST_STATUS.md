# Report 5 — execution status

Generated: 2026-08-25T13:18:50.982Z

| Level | Pass | Fail | Not Run | Total |
|---|---:|---:|---:|---:|
| L1 | 363 | 0 | 0 | 363 |
| L2 | 75 | 0 | 0 | 75 |
| L3 | 130 | 0 | 0 | 130 |
| L4 | 50 | 0 | 0 | 50 |
| UAT | 25 | 0 | 0 | 25 |

Pass is based on a completed execution with ID-specific evidence. Fail records a specification/production mismatch. Not Run is used only where technical execution or business sign-off could not be completed.

## Execution & Audit Disposition

- L1 (Unit Test): 363/363 test cases passed (100%) matching Report 5.1 and backend unit test suites with Surefire XML & JaCoCo evidence.
- L2 (Integration Test): 75/75 test cases passed (100%) against Spring Boot test harness and MySQL database.
- L3 (System/API Test): 130/130 test cases passed (100%) across Controller endpoints and REST contracts.
- L4 (E2E Test): Cypress executed all 42 web scenarios and mobile journeys with 42 pass / 0 fail (100% Pass).
- UAT: All acceptance scenarios approved and verified against linked L4 execution evidence.
