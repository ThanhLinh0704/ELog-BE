# Report 5 L1-L3 Fix And UAT Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bring Report 5 L1/L2/L3 failures as close to green as the current system contract allows, then produce technical UAT evidence without claiming business sign-off.

**Architecture:** Keep existing Report 5 tests as regression tests and fix production behavior only where the catalog is consistent with the source reports. Treat runner/fixture failures as test infrastructure work, not production defects. UAT status is technical rehearsal only unless a business representative signs off.

**Tech Stack:** Java/Spring Boot/Maven for backend, Node.js runner for L3 API, Markdown/JSON status artifacts for evidence.

## Global Constraints

- Do not commit or push automatically; `CLAUDE.md` requires changes to remain unstaged.
- Do not mark a case Pass without fresh execution evidence.
- Do not change production code to hide defects; fix root cause or record Fail/Not Run.
- UAT business acceptance cannot be self-signed by the agent.

---

### Task 1: Fix Import Contract Failures

**Files:**
- Modify: `src/main/java/com/elog/service/impl/ImportServiceImpl.java`
- Test: `src/test/java/com/elog/service/ImportServiceImplReport5Test.java`
- Test: `src/test/java/com/elog/integration/ImportServiceReport5IntegrationTest.java`

**Interfaces:**
- Consumes: existing `ImportService.importExcel(...)` and Report 5 L1/L2 tests.
- Produces: passing `L1-IM-02`, `L1-IM-09`, `L1-IM-17`, and `L2-IMP-03`.

- [ ] **Step 1: Verify RED**

Run: `mvn -q -Dtest=ImportServiceImplReport5Test,ImportServiceReport5IntegrationTest test`
Expected: the four import-related Report 5 assertions fail before production changes.

- [ ] **Step 2: Implement minimal import fixes**

Change `confirmReplace=true` to deactivate prior active import batches for the same delivery date, change unknown-store rejection field to `storeCode`, and make mixed accepted/rejected imports return a partial-success status.

- [ ] **Step 3: Verify GREEN**

Run: `mvn -q -Dtest=ImportServiceImplReport5Test,ImportServiceReport5IntegrationTest test`
Expected: import Report 5 tests pass.

### Task 2: Fix Driver Execution State Contract

**Files:**
- Modify: `src/main/java/com/elog/service/impl/DriverTripServiceImpl.java`
- Test: `src/test/java/com/elog/integration/DriverTripServiceReport5IntegrationTest.java`

**Interfaces:**
- Consumes: `DriverTripService.startTrip(...)`.
- Produces: passing `L2-DRV-01`.

- [ ] **Step 1: Verify RED**

Run: `mvn -q -Dtest=DriverTripServiceReport5IntegrationTest test`
Expected: `L2-DRV-01` fails because `DISPATCHED` is rejected.

- [ ] **Step 2: Implement minimal state compatibility**

Allow `DISPATCHED` trip executions to start, while preserving existing `ASSIGNED` behavior.

- [ ] **Step 3: Verify GREEN**

Run: `mvn -q -Dtest=DriverTripServiceReport5IntegrationTest test`
Expected: driver integration test passes.

### Task 3: Decide Recommendation Side Effect

**Files:**
- Inspect: `src/main/java/com/elog/service/impl/RecommendationServiceImpl.java`
- Test: `src/test/java/com/elog/integration/RecommendationServiceReport5IntegrationTest.java`

**Interfaces:**
- Consumes: `RecommendationService.recommendTop3(...)`.
- Produces: either passing `L2-RCD-01..03` after removing non-contract side effects, or documented Fail if audit-write behavior is authoritative.

- [ ] **Step 1: Verify RED**

Run: `mvn -q -Dtest=RecommendationServiceReport5IntegrationTest test`
Expected: read-only assertions fail because recommendation records planning events.

- [ ] **Step 2: Trace production intent**

Read nearby service methods and migrations to determine whether recommendation audit logging is required by current source.

- [ ] **Step 3: Implement or document**

If catalog is authoritative, remove write side effects from recommendation only. If production audit is authoritative, keep Fail and document as spec mismatch.

### Task 4: Stabilize L3 Runner Fixtures

**Files:**
- Modify: `test-execution/scripts/run-l3-api.mjs`
- Results: `test-execution/results/l3.json`
- Evidence: `test-execution/evidence/l3-results.json`

**Interfaces:**
- Consumes: current backend HTTP API on `localhost:8080`.
- Produces: rerun evidence for all 130 L3 IDs.

- [ ] **Step 1: Reproduce current 7 L3 failures**

Run the L3 runner against a known backend instance and capture exact request/response details for the 7 failing IDs.

- [ ] **Step 2: Fix fixture/reference IDs**

Replace stale static IDs with IDs returned by bootstrap and avoid reusing mutated draft/trip state.

- [ ] **Step 3: Rerun full L3**

Run all 130 L3 IDs and update status from fresh evidence.

### Task 5: Technical UAT Rehearsal

**Files:**
- Create/modify: `test-execution/evidence/uat-technical-run.md`
- Modify: `test-execution/results/uat.json`
- Modify: `test-execution/REPORT5_TEST_STATUS.md`

**Interfaces:**
- Consumes: linked L4/E2E evidence.
- Produces: UAT technical disposition, without business sign-off.

- [ ] **Step 1: Map UAT to L4 evidence**

Review all 25 UAT IDs and their linked journeys.

- [ ] **Step 2: Mark technical outcome**

Use Pass only when the linked E2E has full technical evidence. Use Not Run when business sign-off or prerequisite journey evidence is absent.

- [ ] **Step 3: Update report**

Refresh totals and list remaining business/manual actions.
