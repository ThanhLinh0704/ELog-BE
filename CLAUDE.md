# ELog Delivery Management System — CLAUDE.md
> Auto-loaded every session. ≤ 300 lines. High-density only.
> For details: load docs/ files on demand (see §9).

---

## 1. System One-liner + 3 Key Constraints

**System:** Single warehouse → predefined fixed routes → store delivery (Electronics Logistics, Vietnam SME).

| # | Constraint | Rule |
|---|-----------|------|
| C-1 | LIFO mandatory | Last stop loaded first; first stop unloaded first (BR-04) |
| C-2 | Dual-capacity | Validate BOTH m³ AND kg — never skip one (BR-03) |
| C-3 | Excel-only input | No manual order UI; all orders via Excel upload (BR-01) |

---

## 2. Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Backend | Java / Spring Boot | 3.x |
| Frontend | ReactJS | 18.x |
| Database | MySQL | 8.x |
| Auth | JWT | — |
| API style | REST | JSON |
| Infra | AWS | — |
| Realtime | Firebase | — |
| Mobile (deferred) | Android App | — |

> Details & config patterns → `@docs/tech-stack.md`

---

## 3. Actors

| Type | Role |
|------|------|
| Internal | Dispatcher · Warehouse Staff · Driver · Logistics Manager · System Admin |
| External | GPS Platform · Map Service |

> Descriptions & UC assignments → `@docs/business-rules.md`

---

## 4. Trip Lifecycle

```
Planned → Validated → Dispatched → InProgress → Completed
```

**Invalid transitions (DC-01):** skip Validated · edit locked trip · reopen Completed.

---

## 5. Top 5 Most-Violated Business Rules

| ID | Rule — short form |
|----|-------------------|
| BR-01 | Orders ONLY via Excel. No manual order UI. |
| BR-02 | 1 order → 1 stop → 1 fixed route. Unmapped store = unassignable (error). |
| BR-03 | Capacity check = m³ AND kg both. Never one alone. |
| BR-04 | LIFO: last stop loaded first. No exceptions. |
| BR-07 | Oversized load → split into minimum trips each fitting a vehicle. |

> Full BR-01→BR-11 + edge cases → `@docs/business-rules.md`

---

## 6. Out-of-Scope (NEVER invent these features)

| ID | Excluded |
|----|----------|
| LI-01 | ERP integration |
| LI-02 | Accounting module |
| LI-03 | Unrestricted TSP/dynamic routing |
| LI-04 | Heavy WMS |
| LI-05 | 3D spatial packing |
| LI-06 | Manual order creation UI |
| LI-07 | Multi-warehouse |

---

## 7. Folder Structure (2 levels)

```
ELog-BE/ (Backend Repository)
├── CLAUDE.md                  ← this file (auto-load)
├── docs/                      ← on-demand context files
│   ├── tech-stack.md
│   ├── business-rules.md
│   ├── data-model.md
│   ├── api-conventions.md
│   └── us01-checklist.md
├── src/main/java/             ← Spring Boot source code
│   └── com/elog/
│       ├── config/
│       ├── controller/
│       ├── service/
│       ├── repository/
│       ├── entity/
│       └── dto/
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/          ← Flyway scripts
└── pom.xml                    ← Maven build config
```

---

## 8. API Base URL Convention

```
/api/{resource}
```

| Pattern | Example |
|---------|---------|
| Collection | `GET /api/routes` |
| Single item | `GET /api/routes/{id}` |
| Action | `POST /api/trips/{id}/dispatch` |

> Full conventions (auth header, envelope, errors, pagination) → `@docs/api-conventions.md`

---

## 9. Definition of Done (Universal Checklist)

- [ ] Unit tests written + passing (happy path + at least 1 edge case)
- [ ] API endpoint documented (Swagger / inline comment)
- [ ] All business rules validated server-side (not only frontend)
- [ ] Role-based access enforced on the endpoint
- [ ] No hardcoded secrets (use `application.yml` / env vars)
- [ ] PR merged to `develop` via reviewed PR (not direct push)
- [ ] Flyway migration script added if schema changes
- [ ] Feature verified against sprint acceptance criteria

---

## 10. On-Demand Loading Guide

Load these files only when needed — **do NOT load all at once**:

| Task type | Load |
|-----------|------|
| Schema design / DB work | `@docs/data-model.md` |
| Coding business logic | `@docs/business-rules.md` |
| Writing API endpoints | `@docs/api-conventions.md` |
| Setup / infra / config | `@docs/tech-stack.md` |
| Working on US-01 | `@docs/us01-checklist.md` |

---

## 11. Git Workflow Policy

- **NEVER run `git add .`, `git commit` or `git push` automatically.** 
- All modified and new files must remain unstaged/uncommitted in the working directory at the end of the session.
- Present the changes to the user and let them perform the commit and push manually.


---
*SEP490_G104 · Last updated: 2025 · Source of truth: ELog_Report3_SRS_v1.0.1_EN.docx*

