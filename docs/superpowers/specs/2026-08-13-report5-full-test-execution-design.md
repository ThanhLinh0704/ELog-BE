# Report 5 Full Test Execution Design

## Objective

Implement and execute the Report 5 L1, L2, L3, L4 and UAT catalogs against the current ELog repositories. A case is Pass only when an executable test or controlled acceptance check produces matching evidence. Existing placeholder tests cannot contribute a Pass.

## Scope and repository baseline

- Backend, frontend and mobile work on `test/2026-08-02v-reports-final`.
- Production source is read-only for this effort. Test code, fixtures, runners, configuration scoped to tests, evidence and reports may change.
- Findings in production behavior become bug reports; they are not silently fixed.
- The authoritative catalog contains 425 cases: L1 145, L2 75, L3 130, L4 50 and UAT 25.

## Execution architecture

1. A catalog-aware result ledger maps every Report 5 Test ID to one execution result and evidence path.
2. L1 uses JUnit/Mockito for backend units plus focused frontend/mobile unit tests where the catalog points to those clients.
3. L2 uses Spring integration tests against the configured MySQL/Flyway schema and real repositories/services. External HTTP boundaries use deterministic local stubs.
4. L3 starts the real backend and executes HTTP contract tests for the 118 primary mappings plus the 12 documented risk variants.
5. L4 uses real Cypress browser actions for web. Mobile cases use Flutter integration/widget execution when the local toolchain supports it; otherwise they remain Not Run with the missing capability recorded.
6. UAT is a technical dry-run. Objective, machine-verifiable acceptance criteria may Pass or Fail. Product-owner judgment or sign-off remains Not Run until a stakeholder performs it.

## Result policy

- `Pass`: assertion executed and matched the documented outcome.
- `Fail`: assertion executed and demonstrated a product defect or a contract mismatch.
- `Not Run`: execution was impossible after reasonable local setup because an external account, service, device, stakeholder decision or unavailable toolchain was required.
- Infrastructure and test-code faults must be repaired before classification; they are not product failures.

## Deliverables

- Executable tests and fixtures in their owning repositories.
- Raw logs, screenshots, reports and structured JSON/CSV evidence.
- A single Markdown status matrix covering all 425 IDs with Pass/Fail/Not Run, evidence and defect references.
- A defect report containing reproducible steps, expected/actual results and affected test IDs.

## Safety and quality gates

- No fake assertions such as `expect(true).toBe(true)` are accepted.
- No Pass is inferred from source inspection alone.
- Test data cleanup is deterministic and must not destroy user-owned data.
- The final report reconciles exactly 425 unique catalog IDs.
