# US-02 — JWT Authentication
**Epic:** EP-02 Authentication & Authorization
**Sprint:** Sprint 1
**Goal:** Người dùng đăng nhập an toàn và hệ thống kiểm soát quyền truy cập theo vai trò (Role-Based).
**Roles liên quan:** Dispatcher · Warehouse Staff · Driver · Logistics Manager · System Admin

---

## TASK-02-01 | Design User / Role Schema

| Field | Value |
|---|---|
| **Type** | Backend · DB |
| **Story Points** | 2 |
| **Phụ thuộc** | US-01 (DB skeleton đã có) |
| **Assignee** | *(Backend dev)* |

### Mô tả
Thiết kế và tạo Flyway migration cho các bảng `users`, `roles`, `user_roles`. Schema phải hỗ trợ đủ 5 roles trong scope: `DISPATCHER`, `WAREHOUSE_STAFF`, `DRIVER`, `LOGISTICS_MANAGER`, `SYSTEM_ADMIN`.

### Chi tiết kỹ thuật

**Bảng `users`**
| Column | Type | Ghi chú |
|---|---|---|
| `id` | BIGINT PK | Auto increment |
| `username` | VARCHAR(50) | Unique, not null |
| `password_hash` | VARCHAR(255) | BCrypt, not null |
| `full_name` | VARCHAR(100) | Not null |
| `email` | VARCHAR(100) | Unique |
| `is_active` | BOOLEAN | Default true |
| `created_at` | DATETIME | Auto |
| `updated_at` | DATETIME | Auto |

**Bảng `roles`**
| Column | Type | Ghi chú |
|---|---|---|
| `id` | INT PK | |
| `name` | VARCHAR(50) | Enum string |
| `description` | VARCHAR(255) | |

**Bảng `user_roles`**
| Column | Type | Ghi chú |
|---|---|---|
| `user_id` | BIGINT FK | → users.id |
| `role_id` | INT FK | → roles.id |

- Seed data: 5 roles + ít nhất 1 tài khoản `SYSTEM_ADMIN` mặc định
- File migration: `V2__auth_schema.sql`

### Acceptance Criteria
- [ ] Migration chạy thành công, không lỗi
- [ ] 5 roles được seed đúng tên
- [ ] Cột `password_hash` lưu hash, không lưu plaintext
- [ ] Schema được BA / Tech Lead review và approve trước khi implement các task tiếp theo

---

## TASK-02-02 | Implement JWT Authentication (Backend)

| Field | Value |
|---|---|
| **Type** | Backend |
| **Story Points** | 5 |
| **Phụ thuộc** | TASK-02-01 |
| **Assignee** | *(Backend dev — senior)* |

### Mô tả
Implement luồng login và JWT token lifecycle trong Spring Boot. Bao gồm phát hành token, validate token, và refresh token.

### Chi tiết kỹ thuật

**Endpoints:**

| Method | Path | Mô tả |
|---|---|---|
| POST | `/api/auth/login` | Nhận `username/password` → trả `access_token` + `refresh_token` |
| POST | `/api/auth/refresh` | Nhận `refresh_token` → trả `access_token` mới |
| POST | `/api/auth/logout` | Invalidate `refresh_token` |

**Token config:**
- Thư viện: `io.jsonwebtoken (jjwt)`
- `access_token` TTL: **15 phút**
- `refresh_token` TTL: **7 ngày**
- Lưu `refresh_token` vào bảng `refresh_tokens` trong DB để hỗ trợ logout / invalidate

**JWT Payload:**
```json
{
  "userId": 1,
  "username": "dispatcher01",
  "roles": ["DISPATCHER"],
  "iat": 1700000000,
  "exp": 1700000900
}
```

**Spring Security:**
- Cấu hình `SecurityFilterChain`
- Whitelist public: `/api/auth/**`
- Tạo `JwtAuthenticationFilter` extends `OncePerRequestFilter`

### Acceptance Criteria
- [ ] Login đúng credentials → trả `access_token` và `refresh_token` hợp lệ
- [ ] Login sai credentials → HTTP 401 với message rõ ràng
- [ ] Token hết hạn → HTTP 401, body trả error code phân biệt được với sai credentials
- [ ] Refresh token hợp lệ → cấp `access_token` mới thành công
- [ ] Sau logout, `refresh_token` không thể dùng lại (→ 401)

---

## TASK-02-03 | Implement RBAC Authorization (Backend)

| Field | Value |
|---|---|
| **Type** | Backend |
| **Story Points** | 3 |
| **Phụ thuộc** | TASK-02-02 |
| **Assignee** | *(Backend dev)* |

### Mô tả
Cấu hình Spring Security để kiểm soát quyền truy cập endpoint theo role. Định nghĩa role-permission matrix cho toàn bộ scope INC-1.

### Chi tiết kỹ thuật

**Permission Matrix (INC-1 scope):**

| Endpoint | SYSTEM_ADMIN | DISPATCHER | LOGISTICS_MANAGER | WAREHOUSE_STAFF | DRIVER |
|---|:---:|:---:|:---:|:---:|:---:|
| `POST /api/auth/**` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `GET/POST/PUT/DELETE /api/users/**` | ✅ | ❌ | ❌ | ❌ | ❌ |
| `GET /api/routes/**` | ✅ | ✅ | ✅ | ❌ | ❌ |
| `GET /api/vehicles/**` | ✅ | ✅ | ✅ | ❌ | ❌ |
| `GET /api/stores/**` | ✅ | ✅ | ✅ | ✅ | ❌ |

- Dùng `@PreAuthorize("hasRole('...')")` annotation trên controller methods
- Trả `403 Forbidden` kèm error body chuẩn khi không đủ quyền:
```json
{
  "status": 403,
  "error": "FORBIDDEN",
  "message": "You do not have permission to access this resource."
}
```
- Tạo file `PERMISSION_MATRIX.md` trong repo làm tài liệu sống — cập nhật mỗi khi thêm endpoint mới

### Acceptance Criteria
- [ ] Đúng role → truy cập được endpoint, nhận response bình thường
- [ ] Sai role → HTTP 403, response body theo chuẩn, không lộ thông tin hệ thống
- [ ] `SYSTEM_ADMIN` có quyền cao nhất, truy cập được mọi endpoint
- [ ] `DRIVER` chỉ thấy tài nguyên của chính mình (chuẩn bị cho NFR Security — Driver sees own trips only)
- [ ] `PERMISSION_MATRIX.md` được commit lên repo

---

## TASK-02-04 | Create Login UI (Frontend)

| Field | Value |
|---|---|
| **Type** | Frontend |
| **Story Points** | 3 |
| **Phụ thuộc** | TASK-02-02 *(hoặc mock API)* |
| **Assignee** | *(Frontend dev)* |

### Mô tả
Tạo trang Login trên ReactJS, tích hợp với backend Auth API, quản lý token và session trong client.

### Chi tiết kỹ thuật

**Route:** `/login` — public, tự redirect về `/` nếu đã có token hợp lệ

**Form fields:**
- `username` (text input)
- `password` (password input, toggle show/hide)
- Nút **Đăng nhập**

**Token storage strategy** *(team thống nhất trước khi implement):*
- `access_token` → lưu in-memory (Zustand / React Context)
- `refresh_token` → `httpOnly cookie` (preferred) hoặc `localStorage`

**Axios interceptor:**
- Tự động đính `Authorization: Bearer <token>` vào mọi request
- Khi nhận HTTP 401 → tự động gọi `POST /api/auth/refresh` → retry request gốc 1 lần
- Nếu refresh thất bại → redirect về `/login`

**Redirect sau login theo role:**

| Role | Redirect đến |
|---|---|
| DISPATCHER | `/dashboard` |
| LOGISTICS_MANAGER | `/dashboard` |
| WAREHOUSE_STAFF | `/warehouse` |
| SYSTEM_ADMIN | `/admin` |
| DRIVER | `/` *(placeholder — Driver App deferred)* |

**Error messages:**
- Sai credentials → `"Tên đăng nhập hoặc mật khẩu không đúng."`
- Tài khoản bị khóa (`is_active = false`) → `"Tài khoản đã bị vô hiệu hóa. Vui lòng liên hệ quản trị viên."`
- Lỗi mạng → `"Không thể kết nối đến máy chủ. Vui lòng thử lại."`

### Acceptance Criteria
- [ ] Login thành công → redirect đúng trang theo role
- [ ] Login thất bại → hiển thị thông báo lỗi đúng, không crash trang
- [ ] Axios interceptor tự động refresh token — người dùng không bị đá ra giữa phiên
- [ ] Sau logout / refresh thất bại → redirect về `/login`, xóa token khỏi memory
- [ ] Giao diện responsive cơ bản (desktop-first, theo NFR Usability)

---

## TASK-02-05 | Authentication Testing

| Field | Value |
|---|---|
| **Type** | Testing |
| **Story Points** | 3 |
| **Phụ thuộc** | TASK-02-02, TASK-02-03, TASK-02-04 |
| **Assignee** | *(Tester / Dev tự test)* |

### Mô tả
Viết và thực thi test cases bao phủ các luồng happy path và edge case cho authentication & authorization. Bao gồm cả unit test (Service layer) và integration test (API layer).

### Test Cases

| # | Test Case | Layer | Expected Result |
|---|---|---|---|
| TC-01 | Login đúng username + password | Unit (Service) | Trả `access_token` + `refresh_token` hợp lệ |
| TC-02 | Login sai password | Unit | HTTP 401 |
| TC-03 | Login tài khoản `is_active = false` | Unit | HTTP 403 + message phù hợp |
| TC-04 | Access protected endpoint với token hợp lệ + đúng role | Integration | HTTP 200 |
| TC-05 | Access protected endpoint không có token | Integration | HTTP 401 |
| TC-06 | Access endpoint với token hợp lệ nhưng sai role | Integration | HTTP 403 |
| TC-07 | Gọi `/refresh` với `refresh_token` còn hạn | Integration | Trả `access_token` mới |
| TC-08 | Gọi `/refresh` với `refresh_token` hết hạn hoặc đã logout | Integration | HTTP 401 |
| TC-09 | Gọi endpoint với token bị tamper (chỉnh sửa payload) | Integration | HTTP 401 |

### Acceptance Criteria
- [ ] Tất cả 9 test case pass
- [ ] Coverage Service layer ≥ 80%
- [ ] Test report được commit vào repo (hoặc gắn vào Jira ticket)

---

## TASK-02-06 | API Documentation

| Field | Value |
|---|---|
| **Type** | Docs |
| **Story Points** | 1 |
| **Phụ thuộc** | TASK-02-02, TASK-02-03 |
| **Assignee** | *(Backend dev — ai viết TASK-02-02/03)* |

### Mô tả
Viết tài liệu API cho các endpoint Authentication sử dụng Swagger / OpenAPI (tích hợp `springdoc-openapi`). Đảm bảo đồng nghiệp không cần đọc code vẫn hiểu cách sử dụng.

### Chi tiết kỹ thuật

**Swagger UI:** accessible tại `/swagger-ui.html`

**Các endpoint cần annotate đầy đủ:**

| Endpoint | Annotation cần có |
|---|---|
| `POST /api/auth/login` | `@Operation`, `@ApiResponse` (200, 401, 403) |
| `POST /api/auth/refresh` | `@Operation`, `@ApiResponse` (200, 401) |
| `POST /api/auth/logout` | `@Operation`, `@ApiResponse` (200, 401) |

**Mỗi endpoint phải có:**
- Mô tả ngắn (summary + description)
- Request body schema với example
- Response schema cho từng HTTP status code
- Ghi chú security requirement (Bearer token required / public)

**Tài liệu bổ sung:**
- Cập nhật `PERMISSION_MATRIX.md` nếu có thay đổi so với TASK-02-03
- Ghi chú TTL của `access_token` và `refresh_token` trong docs

### Acceptance Criteria
- [ ] Swagger UI hiển thị đủ 3 endpoint với example request/response đúng
- [ ] Mỗi endpoint ghi rõ HTTP status codes và ý nghĩa
- [ ] `PERMISSION_MATRIX.md` đồng bộ với implementation thực tế
- [ ] Đồng nghiệp không biết code vẫn hiểu được cách gọi API từ Swagger

---

## Tóm tắt Sprint 1

| Task | Assignee | SP | Phụ thuộc | Thứ tự ưu tiên |
|---|---|---|---|---|
| TASK-02-01 | Backend dev | 2 | US-01 | 🔴 Cao — blocking |
| TASK-02-02 | Backend dev (senior) | 5 | 02-01 | 🔴 Cao — blocking |
| TASK-02-03 | Backend dev | 3 | 02-02 | 🟡 Trung bình |
| TASK-02-04 | Frontend dev | 3 | 02-02 *(hoặc mock)* | 🟡 Trung bình |
| TASK-02-05 | Tester / Dev | 3 | 02-02, 02-03, 02-04 | 🟢 Sau khi API stable |
| TASK-02-06 | Backend dev | 1 | 02-02, 02-03 | 🟢 Song song với 02-03 |
| **Tổng** | | **17 SP** | | |

> **Ghi chú triển khai:**
> TASK-02-01 và TASK-02-02 là blocking tasks — ưu tiên hoàn thành trong **nửa đầu Sprint 1**.
> Frontend (TASK-02-04) có thể bắt đầu song song từ ngày 3–4 nếu team thống nhất API contract trước và dùng mock API.
> CI pipeline (tạm hoãn từ US-01) cần được bật lại trước Sprint 2 để test pipeline chạy tự động.
