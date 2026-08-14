# Permission Matrix — ELog Delivery Management System

> **Living document.** Cập nhật khi thêm endpoint mới hoặc thay đổi quyền.
> Áp dụng qua `@PreAuthorize` annotation trên controller methods.
> Nguồn gốc: INC-1 scope, SRS v1.0.1.

---

## Quy ước ký hiệu

| Ký hiệu | Ý nghĩa |
|---------|---------|
| ✅ | Được phép |
| ❌ | Bị từ chối (HTTP 403) |
| 🔓 | Public — không cần token |
| ⚠️ | Chỉ tài nguyên của chính mình (self-resource) |

> **SA** = SYSTEM_ADMIN · **DI** = DISPATCHER · **LM** = LOGISTICS_MANAGER · **WS** = WAREHOUSE_STAFF · **DR** = DRIVER

---

## 1. Auth (US-02)

| Method | Endpoint | SA | DI | LM | WS | DR | Ghi chú |
|--------|----------|----|----|----|----|----|---------|
| POST | `/api/v1/auth/login` | 🔓 | 🔓 | 🔓 | 🔓 | 🔓 | Không cần token |
| POST | `/api/v1/auth/refresh` | 🔓 | 🔓 | 🔓 | 🔓 | 🔓 | Refresh JWT |
| POST | `/api/v1/auth/logout` | ✅ | ✅ | ✅ | ✅ | ✅ | Cần token hợp lệ |

**Implementation:** `SecurityConfig.java` → `.requestMatchers("/api/v1/auth/**").permitAll()`

---

## 2. User Management (US-03)

| Method | Endpoint | SA | DI | LM | WS | DR | Ghi chú |
|--------|----------|----|----|----|----|----|---------|
| GET | `/api/v1/users` | ✅ | ❌ | ❌ | ❌ | ❌ | Danh sách user (phân trang + filter) |
| GET | `/api/v1/users/{id}` | ✅ | ❌ | ❌ | ❌ | ❌ | Chi tiết user |
| POST | `/api/v1/users` | ✅ | ❌ | ❌ | ❌ | ❌ | Tạo user mới |
| PUT | `/api/v1/users/{id}` | ✅ | ❌ | ❌ | ❌ | ❌ | Cập nhật thông tin user |
| PATCH | `/api/v1/users/{id}/roles` | ✅ | ❌ | ❌ | ❌ | ❌ | Gán/thu hồi role |
| PATCH | `/api/v1/users/{id}/status` | ✅ | ❌ | ❌ | ❌ | ❌ | Khóa/mở tài khoản |

**Implementation:** `@PreAuthorize("hasRole('SYSTEM_ADMIN')")` ở class level `UserController`

---

## 3. Store Management (US-04)

| Method | Endpoint | SA | DI | LM | WS | DR | Ghi chú |
|--------|----------|----|----|----|----|----|---------|
| GET | `/api/v1/stores` | ✅ | ✅ | ✅ | ✅ | ❌ | Danh sách cửa hàng |
| GET | `/api/v1/stores/{id}` | ✅ | ✅ | ✅ | ✅ | ❌ | Chi tiết cửa hàng |
| POST | `/api/v1/stores` | ✅ | ❌ | ❌ | ❌ | ❌ | Tạo store mới |
| PUT | `/api/v1/stores/{id}` | ✅ | ❌ | ❌ | ❌ | ❌ | Cập nhật thông tin store |
| PATCH | `/api/v1/stores/{id}/status` | ✅ | ❌ | ❌ | ❌ | ❌ | Kích hoạt/vô hiệu hoá |

---

## 4. Route Management (US-05 — dự kiến Sprint 2)

| Method | Endpoint | SA | DI | LM | WS | DR | Ghi chú |
|--------|----------|----|----|----|----|----|---------|
| GET | `/api/v1/routes` | ✅ | ✅ | ✅ | ❌ | ❌ | Danh sách tuyến cố định |
| GET | `/api/v1/routes/{id}` | ✅ | ✅ | ✅ | ❌ | ❌ | Chi tiết tuyến |
| POST | `/api/v1/routes` | ✅ | ❌ | ❌ | ❌ | ❌ | Tạo tuyến mới (Admin only) |
| PUT | `/api/v1/routes/{id}` | ✅ | ❌ | ❌ | ❌ | ❌ | Sửa tuyến (Admin only) |

---

## 5. Orders / Excel Import (US-06 — dự kiến Sprint 2)

| Method | Endpoint | SA | DI | LM | WS | DR | Ghi chú |
|--------|----------|----|----|----|----|----|---------|
| POST | `/api/v1/orders/import` | ✅ | ✅ | ❌ | ❌ | ❌ | Upload Excel (BR-01) |
| GET | `/api/v1/orders` | ✅ | ✅ | ✅ | ❌ | ❌ | Danh sách đơn hàng |
| GET | `/api/v1/orders/{id}` | ✅ | ✅ | ✅ | ❌ | ❌ | Chi tiết đơn hàng |

---

## 6. Trips (US-16 — dự kiến Sprint 4)

| Method | Endpoint | SA | DI | LM | WS | DR | Ghi chú |
|--------|----------|----|----|----|----|----|---------|
| POST | `/api/v1/trips` | ✅ | ✅ | ❌ | ❌ | ❌ | Tạo chuyến |
| GET | `/api/v1/trips` | ✅ | ✅ | ✅ | ✅ | ⚠️ | Driver chỉ thấy trip của mình |
| GET | `/api/v1/trips/{id}` | ✅ | ✅ | ✅ | ✅ | ⚠️ | Driver chỉ thấy trip của mình |
| POST | `/api/v1/trips/{id}/dispatch` | ✅ | ✅ | ❌ | ❌ | ❌ | Dispatch chuyến |
| PATCH | `/api/v1/trips/{id}/status` | ✅ | ✅ | ❌ | ✅ | ✅ | Cập nhật trạng thái |
| POST | `/api/v1/trips/{id}/epod` | ✅ | ❌ | ❌ | ❌ | ✅ | Upload ePOD (BR-11) |

**Self-resource Driver (⚠️):**
```java
@PreAuthorize("hasRole('SYSTEM_ADMIN') or hasRole('DISPATCHER') or hasRole('LOGISTICS_MANAGER') " +
              "or hasRole('WAREHOUSE_STAFF') " +
              "or (hasRole('DRIVER') and @tripSecurity.isAssignedDriver(#id, authentication.name))")
```
`TripSecurityService.isAssignedDriver()` — implement Sprint 4 khi có Trip entity.

---

## 7. Vehicles (US — dự kiến)

| Method | Endpoint | SA | DI | LM | WS | DR | Ghi chú |
|--------|----------|----|----|----|----|----|---------|
| GET | `/api/v1/vehicles` | ✅ | ✅ | ✅ | ❌ | ❌ | Danh sách phương tiện |
| GET | `/api/v1/vehicles/{id}` | ✅ | ✅ | ✅ | ❌ | ❌ | Chi tiết phương tiện |

---

## 8. System / Health

| Method | Endpoint | SA | DI | LM | WS | DR | Ghi chú |
|--------|----------|----|----|----|----|----|---------|
| GET | `/api/v1/health` | 🔓 | 🔓 | 🔓 | 🔓 | 🔓 | Health check — public |

---

## Nguyên tắc bổ sung

### SYSTEM_ADMIN — Quyền cao nhất
`SYSTEM_ADMIN` luôn có quyền truy cập mọi endpoint. Được đảm bảo qua `hasRole('SYSTEM_ADMIN')` trong từng `@PreAuthorize`.

### Driver — Self-resource (NFR Security)
Các endpoint có ký hiệu ⚠️ cần kiểm tra runtime qua `@tripSecurity` bean.
Pattern xem mục 6 ở trên. Implement Sprint 4.

### Endpoint chưa implement
Mục 4–7 được ghi để lập kế hoạch trước. Confirm lại khi US tương ứng bắt đầu phát triển.

### Cách cập nhật tài liệu này
Mỗi khi thêm endpoint mới:
1. Thêm dòng vào bảng đúng section.
2. Thêm `@PreAuthorize` lên controller method.
3. Nếu cần rule URL-level, thêm vào `SecurityConfig.authorizeHttpRequests()`.
4. Commit file này cùng PR với code thay đổi.

---

*SEP490_G104 · Cập nhật lần cuối: 2026-06-15 · Branch: feature/US-03-rbac-authorization*
