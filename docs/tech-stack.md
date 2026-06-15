# ELog — Tech Stack Details
> Load via `@docs/tech-stack.md` when doing setup / infra / config tasks.
> CLAUDE.md §2 has the summary table — this file adds depth.

---

## Backend — Spring Boot 3.x

### Project Structure Pattern
```
com.elog/
├── config/           # SecurityConfig, JwtConfig, SwaggerConfig
├── controller/       # REST controllers (thin — delegate to service)
├── service/          # Business logic layer
│   └── impl/         # Service implementations
├── repository/       # JPA repositories (extend JpaRepository)
├── entity/           # JPA @Entity classes
├── dto/              # Request / Response DTOs (no entity exposure)
│   ├── request/
│   └── response/
├── exception/        # GlobalExceptionHandler + custom exceptions
├── security/         # JwtFilter, UserDetailsServiceImpl
└── util/             # Helpers (e.g., ExcelParser, LIFOUtil)
```

### Key Dependencies (pom.xml)
```xml
spring-boot-starter-web
spring-boot-starter-data-jpa
spring-boot-starter-security
spring-boot-starter-validation
mysql-connector-j
flyway-core
flyway-mysql
io.jsonwebtoken:jjwt-api:0.11.x
springdoc-openapi-starter-webmvc-ui   <!-- Swagger UI -->
apache-poi (ooxml)                     <!-- Excel parsing -->
lombok
```

### application.yml Key Sections
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3307/elog_db  # local port 3307 in application-dev.yml
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: validate          # Flyway manages schema — NEVER use create/update
    show-sql: false               # Set true only in dev profile
  flyway:
    enabled: true
    locations: classpath:db/migration

elog:
  jwt:
    secret: ${JWT_SECRET}         # from env var — never hardcode
    expiration-ms: 86400000       # 24 hours
```

### Flyway Migration Naming Convention
```
V{version}__{description}.sql
Example: V1__init_schema.sql
         V2__add_trip_table.sql
```

---

## Frontend — ReactJS 18.x
> **Note:** The Frontend application is managed in a separate repository (`ELog-FE`). The conventions below are for reference when working on that repository.

### Project Bootstrap
```bash
npx create-react-app frontend --template typescript
# OR (Vite, preferred for speed):
npm create vite@latest frontend -- --template react-ts
```

### Key Dependencies (package.json)
```json
"axios"           : "^1.x"       // HTTP client
"react-router-dom": "^6.x"       // routing
"@reduxjs/toolkit": "^2.x"       // state management
"react-redux"     : "^9.x"
"antd"            : "^5.x"       // UI component library
"xlsx"            : "^0.18.x"    // Excel preview (client-side, optional)
"dayjs"           : "^1.x"       // date formatting
```

### Folder Conventions
```
src/
├── api/            # axios instances + API call functions (1 file per domain)
├── components/     # Reusable UI components (no page logic)
├── pages/          # Route-level page components
├── hooks/          # Custom React hooks (useAuth, useTrip…)
├── store/          # Redux slices
├── types/          # TypeScript interfaces / enums
└── utils/          # Pure helper functions
```

### Axios Base Config
```ts
// src/api/axiosInstance.ts
const api = axios.create({ baseURL: '/api' });
api.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});
```

---

## Database — MySQL 8.x

- Character set: `utf8mb4`, Collation: `utf8mb4_unicode_ci`
- All tables use `id BIGINT PRIMARY KEY AUTO_INCREMENT`
- Timestamps: `created_at DATETIME DEFAULT CURRENT_TIMESTAMP`, `updated_at DATETIME ON UPDATE CURRENT_TIMESTAMP`
- Soft delete via `deleted_at DATETIME NULL` (where applicable)

> Entity relationships → `@docs/data-model.md`

---

## Authentication — JWT

```
POST /api/auth/login  → { accessToken, refreshToken, tokenType, expiresIn, userId, username, roles }

Header on subsequent requests:
Authorization: Bearer <token>

Role values: DISPATCHER · WAREHOUSE_STAFF · DRIVER · LOGISTICS_MANAGER · ADMIN
```

---

## Firebase (Deferred — INC-2+)
- Used for real-time GPS feed ingestion
- Driver app pushes location; dashboard subscribes
- Not in Sprint 1 scope — do not scaffold yet

---

## AWS (Deferred)
- S3 for e-POD image storage
- Not in Sprint 1 scope — do not scaffold yet

---

## Git Strategy

### Branches
```
main        ← production releases only
develop     ← integration branch (PR target)
feature/US-{n}-{short-desc}   ← individual feature work
hotfix/...  ← critical production fixes
```

### PR Template (minimum)
```markdown
## What
Brief description of the change.

## Why
Business reason / US reference.

## Checklist
- [ ] Tests pass
- [ ] DoD met (see CLAUDE.md §9)
- [ ] No secrets committed
```

---

## CI Pipeline (GitHub Actions)
```yaml
# Minimum steps for Sprint 1
on: [push, pull_request]
jobs:
  build:
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '21' }
      - run: mvn test --no-transfer-progress
```
