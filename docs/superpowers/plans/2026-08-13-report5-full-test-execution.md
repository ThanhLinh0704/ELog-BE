# Report 5 Full Test Execution Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Execute the complete 425-case Report 5 catalog with auditable Pass, Fail or Not Run evidence without modifying production behavior.

**Architecture:** Each test level produces machine-readable results keyed by catalog Test ID. A final reconciler validates unique coverage of all 425 IDs and generates the human status matrix and defect report.

**Tech Stack:** Java 21, Spring Boot 3, JUnit 5, Mockito, Maven, MySQL/Flyway, Node.js 24, Cypress 15, React/Vite, Flutter/Dart where available, PowerShell orchestration.

## Global Constraints

- Work only on `test/2026-08-02v-reports-final` in all three repositories.
- Do not modify production code to make a failing test pass.
- Do not count placeholder or source-only checks as Pass.
- Every result must include an evidence locator.
- Final reconciliation must contain exactly 145 L1, 75 L2, 130 L3, 50 L4 and 25 UAT IDs.

---

### Task 1: Baseline and result ledger

**Files:**
- Create: `test-execution/catalog/`
- Create: `test-execution/results/`
- Create: `test-execution/scripts/validate-results.ps1`

- [ ] Copy the authoritative catalogs without altering IDs or expectations.
- [ ] Write a validator that rejects missing, extra and duplicate IDs and Pass entries without evidence.
- [ ] Run the validator against an empty ledger and verify it fails because 425 IDs are missing.

### Task 2: L1 unit execution

**Files:**
- Modify/Create: `src/test/java/com/elog/service/*Test.java`
- Create: `test-execution/results/l1.json`

- [ ] Map existing executable unit tests to catalog IDs.
- [ ] Add behavior-focused unit tests for uncovered L1 cases.
- [ ] Run Maven unit tests and export per-ID results plus Surefire/Jacoco evidence.

### Task 3: L2 integration execution

**Files:**
- Modify/Create: `src/test/java/com/elog/integration/*IntegrationTest.java`
- Create: `test-execution/results/l2.json`

- [ ] Verify the MySQL/Flyway test database and deterministic cleanup.
- [ ] Add integration scenarios for each uncovered transaction/repository boundary.
- [ ] Run integration tests and export committed/rolled-back database evidence by ID.

### Task 4: L3 System/API execution

**Files:**
- Modify/Create: `src/test/nodejs/l3_system_tests.js`
- Create: `test-execution/results/l3.json`

- [ ] Start the backend and discover the live contract.
- [ ] Execute all 118 primary mappings and 12 risk variants using real HTTP requests.
- [ ] Record status/body/auth results and create defects for mismatches.

### Task 5: L4 E2E execution

**Files:**
- Modify/Create: frontend Cypress specs under `src/Test/e2e/`
- Modify/Create: mobile tests under `integration_test/` and `test/`
- Create: `test-execution/results/l4.json`

- [ ] Remove placeholder suites from the executable test set.
- [ ] Start backend and frontend with controlled test accounts/data.
- [ ] Execute all web browser journeys with screenshots/videos/logs.
- [ ] Execute mobile journeys when the Flutter toolchain/device is available; otherwise record exact Not Run reasons.

### Task 6: UAT technical dry-run

**Files:**
- Create: `test-execution/results/uat.json`

- [ ] Execute machine-verifiable acceptance outcomes using L3/L4 evidence.
- [ ] Mark stakeholder-only decisions/sign-off Not Run with the required role named.
- [ ] Never self-approve Product Owner sign-off.

### Task 7: Reconciliation and handoff report

**Files:**
- Create: `test-execution/REPORT5_TEST_STATUS.md`
- Create: `test-execution/BUG_REPORT.md`
- Create: `test-execution/results/all-results.json`

- [ ] Validate exactly 425 unique result rows.
- [ ] Generate totals by level/status/priority and a row for every Test ID.
- [ ] Include reproducible defects with logs, expected/actual and ownership hints.
- [ ] Run all repository verification commands, commit and push each repository branch.
