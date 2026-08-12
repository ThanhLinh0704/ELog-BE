# IT-US03 — User Management Integration Test

**Sprint:** Sprint 1
**Tester:** NguyenGiap2804
**Date:** 2026-06-17
**Environment:** Local — `http://localhost:8080`
**Tool:** Postman
**Test Level:** L2 Integration Test
**Test Type:** API-driven Integration Test
**System Under Test:** UserController + UserService + UserRepository + RoleRepository + Spring Security + MySQL

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
| `driver01` | `Dev@2025` | `DRIVER` | true *(restore sau TC-09, TC-12)* |

**ID thực tế (lấy từ TC-01):**

| Username | ID | Email |
|----------|----|-------|
| `admin` | 1 | `admin@elog.vn` |
| `dispatcher01` | 2 | `dispatcher01@elog.vn` |
| `driver01` | 4 | `driver01@elog.vn` |

---

## Kết quả tổng hợp

| # | Test Case | Expected | Actual | Kết quả |
|---|-----------|----------|--------|---------|
| TC-01 | SYSTEM_ADMIN GET /api/users | 200 | 200 | ✅ PASS |
| TC-02 | DISPATCHER GET /api/users | 403 | 403 | ✅ PASS |
| TC-03 | Không có token GET /api/users | 401 | 401 | ✅ PASS |
| TC-04 | Tạo user mới hợp lệ | 201 | 201 | ✅ PASS |
| TC-05 | Tạo user username trùng | 409 | 409 | ✅ PASS |
| TC-06 | Tạo user email trùng | 409 | 409 | ✅ PASS |
| TC-07 | Tạo user password yếu | 400 | 400 | ✅ PASS |
| TC-08 | Tạo user role không hợp lệ | 400 | 400 | ✅ PASS |
| TC-09 | Khóa tài khoản user khác | 200 | 200 | ✅ PASS |
| TC-10 | Admin tự khóa tài khoản mình | 403 | 403 | ✅ PASS |
| TC-11 | Admin tự gỡ role SYSTEM_ADMIN | 403 | 403 | ✅ PASS |
| TC-12 | User bị khóa cố đăng nhập | 403 | 403 | ✅ PASS |

**Tổng: 12/12 PASS**

> **Xác nhận bổ sung (2026-06-18):** Chạy lại tự động bằng PowerShell (`Invoke-RestMethod`) — tất cả 12/12 PASS, nhất quán với kết quả Postman.
> TC-04 lần 2: user `it03_user` được tạo với id=10 (id=8 từ lần chạy trước đã tồn tại trong DB).
> Cleanup cần thiết: `DELETE FROM user_roles WHERE user_id IN (8,10); DELETE FROM users WHERE id IN (8,10);`

---

## Chi tiết từng Test Case

---

### TC-01 — SYSTEM_ADMIN gọi GET /api/users

**Mục tiêu:** Role SYSTEM_ADMIN có quyền xem danh sách users → HTTP 200

**Dữ liệu test:**
```
-- Bước 1: Lấy token admin
Method: POST
URL: http://localhost:8080/api/auth/login
Headers: Content-Type: application/json
Body:
{
  "username": "admin",
  "password": "Admin@2025"
}

-- Bước 2: Gọi API
Method: GET
URL: http://localhost:8080/api/users
Headers:
  Authorization: Bearer <accessToken từ bước 1>
```

**Các bước thực hiện:**
1. Login `admin` / `Admin@2025` → copy `data.accessToken`
2. Tạo request `GET http://localhost:8080/api/users`
3. Tab Authorization → Bearer Token → paste token
4. Send

**Kết quả mong muốn:**
- HTTP `200`
- `success: true`
- `data` là mảng users, `pagination` có `totalElements >= 3`

**Kết quả thực tế:**
```json
{
  "success": true,
  "data": [
    { "id": 1, "username": "admin", "fullName": "System Administrator", "email": "admin@elog.vn", "roles": ["SYSTEM_ADMIN"], "isActive": true },
    { "id": 2, "username": "dispatcher01", "fullName": "Nguyen Van Dispatcher", "email": "dispatcher01@elog.vn", "roles": ["DISPATCHER"], "isActive": true },
    { "id": 3, "username": "warehouse01", "roles": ["WAREHOUSE_STAFF"], "isActive": true },
    { "id": 4, "username": "driver01", "roles": ["DRIVER"], "isActive": false },
    { "id": 5, "username": "manager01", "roles": ["LOGISTICS_MANAGER"], "isActive": true },
    { "id": 6, "username": "testuser01", "roles": ["DISPATCHER"], "isActive": true },
    { "id": 7, "username": "testuserMeoMeo01", "roles": ["DRIVER"], "isActive": true }
  ],
  "pagination": { "page": 0, "size": 20, "totalElements": 7, "totalPages": 1 }
}
```

**Verdict: ✅ PASS**

---

### TC-02 — DISPATCHER gọi GET /api/users

**Mục tiêu:** Role DISPATCHER không có quyền xem danh sách users → HTTP 403

**Dữ liệu test:**
```
Method: POST  URL: http://localhost:8080/api/auth/login
Body: { "username": "dispatcher01", "password": "Dev@2025" }

Method: GET  URL: http://localhost:8080/api/users
Headers: Authorization: Bearer <accessToken dispatcher>
```

**Các bước thực hiện:**
1. Login `dispatcher01` / `Dev@2025` → copy `data.accessToken`
2. Tạo request `GET http://localhost:8080/api/users` với token dispatcher
3. Send

**Kết quả mong muốn:**
- HTTP `403` / `error.code: ACCESS_DENIED`

**Kết quả thực tế:**
```json
{
  "error": { "code": "ACCESS_DENIED", "message": "You do not have permission to perform this action" },
  "success": false
}
```

**Verdict: ✅ PASS**

---

### TC-03 — Không có token gọi GET /api/users

**Mục tiêu:** Request không có Authorization header → HTTP 401

**Dữ liệu test:**
```
Method: GET
URL: http://localhost:8080/api/users
Headers: (không có Authorization)
```

**Các bước thực hiện:**
1. Tạo request `GET http://localhost:8080/api/users` không có Authorization
2. Send

**Kết quả mong muốn:**
- HTTP `401` / `error.code: AUTHENTICATION_FAILED`

**Kết quả thực tế:**
```json
{
  "success": false,
  "error": { "code": "AUTHENTICATION_FAILED", "message": "Full authentication is required to access this resource" }
}
```

**Verdict: ✅ PASS**

---

### TC-04 — SYSTEM_ADMIN tạo user mới hợp lệ

**Mục tiêu:** Admin tạo user với đầy đủ thông tin hợp lệ → HTTP 201 + user object

**Dữ liệu test:**
```
Method: POST
URL: http://localhost:8080/api/users
Headers: Content-Type: application/json / Authorization: Bearer <token admin>
Body:
{
  "username": "testuser011",
  "password": "Test@2025",
  "fullName": "Test User 011",
  "email": "testuser06@elog.vn",
  "roles": ["DRIVER"]
}
```

**Các bước thực hiện:**
1. Lấy token admin
2. Tạo request `POST http://localhost:8080/api/users` với body trên
3. Send

**Kết quả mong muốn:**
- HTTP `201` / `data.isActive: true`

**Cleanup sau test:**
```sql
DELETE FROM user_roles WHERE user_id = 8;
DELETE FROM users WHERE id = 8;
```

**Kết quả thực tế:**
```json
{
  "success": true,
  "data": {
    "id": 8, "username": "testuser011", "fullName": "Test User 011",
    "email": "testuser06@elog.vn", "roles": ["DRIVER"], "isActive": true,
    "createdAt": "2026-06-17T23:51:05.824663"
  },
  "message": "User created successfully"
}
```

**Verdict: ✅ PASS**

---

### TC-05 — Tạo user với username đã tồn tại

**Mục tiêu:** Username `admin` đã tồn tại → HTTP 409

**Dữ liệu test:**
```
Method: POST  URL: http://localhost:8080/api/users
Body: { "username": "admin", "password": "Test@2025", "fullName": "Duplicate Admin", "email": "duplicate@elog.vn", "roles": ["DRIVER"] }
```

**Kết quả mong muốn:**
- HTTP `409` / `error.message` chứa "already exists"

**Kết quả thực tế:**
```json
{
  "error": { "code": "VALIDATION_FAILED", "message": "Username already exists" },
  "success": false
}
```

**Verdict: ✅ PASS**

---

### TC-06 — Tạo user với email đã tồn tại

**Mục tiêu:** Email `admin@elog.vn` đã thuộc tài khoản khác → HTTP 409

**Dữ liệu test:**
```
Method: POST  URL: http://localhost:8080/api/users
Body: { "username": "newuser99", "password": "Test@2025", "fullName": "New User 99", "email": "admin@elog.vn", "roles": ["DRIVER"] }
```

**Kết quả mong muốn:**
- HTTP `409` / `error.message` chứa "already exists"

**Kết quả thực tế:**
```json
{
  "error": { "code": "VALIDATION_FAILED", "message": "Email already exists" },
  "success": false
}
```

**Verdict: ✅ PASS**

---

### TC-07 — Tạo user với password yếu

**Mục tiêu:** Password `"12345"` không đáp ứng regex → HTTP 400

**Dữ liệu test:**
```
Method: POST  URL: http://localhost:8080/api/users
Body: { "username": "weakpassuser", "password": "12345", "fullName": "Weak Pass User", "email": "weakpass@elog.vn", "roles": ["DRIVER"] }
```

**Kết quả mong muốn:**
- HTTP `400` / `error.code: VALIDATION_FAILED` / `details` chứa lỗi field `password`

**Kết quả thực tế:**
```json
{
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "Validation failed",
    "details": ["password: Password must be at least 8 characters, containing at least 1 uppercase letter, 1 number, and 1 special character"]
  },
  "success": false
}
```

**Verdict: ✅ PASS**

---

### TC-08 — Tạo user với role không hợp lệ

**Mục tiêu:** Role `UNKNOWN_ROLE` không tồn tại trong DB → HTTP 400

**Dữ liệu test:**
```
Method: POST  URL: http://localhost:8080/api/users
Body: { "username": "invalidroleuser", "password": "Test@2025", "fullName": "Invalid Role User", "email": "invalidrole@elog.vn", "roles": ["UNKNOWN_ROLE"] }
```

**Kết quả mong muốn:**
- HTTP `400` / `error.message` chứa "Invalid role"

**Kết quả thực tế:**
```json
{
  "error": { "code": "VALIDATION_FAILED", "message": "Invalid role: UNKNOWN_ROLE" },
  "success": false
}
```

**Verdict: ✅ PASS**

---

### TC-09 — SYSTEM_ADMIN khóa tài khoản user khác

**Mục tiêu:** Admin khóa `driver01` (id=4) → HTTP 200, `isActive: false`

**Setup trước test:**
```sql
UPDATE users SET is_active = 1 WHERE username = 'driver01';
```

**Dữ liệu test:**
```
Method: PATCH
URL: http://localhost:8080/api/users/4/status
Headers: Content-Type: application/json / Authorization: Bearer <token admin>
Body: { "isActive": false }
```

**Kết quả mong muốn:**
- HTTP `200` / `data.isActive: false`

**Restore sau test:** Giữ nguyên để chạy TC-12, restore sau TC-12.

**Kết quả thực tế:**
```json
{
  "success": true,
  "data": { "id": 4, "username": "driver01", "fullName": "Le Van Driver", "email": "driver01@elog.vn", "roles": ["DRIVER"], "isActive": false },
  "message": "User status updated successfully"
}
```

**Verdict: ✅ PASS**

---

### TC-10 — SYSTEM_ADMIN tự khóa tài khoản của mình

**Mục tiêu:** Admin gửi `isActive: false` cho chính mình (id=1) → HTTP 403

**Dữ liệu test:**
```
Method: PATCH
URL: http://localhost:8080/api/users/1/status
Headers: Content-Type: application/json / Authorization: Bearer <token admin>
Body: { "isActive": false }
```

**Kết quả mong muốn:**
- HTTP `403` / `error.message` chứa "cannot lock their own account"

**Kết quả thực tế:**
```json
{
  "error": { "code": "ACCESS_DENIED", "message": "Admin cannot lock their own account" },
  "success": false
}
```

**Verdict: ✅ PASS**

---

### TC-11 — SYSTEM_ADMIN tự gỡ role SYSTEM_ADMIN của mình

**Mục tiêu:** Admin gửi `roles: ["DISPATCHER"]` cho chính mình (id=1) → HTTP 403

**Dữ liệu test:**
```
Method: PATCH
URL: http://localhost:8080/api/users/1/roles
Headers: Content-Type: application/json / Authorization: Bearer <token admin>
Body: { "roles": ["DISPATCHER"] }
```

**Kết quả mong muốn:**
- HTTP `403` / `error.message` chứa "cannot remove their own SYSTEM_ADMIN role"

**Kết quả thực tế:**
```json
{
  "error": { "code": "ACCESS_DENIED", "message": "Admin cannot remove their own SYSTEM_ADMIN role" },
  "success": false
}
```

**Verdict: ✅ PASS**

---

### TC-12 — User bị khóa cố đăng nhập

**Mục tiêu:** `driver01` đang bị khóa → HTTP 403, phân biệt được với sai password (401)

**Yêu cầu:** Chạy sau TC-09 (driver01 đang `isActive: false`)

**Dữ liệu test:**
```
Method: POST
URL: http://localhost:8080/api/auth/login
Headers: Content-Type: application/json
Body: { "username": "driver01", "password": "Dev@2025" }
```

**Kết quả mong muốn:**
- HTTP `403` *(không phải 401)* / `error.code: ACCOUNT_DISABLED`

**Restore sau test:**
```sql
UPDATE users SET is_active = 1 WHERE username = 'driver01';
```

**Kết quả thực tế:**
```json
{
  "error": { "code": "ACCOUNT_DISABLED", "message": "Account has been disabled" },
  "success": false
}
```

**Verdict: ✅ PASS**

---

## Bug Report

Không có bug. Tất cả 12/12 test case PASS.
