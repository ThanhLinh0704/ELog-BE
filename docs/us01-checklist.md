# ELog — US-01 Project Skeleton Checklist
> Load via `@docs/us01-checklist.md` ONLY when working on US-01.
> Sprint 1 scope only. Do NOT scaffold deferred components (Firebase, Android, AWS S3).

---

## US-01 Goal

> Developer needs Backend + Frontend + Database skeleton ready.

**Epic:** EP-01 Project Setup & Infrastructure  
**Sprint:** Sprint 1 (with US-02, US-03, US-04)  
**Primary deliverable:** Runnable skeleton — no business logic required.

---

## Task Breakdown

### T-01 Database: Initial Schema + Flyway Migration

- [ ] Create MySQL database `elog_db` (UTF8MB4)
- [ ] Write `V1__init_schema.sql` with core tables:
  - `roles`, `users` (auth skeleton)
  - `routes`, `route_stops`, `stores` (master data skeleton)
  - `vehicles` (capacity fields: m³ + kg)
  - `products` (dimension fields for volume calculation)
- [ ] Verify Flyway runs clean on fresh DB
- [ ] Seed reference data: 3 sample roles, 1 admin user (hashed password)

> Full schema field specs → `@docs/data-model.md`

---

### T-02 Backend: Spring Boot Project Structure

- [ ] Initialize project via Spring Initializr or `mvn archetype:generate`
  - Java 21, Spring Boot 3.x, Maven
  - Dependencies: Web, JPA, Security, Validation, Flyway, MySQL, Lombok
- [ ] Apply package structure:
  ```
  com.elog.{config, controller, service, service.impl,
            repository, entity, dto.request, dto.response,
            exception, security, util}
  ```
- [ ] Configure `application.yml` (dev profile):
  - Datasource → `elog_db` local
  - Flyway enabled, `ddl-auto: validate`
  - JWT secret from env var
- [ ] Add `GlobalExceptionHandler` (skeleton — catches Exception, returns error envelope)
- [ ] Add `SwaggerConfig` → Swagger UI accessible at `/swagger-ui/index.html`
- [ ] `GET /api/v1/health` → `{ "status": "UP" }` (no auth required)
- [ ] Verify: `mvn spring-boot:run` starts without errors

---

### T-03 Frontend: ReactJS Project Structure
> **Note:** These frontend tasks are performed in the separate Frontend repository (`ELog-FE`) and are not part of this backend codebase.

- [ ] Bootstrap project:
  ```bash
  npm create vite@latest frontend -- --template react-ts
  cd frontend && npm install
  ```
- [ ] Install core dependencies:
  ```bash
  npm install axios react-router-dom @reduxjs/toolkit react-redux antd dayjs
  ```
- [ ] Apply folder structure:
  ```
  src/{api, components, pages, hooks, store, types, utils}
  ```
- [ ] Configure Axios base instance with JWT interceptor (skeleton — no real token yet)
- [ ] Setup React Router with placeholder routes:
  - `/login` → LoginPage (empty)
  - `/dashboard` → DashboardPage (empty)
  - `*` → 404 page
- [ ] Add `.env.development` with `VITE_API_BASE_URL=http://localhost:8080`
- [ ] Verify: `npm run dev` starts, placeholder pages render without console errors

---

### T-04 Git Strategy Setup

- [ ] Create `develop` branch from `main`
- [ ] Add `.gitignore` (Java + Node + IDE files)
- [ ] Add `PULL_REQUEST_TEMPLATE.md` at `.github/`
- [ ] Branch naming convention documented in README:
  ```
  feature/US-{n}-{short-desc}
  hotfix/...
  ```
- [ ] Protect `main` branch: require PR + 1 review (GitHub branch rules)

---

<!-- ### T-05 CI Pipeline

- [ ] Create `.github/workflows/ci.yml` with:
  - Trigger: push + PR to `develop` and `main`
  - Backend job: `mvn test --no-transfer-progress`
  - Frontend job: `npm ci && npm test -- --watchAll=false`
- [ ] Verify pipeline runs green on `develop` branch

> Full CI config template → `@docs/tech-stack.md`

--- -->

### T-06 Environment Setup Guide

- [x] Create `README.md` at repo root with:
  - Prerequisites (Java 21, Node 18+, MySQL 8, Maven 3.8+)
  - Local setup steps (DB creation, env vars, run commands)
  - Project structure overview
  - Git workflow summary
  - Link to Swagger UI

---

## US-01 Definition of Done

- [ ] `mvn spring-boot:run` → backend starts, Flyway migration clean
- [ ] *[Moved to ELog-FE]* `npm run dev` → frontend starts, routing works, no console errors
- [ ] `GET /api/v1/health` returns `{ "status": "UP" }` with HTTP 200
- [ ] Flyway baseline migration script committed (`V1__init_schema.sql`)
- [ ] GitHub Actions CI pipeline passes on `develop` (Backend build & tests)
- [ ] PR template in place (`.github/PULL_REQUEST_TEMPLATE.md`)
- [ ] `README.md` with setup guide committed
- [ ] No hardcoded secrets in any committed file

---

## Sprint 1 Scope Reminder

| US | What it adds on top of US-01 |
|----|------------------------------|
| US-02 | JWT auth implementation + login UI |
| US-03 | User CRUD + RBAC |
| US-04 | Route/RouteStop CRUD + master data UI |

**Do NOT implement** US-02/03/04 logic during US-01. Skeleton only.

---

## File Naming Conventions (US-01 scope)

### Java
```
Entity:      Route.java, Vehicle.java, User.java
Repository:  RouteRepository.java
Service:     RouteService.java (interface), RouteServiceImpl.java
Controller:  HealthController.java
DTO:         (skeleton empty classes OK for US-01)
```

### SQL
```
V1__init_schema.sql     ← all tables in one file for INC-1 baseline
```

### React
```
pages/LoginPage.tsx
pages/DashboardPage.tsx
api/axiosInstance.ts
App.tsx                 ← router setup
```

---

## Known Issues / Decisions for US-01

| # | Decision | Rationale |
|---|---------|-----------|
| D-01 | Flyway over Hibernate DDL | Reproducible schema; prevents accidental data loss |
| D-02 | `ddl-auto: validate` in all profiles | Forces schema-code sync; catches drift early |
| D-03 | Vite over CRA | Faster HMR; CRA is no longer maintained |
| D-04 | Skip Firebase scaffold | Deferred to INC-2+; not needed for Sprint 1 |
