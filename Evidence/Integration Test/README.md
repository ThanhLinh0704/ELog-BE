# ELog L2 Integration Test Evidence

This folder stores **L2 Integration Test** evidence for ELog backend user stories.

## Classification

These tests are intentionally written as **API-driven Integration Tests**, not pure API contract tests.

| Term | Meaning in this project |
|---|---|
| API Testing | Verifies an HTTP endpoint contract: method, URL, status code, response body, headers. |
| Integration Testing | Verifies multiple backend layers working together: Controller, Security, Validation, Service, Repository, Flyway schema, and MySQL data. |
| API-driven Integration Test | Uses HTTP/Postman as the entry point, but the purpose is to verify backend integration across real layers and database state. |

## Required scope for each US

Each US folder should contain one canonical markdown file named:

```text
IT-USxx-{FeatureName}.md
```

The canonical file should cover:

- happy path API behavior;
- authentication and authorization;
- validation failures;
- business-rule failures;
- state/dependency-sensitive cases;
- setup, cleanup, and restore SQL where needed;
- clear separation between API-driven integration tests and UI/E2E tests.

## Current coverage map

| US | Canonical L2 Integration file | Status |
|---|---|---|
| US-02 | `US-02/IT-US02-Authentication.md` | Canonical file added; historical raw result remains in `TASK-02-05_Test_Result.md`. |
| US-03 | `US-03/IT-US03-UserManagement.md` | Existing file; already has completed Postman evidence. |
| US-04 | `US-04/IT-US04-StoreManagement.md` | Existing initial suite with screenshots; Actual/Verdict should be filled from final run evidence. |
| US-05 | `US-05/IT-US05-RouteManagement.md` | Existing initial suite; ready for Postman execution. |
| US-06 | `US-06/IT-US06-VehicleManagement.md` | Canonical initial suite added. |

## Cleanup rule

Do not reuse old created data blindly. Use deterministic `IT` prefixes and remove test data after execution:

- users: `it02_*`, `it03_*`;
- stores: `ST-IT04-*`, `ST-IT05-*`;
- routes: `RT-IT05-*`;
- vehicles: `VH-IT06-*` or plate numbers listed in the US-06 cleanup section.

When a test changes existing seed data, prefer `Restore sau test` over deletion.

