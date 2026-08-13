# Report 5 - Bug and test-gap report

Execution date: 2026-08-13
Target branch: `test/2026-08-02v-reports-final`

## Product defects observed

### BUG-FE-BUILD-001 - Frontend production build is blocked by unused imports/variable

- Severity: Major
- Component: `ELog-FE/src/pages/admin/routes/RouteListPage.tsx`
- Reproduction: run `npm run build` from `ELog-FE`.
- Actual: TypeScript reports unused `Segmented`, unused `ListIcon`, and unused `navigate`; build exits non-zero.
- Expected: production build completes successfully.
- Note for dev: production source was not changed because this test task explicitly excludes production fixes.

### BUG-FE-LINT-001 - Frontend lint baseline has 481 findings

- Severity: Major for CI readiness
- Reproduction: run `npm run lint` from `ELog-FE`.
- Actual: 461 errors and 20 warnings.
- Scope: includes production findings and test-config findings such as Chai expression rules.
- Expected: configured lint command exits zero for the committed source and intended test folders.

## Test infrastructure defects fixed in this branch

### TEST-INFRA-CYPRESS-001 - Cypress process inherited Electron-as-Node mode

- Root cause: `ELECTRON_RUN_AS_NODE=1` in the parent environment made Cypress Electron reject `--smoke-test` and `--ping`.
- Resolution: test commands remove the variable for the child process only; no machine configuration was modified.

### TEST-INFRA-E2E-002 - Full-stack login selector was stale English UI text

- Root cause: the real login page is Vietnamese while the helper selected English placeholders and button text.
- Resolution: the helper now binds to stable Ant Form field IDs and submit-button semantics.

### TEST-INFRA-MOBILE-003 - Flutter smoke test omitted Riverpod ProviderScope

- Root cause: `ELogDriverApp` is a `ConsumerWidget`, but the test mounted it without `ProviderScope`.
- Resolution: the test harness wraps the app in `ProviderScope`; Flutter smoke now passes.

## Invalid tests excluded from Pass

- Sixteen legacy `src/test/java/com/elog/integration/*IntegrationTest.java` classes contain TODO comments and unconditional `assertTrue(true)`. They were not accepted as L2 evidence.
- `src/test/nodejs/l3_system_tests.js` creates local `{ status: 200 }` objects rather than sending HTTP requests. It was not accepted as L3 evidence.
- Cypress UI specs that use `apiSuccess(...)` and mock local-storage tokens are component/UI tests, not L4 full-stack E2E evidence.
- The component/UI Cypress batch was stopped after exceeding five minutes; its partial result was 35 tests with 27 failures. This does not change L4 status because those specs are stubbed and outside the accepted L4 evidence boundary.

## Environment and data gaps (Not Run, not product defects)

- L2: 74 cases require isolated committed/rollback database fixtures. The existing generated classes are fake and were rejected; only the real consolidation transaction case is Pass.
- L3: 57 cases lacked a safe matching business fixture (trip execution, trip draft, outcome, exception, import workbook, or disabled-user credential). The real server was called, but 400/404 or missing multipart setup is classified Not Run rather than falsely reported as a product bug.
- L4: 45 journeys do not yet have a dedicated real-backend Cypress implementation covering every documented step. Stubbed UI tests are not promoted to Pass.
- Mobile L4: Flutter smoke passed, but no emulator/physical device with GPS and a seeded assigned driver trip was available.
- UAT: all 25 business decisions remain Not Run until linked L4 journeys pass and a business representative records sign-off.

## Verified passing infrastructure

- Backend unit/mapper baseline: 133 tests, 0 failures, 0 errors before the two new suites.
- Added direct catalog tests: TripStateMachine 7/7 and Haversine 4/4 Pass.
- MySQL/Flyway: application starts against MySQL on port 3307 and validates 53 migrations.
- L3 real HTTP runner: 130 catalog cases processed; 73 Pass and 57 Not Run after prerequisite classification.
- Full-stack Cypress: 5 real journeys Pass (login, users, stores, routes/map, vehicles).
- Flutter smoke: 1/1 Pass after test-harness repair.
