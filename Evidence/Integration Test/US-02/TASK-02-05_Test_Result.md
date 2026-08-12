# TASK-02-05 — Authentication Testing Report

**Sprint:** Sprint 1
**Task:** TASK-02-05 Authentication Testing
**Tester:** NguyenGiap2804
**Date:** 2026-06-16
**Environment:** Local — `http://localhost:8080`
**Tool:** Postman (Integration) + JUnit 5 / Mockito + Spring MockMvc (Unit)

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
| `driver01` | `Dev@2025` | `DRIVER` | true (restore sau TC-03) |

---

## Kết quả tổng hợp

| # | Test Case | Layer | Unit (JUnit) | Integration (Postman) | Kết quả |
|---|-----------|-------|:---:|:---:|---------|
| TC-01 | Login đúng credentials | Unit + Integration | ✅ PASS | ✅ PASS | ✅ PASS |
| TC-02 | Login sai password | Unit + Integration | ✅ PASS | ✅ PASS | ✅ PASS |
| TC-03 | Login tài khoản bị khóa | Unit + Integration | ✅ PASS* | ❌ FAIL | ❌ FAIL |
| TC-04 | Token hợp lệ + đúng role | Unit + Integration | ✅ PASS | ✅ PASS | ✅ PASS |
| TC-05 | Không có token | Unit + Integration | ✅ PASS | ✅ PASS | ✅ PASS |
| TC-06 | Token hợp lệ + sai role | Unit + Integration | ✅ PASS | ✅ PASS | ✅ PASS |
| TC-07 | Refresh token còn hạn | Unit + Integration | ✅ PASS | ✅ PASS | ✅ PASS |
| TC-08 | Refresh sau logout | Unit + Integration | ✅ PASS | ✅ PASS | ✅ PASS |
| TC-09 | Token bị tamper | Unit + Integration | ✅ PASS | ✅ PASS | ✅ PASS |

**Tổng: 8/9 PASS — 1 FAIL**

**Unit test coverage:** 10 tests PASS / 10 tests — `AuthServiceImplTest` (5), `JwtUtilsTest` (2), `UserControllerSecurityTest` (3)

> *TC-03 Unit test PASS vì test xác nhận đúng behavior hiện tại (401) — nhưng behavior đó SAI so với spec (phải là 403). Bug được ghi nhận tại BUG-AUTH-01.

---

## Chi tiết từng Test Case

---

### TC-01 — Login đúng credentials

**Mục tiêu:** Đăng nhập với credentials hợp lệ → nhận `accessToken` + `refreshToken`

**Dữ liệu test:**
```
Method: POST
URL: http://localhost:8080/api/auth/login
Headers: Content-Type: application/json
Body:
{
  "username": "admin",
  "password": "Admin@2025"
}
```

**Các bước thực hiện:**
1. Mở Postman → tạo request `POST http://localhost:8080/api/auth/login`
2. Tab Body → raw → JSON
3. Nhập body như trên
4. Send

**Kết quả mong muốn:**
- HTTP `200`
- Response có `accessToken`, `refreshToken`, `roles`

**Kết quả thực tế:**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJyb2xlcyI6WyJTWVNURU1fQURNSU4iXSwidXNlcklkIjoxLCJ1c2VybmFtZSI6ImFkbWluIiwic3ViIjoiYWRtaW4iLCJpYXQiOjE3ODE1NDIyNDEsImV4cCI6MTc4MTYyODY0MX0.E81LV79Pbfc-wWgpYAn-afmrIlt-3J7ZvMPBxB2N32o",
    "refreshToken": "0f17c37e-5256-4f03-bd97-face119d7e97",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "userId": 1,
    "username": "admin",
    "roles": ["SYSTEM_ADMIN"]
  }
}
```

**Verdict: ✅ PASS**

---

### TC-02 — Login sai password

**Mục tiêu:** Đăng nhập sai password → trả `401 INVALID_CREDENTIALS`

**Dữ liệu test:**
```
Method: POST
URL: http://localhost:8080/api/auth/login
Headers: Content-Type: application/json
Body:
{
  "username": "admin",
  "password": "WrongPass123"
}
```

**Các bước thực hiện:**
1. Tạo request `POST http://localhost:8080/api/auth/login`
2. Nhập body với password sai
3. Send

**Kết quả mong muốn:**
- HTTP `401`
- `code: INVALID_CREDENTIALS`

**Kết quả thực tế:**
```json
{
  "error": {
    "code": "INVALID_CREDENTIALS",
    "message": "Invalid username or password"
  },
  "success": false
}
```

**Verdict: ✅ PASS**

---

### TC-03 — Login tài khoản bị khóa

**Mục tiêu:** Đăng nhập tài khoản `is_active = false` → trả lỗi phân biệt được với sai password

**Dữ liệu test:**
```
-- Setup: khóa driver01 trong DB
UPDATE users SET is_active = 0 WHERE username = 'driver01';

Method: POST
URL: http://localhost:8080/api/auth/login
Headers: Content-Type: application/json
Body:
{
  "username": "driver01",
  "password": "Dev@2025"
}
```

**Các bước thực hiện:**
1. Chạy SQL trong Workbench: `UPDATE users SET is_active = 0 WHERE username = 'driver01';`
2. Tạo request `POST http://localhost:8080/api/auth/login`
3. Nhập body với `driver01` / `Dev@2025`
4. Send
5. Sau test: restore `UPDATE users SET is_active = 1 WHERE username = 'driver01';`

**Kết quả mong muốn:**
- HTTP `403`
- `code` riêng biệt (VD: `ACCOUNT_DISABLED`) — khác với TC-02

**Kết quả thực tế:**
```json
{
  "error": {
    "code": "INVALID_CREDENTIALS",
    "message": "Invalid username or password"
  },
  "success": false
}
```
HTTP Status: `401`

**Verdict: ❌ FAIL**

**Mô tả lỗi:**
Backend trả về `401 INVALID_CREDENTIALS` giống hệt TC-02 (sai password). Không phân biệt được account bị khóa hay sai password. Frontend không thể hiển thị đúng message "Tài khoản đã bị vô hiệu hóa".

**Root cause:**
`UserDetailsServiceImpl.java` dùng query `findByUsernameAndIsActiveTrue` — nếu `is_active = false` thì user không tìm thấy, ném ra `INVALID_CREDENTIALS` thay vì `ACCOUNT_DISABLED`.

**File cần sửa:**
```
src/main/java/com/elog/security/UserDetailsServiceImpl.java
```

**Cách sửa:**
```java
// Hiện tại:
User user = userRepository.findByUsernameAndIsActiveTrue(username)
    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS, ...));

// Nên sửa thành:
User user = userRepository.findByUsername(username)
    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS, ...));

if (!user.getIsActive()) {
    throw new BusinessException(ErrorCode.ACCOUNT_DISABLED,
        "Account is disabled", HttpStatus.FORBIDDEN);
}
```

---

### TC-04 — Token hợp lệ + đúng role

**Mục tiêu:** `SYSTEM_ADMIN` gọi `GET /api/users` → `200`

**Unit test:** `UserControllerSecurityTest.TC04_adminRole_getUsers_returns200` — `@WithMockUser(roles = "SYSTEM_ADMIN")` + MockMvc → `status().isOk()`

**Dữ liệu test (Integration):**
```
Method: GET
URL: http://localhost:8080/api/users
Headers:
  Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJyb2xlcyI6WyJTWVNURU1fQURNSU4iXSwidXNlcklkIjoxLCJ1c2VybmFtZSI6ImFkbWluIiwic3ViIjoiYWRtaW4iLCJpYXQiOjE3ODE1NDIyNDEsImV4cCI6MTc4MTYyODY0MX0.E81LV79Pbfc-wWgpYAn-afmrIlt-3J7ZvMPBxB2N32o
```

**Các bước thực hiện:**
1. Lấy `accessToken` từ TC-01
2. Tạo request `GET http://localhost:8080/api/users`
3. Tab Authorization → Bearer Token → paste token
4. Send

**Kết quả mong muốn:**
- HTTP `200`
- Danh sách users

**Kết quả thực tế:**
- HTTP `200`, trả đủ 7 users với pagination

**Verdict: ✅ PASS**

---

### TC-05 — Gọi endpoint không có token

**Mục tiêu:** Không có Authorization header → `401`

**Unit test:** `UserControllerSecurityTest.TC05_noToken_returns401` — request không có auth → `status().isUnauthorized()`

**Dữ liệu test (Integration):**
```
Method: GET
URL: http://localhost:8080/api/users
Headers: (không có Authorization)
```

**Các bước thực hiện:**
1. Tạo request `GET http://localhost:8080/api/users`
2. Không thêm Authorization header
3. Send

**Kết quả mong muốn:**
- HTTP `401`

**Kết quả thực tế:**
```json
{
  "success": false,
  "error": {
    "code": "AUTHENTICATION_FAILED",
    "message": "Full authentication is required to access this resource"
  }
}
```

**Verdict: ✅ PASS**

---

### TC-06 — Token hợp lệ nhưng sai role

**Mục tiêu:** `DISPATCHER` gọi `GET /api/users` (chỉ dành cho `SYSTEM_ADMIN`) → `403`

**Unit test:** `UserControllerSecurityTest.TC06_driverRole_getUsers_returns403` — `@WithMockUser(roles = "DRIVER")` + MockMvc → `status().isForbidden()`

**Dữ liệu test (Integration):**
```
-- Bước 1: Lấy token dispatcher01
Method: POST
URL: http://localhost:8080/api/auth/login
Body: { "username": "dispatcher01", "password": "Dev@2025" }

-- Bước 2: Gọi endpoint
Method: GET
URL: http://localhost:8080/api/users
Headers:
  Authorization: Bearer <accessToken của dispatcher01>
```

**Các bước thực hiện:**
1. Login `dispatcher01` / `Dev@2025` → lấy `accessToken`
2. Tạo request `GET http://localhost:8080/api/users` với token dispatcher
3. Send

**Kết quả mong muốn:**
- HTTP `403`
- `code: ACCESS_DENIED`

**Kết quả thực tế:**
```json
{
  "error": {
    "code": "ACCESS_DENIED",
    "message": "You do not have permission to perform this action"
  },
  "success": false
}
```

**Verdict: ✅ PASS**

---

### TC-07 — Refresh token còn hạn

**Mục tiêu:** Dùng `refreshToken` hợp lệ → nhận `accessToken` mới

**Unit test:** `AuthServiceImplTest.TC07_refresh_validToken_returnsNewAccessToken` — mock `refreshTokenRepository.findByToken()` trả token chưa hết hạn → xác nhận `accessToken` mới và `expiresIn = 900`

**Dữ liệu test (Integration):**
```
Method: POST
URL: http://localhost:8080/api/auth/refresh
Headers: Content-Type: application/json
Body:
{
  "refreshToken": "0f17c37e-5256-4f03-bd97-face119d7e97"
}
```

**Các bước thực hiện:**
1. Lấy `refreshToken` từ TC-01
2. Tạo request `POST http://localhost:8080/api/auth/refresh`
3. Nhập body với refreshToken
4. Send

**Kết quả mong muốn:**
- HTTP `200`
- `accessToken` mới (khác TC-01)

**Kết quả thực tế:**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJyb2xlcyI6WyJTWVNURU1fQURNSU4iXSwidXNlcklkIjoxLCJ1c2VybmFtZSI6ImFkbWluIiwic3ViIjoiYWRtaW4iLCJpYXQiOjE3ODE1NDM0NTMsImV4cCI6MTc4MTYyOTg1M30.dV03Dzip-hEq6KuWv7e0lsYs_RWx-B9JLds6qvkidEU",
    "expiresIn": 900
  }
}
```

**Verdict: ✅ PASS**

---

### TC-08 — Refresh token sau khi logout

**Mục tiêu:** `refreshToken` đã dùng để logout → không thể dùng lại

**Unit test:** `AuthServiceImplTest.TC08_refresh_tokenNotInDB_throws401` — mock `findByToken()` trả `Optional.empty()` → `BusinessException(TOKEN_INVALID, 401)`

**Dữ liệu test (Integration):**
```
-- Bước 1: Logout
Method: POST
URL: http://localhost:8080/api/auth/logout
Body: { "refreshToken": "0f17c37e-5256-4f03-bd97-face119d7e97" }

-- Bước 2: Dùng lại refreshToken đó
Method: POST
URL: http://localhost:8080/api/auth/refresh
Body: { "refreshToken": "0f17c37e-5256-4f03-bd97-face119d7e97" }
```

**Các bước thực hiện:**
1. Gọi `POST /api/auth/logout` với refreshToken từ TC-01
2. Gọi `POST /api/auth/refresh` với cùng refreshToken đó
3. Send bước 2

**Kết quả mong muốn:**
- HTTP `401`
- Token không còn hiệu lực

**Kết quả thực tế:**
```json
{
  "error": {
    "code": "TOKEN_INVALID",
    "message": "Refresh token is not in database!"
  },
  "success": false
}
```

**Verdict: ✅ PASS**

---

### TC-09 — Token bị tamper

**Mục tiêu:** Token bị chỉnh sửa signature → `401`

**Unit test:** `JwtUtilsTest.TC09_tamperedSignature_validateToken_returnsFalse` — generate token hợp lệ, thay phần signature bằng bytes sai → `validateToken()` trả `false`

> **Bug fix phát hiện trong quá trình viết unit test:** `JwtUtils.validateToken()` không có `catch (SignatureException e)` → khi signature bị giả mạo sẽ throw exception thay vì trả `false`. Đã fix tại `src/main/java/com/elog/security/JwtUtils.java` — thêm `catch (SignatureException e)` trước `MalformedJwtException`.

**Dữ liệu test (Integration):**
```
Method: GET
URL: http://localhost:8080/api/users
Headers:
  Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJyb2xlcyI6WyJTWVNURU1fQURNSU4iXSwidXNlcklkIjoxLCJ1c2VybmFtZSI6ImFkbWluIiwic3ViIjoiYWRtaW4iLCJpYXQiOjE3ODE1NDM0NTMsImV4cCI6MTc4MTYyOTg1M30.dV03Dzip-hEq6KuWv7e0lsYs_RWx-B9JLds6qvkidUUUUU
```
*(Token hợp lệ từ TC-07 bị sửa phần signature cuối)*

**Các bước thực hiện:**
1. Lấy `accessToken` từ TC-07
2. Sửa vài ký tự cuối của signature (phần sau dấu `.` cuối cùng)
3. Tạo request `GET http://localhost:8080/api/users` với token đã tamper
4. Send

**Kết quả mong muốn:**
- HTTP `401`

**Kết quả thực tế:**
```json
{
  "success": false,
  "error": {
    "code": "AUTHENTICATION_FAILED",
    "message": "Full authentication is required to access this resource"
  }
}
```

**Verdict: ✅ PASS**

---

## Bug Report

### BUG-AUTH-01 — Account bị khóa trả về lỗi giống sai password

| Field | Value |
|-------|-------|
| **TC liên quan** | TC-03 |
| **Severity** | Medium |
| **Priority** | High |
| **File lỗi** | `src/main/java/com/elog/security/UserDetailsServiceImpl.java` |

**Mô tả:**
Khi login với tài khoản có `is_active = false`, backend trả về `401 INVALID_CREDENTIALS` — giống hệt trường hợp sai password. Frontend không thể phân biệt hai trường hợp để hiển thị đúng thông báo lỗi cho user.

**Expected:** `403` + `code: ACCOUNT_DISABLED` + message riêng biệt

**Actual:** `401` + `code: INVALID_CREDENTIALS` + message giống TC-02

**Cách sửa:**

File: `src/main/java/com/elog/security/UserDetailsServiceImpl.java`

```java
// Tách 2 bước: tìm user trước, check is_active sau
User user = userRepository.findByUsername(username)
    .orElseThrow(() -> new BusinessException(
        ErrorCode.INVALID_CREDENTIALS,
        "Invalid username or password",
        HttpStatus.UNAUTHORIZED));

if (!user.getIsActive()) {
    throw new BusinessException(
        ErrorCode.ACCOUNT_DISABLED,
        "Account has been disabled. Please contact administrator.",
        HttpStatus.FORBIDDEN);
}
```

Đồng thời cần thêm `ACCOUNT_DISABLED` vào `ErrorCode.java`:
```java
ACCOUNT_DISABLED("ACCOUNT_DISABLED"),
```

---

### BUG-AUTH-02 — `JwtUtils.validateToken()` không catch `SignatureException` ✅ FIXED

| Field | Value |
|-------|-------|
| **TC liên quan** | TC-09 |
| **Severity** | High |
| **Priority** | High |
| **Status** | ✅ Fixed (phát hiện khi viết unit test) |
| **File lỗi** | `src/main/java/com/elog/security/JwtUtils.java` |

**Mô tả:**
`validateToken()` có `catch (MalformedJwtException e)` nhưng thiếu `catch (SignatureException e)`. Khi token bị giả mạo chữ ký (valid base64url nhưng sai bytes), JJWT ném `io.jsonwebtoken.SignatureException` — exception này không được bắt, thoát ra ngoài filter và có thể gây lỗi 500 thay vì 401.

**Root cause:**
JJWT 0.11.5: `SignatureException` không extends `MalformedJwtException`, cần catch riêng.

**Fix đã áp dụng:**
```java
// Thêm vào đầu khối catch trong validateToken():
} catch (SignatureException e) {
    log.warn("Invalid JWT signature: {}", e.getMessage());
}
```

File: `src/main/java/com/elog/security/JwtUtils.java`
