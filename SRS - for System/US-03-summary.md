# US-03 User Management — Tổng kết

**Branch:** `feature/US-03-user-management`  
**Sprint:** 1  
**Cập nhật:** 2026-06-15

---

## 1. Tổng quan tiến độ

| Task | Mô tả | Trạng thái |
|------|-------|-----------|
| TASK-01 | User CRUD API | ✅ Hoàn thành |
| TASK-02 | User Management UI | 🔲 Frontend — ngoài scope backend |
| TASK-03 | Authorization Testing | ⚠️ Chưa làm — đang có merge conflict cần resolve trước |

---

## 2. TASK-01 — Những gì đã làm

### 2.1 Database Schema (Flyway migrations)

| File | Nội dung |
|------|----------|
| `V1__init_schema.sql` | Tạo toàn bộ schema: `roles`, `users` (role_id đơn), `stores`, `routes`, `route_stops`, `vehicles`, `products`, `orders`, `trips`, v.v. |
| `V2__seed_reference_data.sql` | Seed 5 roles (`ADMIN`, `DISPATCHER`, `WAREHOUSE_STAFF`, `DRIVER`, `LOGISTICS_MANAGER`), 1 admin user, 3 stores mẫu, 2 routes, 2 vehicles, 3 products |
| `V3__update_user_roles_and_email.sql` | 4 thay đổi quan trọng: (1) thêm cột `email`, (2) tạo bảng `user_roles` (many-to-many), (3) migrate dữ liệu từ `role_id` sang `user_roles`, (4) đổi tên role `ADMIN` → `SYSTEM_ADMIN` |

### 2.2 Endpoints đã implement

Base path: `/api/v1/users`

| Method | Path | Mô tả | Role |
|--------|------|-------|------|
| GET | `/api/v1/users` | Danh sách có phân trang + filter | SYSTEM_ADMIN |
| GET | `/api/v1/users/{id}` | Chi tiết 1 user | SYSTEM_ADMIN |
| POST | `/api/v1/users` | Tạo user mới | SYSTEM_ADMIN |
| PUT | `/api/v1/users/{id}` | Cập nhật fullName, email | SYSTEM_ADMIN |
| PATCH | `/api/v1/users/{id}/roles` | Gán / gỡ role | SYSTEM_ADMIN |
| PATCH | `/api/v1/users/{id}/status` | Khoá / mở khoá tài khoản | SYSTEM_ADMIN |

### 2.3 Các file đã tạo

**Controller / Service / Repository:**
- `UserController.java` — 6 endpoints, `@PreAuthorize` ở class level **đang bị comment out** (cần bổ sung trong TASK-03)
- `UserService.java` (interface) + `UserServiceImpl.java`
- `UserMapper.java`
- `UserSpecification.java` — filter động theo keyword, role, isActive

**DTOs:**
- `UserCreateRequest.java` — validation: username (regex), password (regex mạnh), email, fullName, roles
- `UserUpdateRequest.java` — chỉ cho phép sửa fullName và email
- `UserRolesUpdateRequest.java`
- `UserStatusUpdateRequest.java`
- `UserResponse.java`

**Security:**
- `JwtAuthFilter.java` — filter đọc Bearer token, validate, set SecurityContext
- `JwtUtils.java` — generate/validate/parse JWT (jjwt 0.11.x API, đọc `${elog.jwt.secret}`)
- `UserDetailsServiceImpl.java` — load user bằng `findByUsernameAndIsActiveTrue` (tự động bỏ qua user bị khoá)

**Config / Exception:**
- `CorsConfig.java` — CORS từ `${elog.cors.allowed-origins}`
- `SwaggerConfig.java`
- `BusinessException.java` + `ErrorCode.java`
- `ApiResponse.java` — wrapper `{ success, data, message, pagination }`

### 2.4 Business rules đã implement trong service

| Rule | File | Ghi chú |
|------|------|---------|
| Password BCrypt trước khi lưu | `UserServiceImpl.createUser` | ✅ |
| Username unique | `UserServiceImpl.createUser` | ✅ → 409 |
| Email unique | `UserServiceImpl.createUser` | ✅ → 409 |
| Admin không tự khoá mình (TC-10) | `UserServiceImpl.updateUserStatus` | ✅ → 403 |
| Admin không gỡ role SYSTEM_ADMIN của mình (TC-11) | `UserServiceImpl.updateUserRoles` | ✅ → 403 |
| Role không hợp lệ → reject (TC-08) | `UserServiceImpl.createUser` | ✅ → 400 |

---

## 3. Merge Conflicts hiện tại (cần resolve trước khi build)

Các file đang ở trạng thái `AA` (cả 2 nhánh cùng thêm):

| File | Vấn đề chính | Nên dùng version nào |
|------|-------------|---------------------|
| `SecurityConfig.java` | Team để `/api/v1/users/**` là **public** (sai); version US-04 có RBAC đầy đủ | Merge: giữ RBAC của US-04, thêm `JwtAuthFilter` + `CorsConfig` của US-03 |
| `Role.java` | US-04 dùng enum `RoleName` + `Integer id`; US-03 dùng `String name` + `Long id` | Dùng US-03 (String, Long) — tương thích với V3 migration |
| `User.java` | US-04: email nullable; US-03: email NOT NULL | Dùng US-03 (NOT NULL) — đúng với V3 migration |
| `RoleRepository.java` | US-04 dùng `RoleName` enum; US-03 dùng `String` | Dùng US-03 |
| `UserRepository.java` | US-04: `findByUsername`; US-03: `findByUsernameAndIsActiveTrue` | Giữ cả hai method |
| `GlobalExceptionHandler.java` | US-04: format `ApiError`; US-03: format `Map<String,Object>` | Cần thống nhất — `ApiError` dễ test hơn |
| `HealthController.java` | Mapping path khác nhau (`/api/v1` vs `/api/v1/health`) | Dùng US-03 (`/api/v1/health`) |
| `application.yml` | US-04 thiếu `elog.jwt.*`, `elog.cors.*`, JPA, Flyway config | Dùng US-03 (đầy đủ hơn) |
| `application-dev.yml` | Khác nhau về MySQL URL, password, JWT properties | Dùng US-03 (chuẩn hơn) |
| `pom.xml` | Spring Boot version, jjwt version (0.11.5 vs 0.12.6), thiếu H2, Apache POI | Merge: lấy Spring Boot 3.3.5, jjwt 0.11.5 (tương thích JwtUtils), thêm H2 + POI |

**Lưu ý thêm:** Do US-03 và US-04 có 2 implementation JWT song song:
- US-04: `JwtAuthenticationFilter` + `JwtService` + `CustomUserDetailsService` + `UserPrincipal`
- US-03: `JwtAuthFilter` + `JwtUtils` + `UserDetailsServiceImpl`

Nếu giữ cả 2, sẽ có **duplicate `UserDetailsService` bean** → app không start được. Cần chọn 1 bộ (nên giữ US-03 vì đơn giản hơn và đã được wired đúng).

---

## 4. TASK-03 — Authorization Testing (chưa làm)

Cần implement 12 backend TC (TC-13 → TC-19 là frontend, không làm ở đây):

| TC | Test Case | Expected |
|----|-----------|----------|
| TC-01 | SYSTEM_ADMIN GET /api/v1/users | 200 |
| TC-02 | DISPATCHER GET /api/v1/users | 403 |
| TC-03 | Không có token GET /api/v1/users | 401 |
| TC-04 | SYSTEM_ADMIN tạo user hợp lệ | 201 |
| TC-05 | Tạo user với username trùng | 409 |
| TC-06 | Tạo user với email trùng | 409 |
| TC-07 | Tạo user với password yếu | 400 |
| TC-08 | Gán role không hợp lệ | 400 |
| TC-09 | Admin khoá user khác | 200 |
| TC-10 | Admin tự khoá mình | 403 |
| TC-11 | Admin tự gỡ role SYSTEM_ADMIN | 403 |
| TC-12 | User bị khoá cố đăng nhập | 403 *(cần TASK-02 auth endpoint)* |

**Prerequisite trước khi viết test:**
1. Resolve toàn bộ merge conflicts
2. Thêm `@PreAuthorize("hasRole('SYSTEM_ADMIN')")` ở class level `UserController`
3. Đảm bảo `application-test.yml` có `elog.jwt.secret` và `elog.jwt.expiration-ms`

---

## 5. Điều cần làm để hoàn thành US-03

```
1. Resolve merge conflicts (xem bảng §3)
2. Thêm @PreAuthorize vào UserController
3. Cập nhật application-test.yml
4. Viết UserControllerAuthorizationTest.java (TC-01 → TC-12)
5. Chạy mvn test để verify
6. Push + tạo PR
```

---

*SEP490_G104 · Sprint 1 · Cập nhật: 2026-06-15*
