# ELog Delivery Management System

Electronics logistics delivery management for Vietnam SME — single warehouse, fixed routes, store delivery.

---

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 21 |
| Maven | 3.8+ |
| Node.js | 18+ |
| MySQL | 8.x |

---

## Local Setup

```bash
# 1. Create database (Make sure MySQL is running on port 3307 or update application-dev.yml)
mysql -u root -p -P 3307 -e "CREATE DATABASE elog_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 2. Set env vars (or configure application-dev.yml)
export DB_PASSWORD=yourpassword
export JWT_SECRET=your-256-bit-secret

# 3. Run backend (Flyway migration runs automatically)
mvn spring-boot:run
```

*Note: Frontend is hosted in a separate repository (ELog-FE).*

Swagger UI: http://localhost:8080/swagger-ui/index.html  
Health check: `GET http://localhost:8080/api/v1/health`

---

## Git Workflow

### Branch naming

```
feature/US-{n}-{short-desc}     # new feature, e.g. feature/US-02-jwt-auth
bugfix/US-{n}-{short-desc}      # bug fix tied to a user story
hotfix/{short-desc}             # urgent production fix
chore/{short-desc}              # config, deps, CI, tooling
```

### Flow

```
main  ←── (PR + 1 review required)
  └── develop  ←── (PRs from feature/bugfix branches)
        └── feature/US-xx-...
```

- All feature work targets `develop`.
- `main` is protected: direct push is blocked; PR + 1 reviewer required.
- Never commit secrets, generated files, or `node_modules`.

---

## Project Structure

```
ELog-BE/ (Backend Repository)
├── src/              Spring Boot 3.x Source Code (Java 21, Maven)
├── docs/             Architecture & business-rule docs (load on demand)
├── pom.xml           Maven Project Object Model
└── .gitignore        Git ignore configurations
```

---

*SEP490_G104 · ELog · Sprint 1*
