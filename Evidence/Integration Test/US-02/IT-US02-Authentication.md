# IT-US02 — Authentication API-driven Integration Test

**Sprint:** Sprint 1  
**Tester:** NguyenGiap2804  
**Latest execution date:** 2026-06-28  
**Environment:** Local — `http://localhost:8080`  
**Tool:** Postman  
**Test Level:** L2 Integration Test  
**Test Type:** API-driven Integration Test  
**System Under Test:** ELog-BE AuthController/AuthService + Spring Security/JWT + RefreshTokenRepository + UserRepository + MySQL  
**Historical evidence:** `TASK-02-05_Test_Result.md` and TC screenshots in this folder.

> This file is the canonical L2 Integration Test index for US-02. It keeps the suite clearly classified as integration testing, while the older `TASK-02-05_Test_Result.md` remains the raw historical execution report.

---

## Môi trường & Điều kiện tiên quyết

| Item | Value |
|------|-------|
| Backend URL | `http://localhost:8080` |
| Database | MySQL `localhost:3307` / `elog_db` |
| Profile | `dev` |
| Java | 21.0.9 |
| Spring Boot | 3.3.5 |

**Tài khoản test:**

| Username | Password | Role | is_active |
|----------|----------|------|-----------|
| `admin` | `Admin@2025` | `SYSTEM_ADMIN` | true |
| `dispatcher01` | `Dev@2025` | `DISPATCHER` | true |
| `driver01` | `Dev@2025` | `DRIVER` | true *(may be temporarily disabled in TC-03)* |

---

## Kết quả tổng hợp

| # | Test Case | Integration boundary | Expected | Actual | Kết quả |
|---|-----------|----------------------|----------|--------|---------|
| TC-01 | Login đúng credentials | AuthController → AuthService → UserRepository → JWT → RefreshTokenRepository | 200 | 200; access/refresh token issued; SYSTEM_ADMIN returned | PASS |
| TC-02 | Login sai password | AuthController → Spring Security/AuthManager → exception handler | 401 | 401; INVALID_CREDENTIALS | PASS |
| TC-03 | Login tài khoản bị khóa | UserRepository active-state check → AuthService exception mapping | 403 | 403; ACCOUNT_DISABLED | PASS |
| TC-04 | Token hợp lệ + đúng role gọi protected API | JWT filter → Method Security → UserController | 200 | 200; user list and pagination returned | PASS |
| TC-05 | Không có token gọi protected API | Spring Security filter chain | 401 | 401; AUTHENTICATION_FAILED | PASS |
| TC-06 | Token hợp lệ nhưng sai role | JWT filter → Method Security | 403 | 403; ACCESS_DENIED | PASS |
| TC-07 | Refresh token còn hạn | AuthController → RefreshTokenRepository → JWT | 200 | 200; new access token; expiresIn=900 | PASS |
| TC-08 | Refresh sau logout | Logout removes refresh token → refresh cannot reuse token | 401 | logout 200; refresh 401 TOKEN_INVALID | PASS |
| TC-09 | Token bị tamper | JWT validation filter → error response | 401 | 401; AUTHENTICATION_FAILED | PASS |

**Latest result: 9/9 PASS.** TC-03 was rerun successfully after the account-disabled fix.

### Latest rerun notes — 2026-06-28

- The suite was executed directly against the running backend at `http://localhost:8080`.
- No access or refresh token value was written to the evidence log.
- `driver01` was disabled through the protected User API for TC-03 and restored to active immediately afterward.
- Admin and dispatcher refresh tokens created by the rerun were logged out during cleanup.
- Final cleanup state: `driver01.is_active=true`.

---

## Chi tiết từng Test Case

### TC-01 — Login đúng credentials

**Mục tiêu:** Verify login integrates credential validation, user lookup, JWT generation, refresh-token persistence, and response shaping.

**Dữ liệu test:**

```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json
```

```json
{ "username": "admin", "password": "Admin@2025" }
```

**Kết quả mong muốn:**
- HTTP `200`
- `success: true`
- `data.accessToken` is present
- `data.refreshToken` is present
- `data.roles` contains `SYSTEM_ADMIN`

**Cleanup sau test:** none.

### TC-02 — Login sai password

**Mục tiêu:** Verify invalid password is rejected by the authentication layer and mapped to the correct API error.

**Dữ liệu test:**

```json
{ "username": "admin", "password": "WrongPass123" }
```

**Kết quả mong muốn:**
- HTTP `401`
- `error.code: "INVALID_CREDENTIALS"`
- no token is issued

**Cleanup sau test:** none.

### TC-03 — Login tài khoản bị khóa

**Setup trước test:**

```sql
UPDATE users SET is_active = 0 WHERE username = 'driver01';
```

**Dữ liệu test:**

```json
{ "username": "driver01", "password": "Dev@2025" }
```

**Kết quả mong muốn:**
- HTTP `403`
- `error.code: "ACCOUNT_DISABLED"`
- response must be different from TC-02 wrong-password response

**Restore sau test:**

```sql
UPDATE users SET is_active = 1 WHERE username = 'driver01';
```

### TC-04 — Token hợp lệ + đúng role

**Mục tiêu:** Verify JWT filter and method security allow `SYSTEM_ADMIN` to call a protected admin API.

**Dữ liệu test:**

```http
GET http://localhost:8080/api/users
Authorization: Bearer <adminAccessToken>
```

**Kết quả mong muốn:**
- HTTP `200`
- response contains user list and pagination

### TC-05 — Không có token

**Dữ liệu test:**

```http
GET http://localhost:8080/api/users
```

**Kết quả mong muốn:**
- HTTP `401`
- `error.code: "AUTHENTICATION_FAILED"`

### TC-06 — Token hợp lệ nhưng sai role

**Dữ liệu test:**

```http
GET http://localhost:8080/api/users
Authorization: Bearer <dispatcherAccessToken>
```

**Kết quả mong muốn:**
- HTTP `403`
- `error.code: "ACCESS_DENIED"`

### TC-07 — Refresh token còn hạn

**Dữ liệu test:**

```http
POST http://localhost:8080/api/auth/refresh
Content-Type: application/json
```

```json
{ "refreshToken": "<refreshToken from TC-01>" }
```

**Kết quả mong muốn:**
- HTTP `200`
- new `accessToken` is present
- `expiresIn` is returned

### TC-08 — Refresh after logout

**Dữ liệu test:**

```http
POST http://localhost:8080/api/auth/logout
Content-Type: application/json
```

```json
{ "refreshToken": "<refreshToken from TC-01>" }
```

Then:

```http
POST http://localhost:8080/api/auth/refresh
Content-Type: application/json
```

```json
{ "refreshToken": "<sameRefreshToken>" }
```

**Kết quả mong muốn:**
- logout returns success
- reuse of logged-out refresh token returns HTTP `401`
- `error.code: "TOKEN_INVALID"`

### TC-09 — Token bị tamper

**Dữ liệu test:**

```http
GET http://localhost:8080/api/users
Authorization: Bearer <tamperedAccessToken>
```

**Kết quả mong muốn:**
- HTTP `401`
- `error.code: "AUTHENTICATION_FAILED"`
- no protected data is returned

---

## Cleanup / Restore checklist

```sql
UPDATE users SET is_active = 1 WHERE username = 'driver01';
DELETE FROM refresh_tokens WHERE user_id IN (SELECT id FROM users WHERE username IN ('admin', 'dispatcher01', 'driver01'));
```

Only run refresh-token cleanup if it is safe for the local dev database after finishing the test session.

---

## Bug Report

Historical `BUG-AUTH-01` is resolved. The latest rerun returned `403 ACCOUNT_DISABLED` for an inactive account. No open US-02 integration defect remains.
