# US-02 Authentication & Authorization — E2E Test Specification

> Tài liệu này mô tả đặc tả E2E test cho 9 test case của TASK-05 (US-02).
> Dùng để implement bằng Cypress hoặc Playwright trên frontend React.

---

## Thông tin API Backend

| Item | Giá trị |
|------|---------|
| Base URL | `http://localhost:8080` |
| Login endpoint | `POST /api/auth/login` |
| Refresh endpoint | `POST /api/auth/refresh` |
| Logout endpoint | `POST /api/auth/logout` |
| Protected endpoint (admin) | `GET /api/users` |

### Response envelope chuẩn

**Thành công:**
```json
{
  "success": true,
  "data": { ... }
}
```

**Thất bại:**
```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "Mô tả lỗi"
  }
}
```

---

## Tài khoản test cần chuẩn bị

| # | Username | Password | Role | Trạng thái | Ghi chú |
|---|----------|----------|------|------------|---------|
| 1 | `admin` | `Admin@2025` | SYSTEM_ADMIN | active | Đã có sẵn (DataInitializer) |
| 2 | `driver01` | `Driver@2025` | DRIVER | active | Tạo thủ công trước khi test |
| 3 | `locked_user` | `Locked@2025` | DISPATCHER | **inactive** | Tạo thủ công, set is_active = false |

> **Tạo tài khoản test:** Đăng nhập bằng admin → vào trang quản lý user → tạo `driver01` và `locked_user`.
> Hoặc insert thẳng vào DB (xem script SQL ở cuối file).

---

## Token Storage Convention

Frontend cần lưu token theo quy ước sau (để test có thể kiểm tra):

| Key | Giá trị |
|-----|---------|
| `localStorage.getItem('accessToken')` | Bearer token (JWT string) |
| `localStorage.getItem('refreshToken')` | Refresh token (UUID string) |

---

## Test Cases

---

### TC-01: Login đúng credentials → token hợp lệ

**Mục đích:** Kiểm tra luồng login happy path.

**Pre-condition:** Tài khoản `admin / Admin@2025` tồn tại và đang active.

**Steps:**
1. Mở trang `/login`
2. Nhập `username = admin`
3. Nhập `password = Admin@2025`
4. Click nút "Đăng nhập"

**Expected:**
- HTTP response: `POST /api/auth/login` → `200 OK`
- Response body:
  ```json
  {
    "success": true,
    "data": {
      "accessToken": "<JWT string>",
      "refreshToken": "<UUID string>",
      "tokenType": "Bearer",
      "username": "admin",
      "roles": ["SYSTEM_ADMIN"]
    }
  }
  ```
- `localStorage.accessToken` được set (không rỗng)
- `localStorage.refreshToken` được set (không rỗng)
- Trang chuyển hướng đến `/dashboard` (hoặc trang chủ sau login)
- Không hiển thị thông báo lỗi

---

### TC-02: Login sai password → 401

**Mục đích:** Kiểm tra xử lý sai mật khẩu.

**Pre-condition:** Tài khoản `admin` tồn tại.

**Steps:**
1. Mở trang `/login`
2. Nhập `username = admin`
3. Nhập `password = SaiMatKhau123`
4. Click nút "Đăng nhập"

**Expected:**
- HTTP response: `POST /api/auth/login` → `401 Unauthorized`
- Response body:
  ```json
  {
    "success": false,
    "error": {
      "code": "INVALID_CREDENTIALS",
      "message": "Invalid username or password"
    }
  }
  ```
- Trang KHÔNG chuyển hướng, vẫn ở `/login`
- Hiển thị thông báo lỗi trên UI (ví dụ: "Tên đăng nhập hoặc mật khẩu không đúng")
- `localStorage.accessToken` KHÔNG được set

---

### TC-03: Login tài khoản bị khóa → 403

**Mục đích:** Kiểm tra phân biệt tài khoản bị khóa với sai mật khẩu.

**Pre-condition:** Tài khoản `locked_user / Locked@2025` tồn tại, `is_active = false`.

**Steps:**
1. Mở trang `/login`
2. Nhập `username = locked_user`
3. Nhập `password = Locked@2025`
4. Click nút "Đăng nhập"

**Expected:**
- HTTP response: `POST /api/auth/login` → `403 Forbidden`
- Response body:
  ```json
  {
    "success": false,
    "error": {
      "code": "ACCOUNT_DISABLED",
      "message": "Account has been disabled"
    }
  }
  ```
- Trang KHÔNG chuyển hướng
- Hiển thị thông báo lỗi **khác với TC-02** (ví dụ: "Tài khoản đã bị khóa. Vui lòng liên hệ admin.")
- `localStorage.accessToken` KHÔNG được set

---

### TC-04: Access protected page với token hợp lệ → thành công

**Mục đích:** Kiểm tra route guard cho phép vào trang khi đã đăng nhập.

**Pre-condition:** Đã đăng nhập thành công với `admin`.

**Steps:**
1. Đăng nhập với `admin / Admin@2025` (hoặc set `localStorage.accessToken` = token hợp lệ)
2. Điều hướng đến `/users` (trang quản lý user — chỉ SYSTEM_ADMIN xem được)

**Expected:**
- HTTP request: `GET /api/users` với header `Authorization: Bearer <token>` → `200 OK`
- Trang `/users` hiển thị danh sách user
- Không bị redirect về `/login`
- Không hiển thị thông báo lỗi

---

### TC-05: Access protected page không có token → 401, redirect login

**Mục đích:** Kiểm tra route guard chặn truy cập khi chưa đăng nhập.

**Pre-condition:** Không có token trong localStorage (chưa đăng nhập hoặc đã xóa).

**Steps:**
1. Xóa `localStorage.accessToken` và `localStorage.refreshToken` (hoặc mở tab ẩn danh)
2. Điều hướng trực tiếp đến `/users`

**Expected:**
- HTTP request: `GET /api/users` → `401 Unauthorized` (hoặc frontend redirect TRƯỚC khi gọi API)
- Trang tự động redirect về `/login`
- Không hiển thị nội dung của `/users`

---

### TC-06: Access protected page với role không đủ quyền → 403

**Mục đích:** Kiểm tra RBAC — DRIVER không được xem trang quản lý user.

**Pre-condition:** Đã đăng nhập với `driver01 / Driver@2025` (role = DRIVER).

**Steps:**
1. Đăng nhập với `driver01 / Driver@2025`
2. Điều hướng đến `/users`

**Expected:**
- HTTP request: `GET /api/users` với DRIVER token → `403 Forbidden`
- Response body:
  ```json
  {
    "success": false,
    "error": {
      "code": "ACCESS_DENIED",
      "message": "You do not have permission to access this resource."
    }
  }
  ```
- Frontend hiển thị trang "403 Không có quyền" hoặc redirect về `/dashboard`
- Không hiển thị danh sách user

---

### TC-07: Refresh token hợp lệ → access token mới

**Mục đích:** Kiểm tra cơ chế tự động làm mới token.

**Pre-condition:** Đã đăng nhập, có `refreshToken` trong localStorage.

**Steps:**
1. Đăng nhập với `admin / Admin@2025`
2. Lấy `refreshToken` từ `localStorage`
3. Gọi `POST /api/auth/refresh` với body `{ "refreshToken": "<token>" }`

**Expected:**
- HTTP response: `200 OK`
- Response body:
  ```json
  {
    "success": true,
    "data": {
      "accessToken": "<JWT string mới>",
      "expiresIn": 900
    }
  }
  ```
- `accessToken` mới khác với `accessToken` cũ
- Frontend tự cập nhật `localStorage.accessToken` với token mới

> **Ghi chú triển khai:** Frontend nên dùng axios interceptor để tự gọi refresh khi nhận 401, thay vì test thủ công bước này.

---

### TC-08: Refresh token đã logout / không hợp lệ → 401

**Mục đích:** Kiểm tra refresh token bị thu hồi sau khi logout.

**Pre-condition:** Đã đăng nhập rồi logout, hoặc dùng refresh token giả.

**Steps:**
1. Đăng nhập với `admin / Admin@2025`, lưu lại `refreshToken`
2. Logout (`POST /api/auth/logout`)
3. Gọi lại `POST /api/auth/refresh` với `refreshToken` vừa lưu

**Expected:**
- HTTP response: `401 Unauthorized`
- Response body:
  ```json
  {
    "success": false,
    "error": {
      "code": "TOKEN_INVALID",
      "message": "Refresh token is not in database!"
    }
  }
  ```
- Frontend xóa token khỏi localStorage và redirect về `/login`

---

### TC-09: Access token bị tamper (sửa payload) → 401

**Mục đích:** Kiểm tra backend từ chối token bị giả mạo.

**Pre-condition:** Có access token hợp lệ.

**Steps:**
1. Đăng nhập với `admin / Admin@2025`, lấy `accessToken`
2. Tách token: `parts = accessToken.split('.')`
3. Thay `parts[1]` (payload) bằng một chuỗi Base64 khác (ví dụ: `parts[1] + "tampered"`)
4. Ghép lại: `tamperedToken = parts[0] + '.' + parts[1] + 'tampered' + '.' + parts[2]`
5. Set `localStorage.accessToken = tamperedToken`
6. Điều hướng đến `/users`

**Expected:**
- HTTP request: `GET /api/users` với tampered token → `401 Unauthorized`
- Response body:
  ```json
  {
    "success": false,
    "error": {
      "code": "AUTHENTICATION_FAILED",
      "message": "..."
    }
  }
  ```
- Frontend xóa token khỏi localStorage và redirect về `/login`

---

## Script SQL — Tạo tài khoản test

Chạy script này trên MySQL để tạo sẵn tài khoản test (thay password hash bằng BCrypt của `Driver@2025` và `Locked@2025`):

```sql
-- Tạo role DRIVER nếu chưa có
INSERT IGNORE INTO roles (name) VALUES ('DRIVER');
INSERT IGNORE INTO roles (name) VALUES ('DISPATCHER');

-- driver01: active, role DRIVER
-- BCrypt hash của "Driver@2025" (strength 12)
INSERT INTO users (username, password_hash, full_name, email, is_active)
VALUES ('driver01',
        '$2a$12$BCryptHashCuaDriver2025ThayChuoiNayBangHashThucTe',
        'Driver Test', 'driver01@elog.vn', true);

-- Gán role
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'driver01' AND r.name = 'DRIVER';

-- locked_user: inactive, role DISPATCHER
-- BCrypt hash của "Locked@2025"
INSERT INTO users (username, password_hash, full_name, email, is_active)
VALUES ('locked_user',
        '$2a$12$BCryptHashCuaLocked2025ThayChuoiNayBangHashThucTe',
        'Locked User Test', 'locked@elog.vn', false);

-- Gán role
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'locked_user' AND r.name = 'DISPATCHER';
```

> **Lưu ý:** Thay `$2a$12$BCryptHash...` bằng hash thật. Dùng tool online hoặc chạy đoạn Java:
> ```java
> System.out.println(new BCryptPasswordEncoder(12).encode("Driver@2025"));
> ```

---

## Acceptance Criteria

| TC | Mô tả | Pass khi |
|----|-------|---------|
| TC-01 | Login đúng → token | Status 200, accessToken trong localStorage |
| TC-02 | Sai password → 401 | Status 401, code = INVALID_CREDENTIALS, vẫn ở /login |
| TC-03 | Tài khoản khóa → 403 | Status 403, code = ACCOUNT_DISABLED, message khác TC-02 |
| TC-04 | Token hợp lệ → vào được page | Status 200, hiển thị nội dung trang |
| TC-05 | Không có token → redirect | Redirect về /login, không thấy nội dung |
| TC-06 | Sai role → 403 | Status 403, code = ACCESS_DENIED, không thấy nội dung |
| TC-07 | Refresh hợp lệ → token mới | Status 200, accessToken mới ≠ cũ |
| TC-08 | Refresh đã logout → 401 | Status 401, code = TOKEN_INVALID, xóa localStorage |
| TC-09 | Token tampered → 401 | Status 401, redirect về /login |
