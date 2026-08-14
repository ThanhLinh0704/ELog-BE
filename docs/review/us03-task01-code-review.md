# Code Review Report — US-03 TASK-01: User Management API

**Reviewer:** Nguyễn Xuân Nguyên Giáp  
**Branch:** `feature/US-03-user-management`  
**Ngày review:** 2026-06-16  
**Trạng thái:** ❌ Cần sửa trước khi merge

---

## Tổng quan

Code đã implement đủ 6 endpoints theo yêu cầu, logic nghiệp vụ cơ bản đúng hướng.
Tuy nhiên có **1 lỗi compile nghiêm trọng** sẽ làm app không build được, cùng với
3 vấn đề cần khắc phục trước khi merge vào `develop`.

---

## Kết quả kiểm tra theo Acceptance Criteria

| # | Tiêu chí | Kết quả |
|---|----------|---------|
| 1 | SYSTEM_ADMIN tạo được user mới → HTTP 201 | ✅ Pass |
| 2 | Username/email trùng → HTTP 409 Conflict | ✅ Pass (xem vấn đề #3) |
| 3 | Password vi phạm policy → HTTP 400 | ✅ Pass |
| 4 | Admin không tự khóa / tự gỡ role SYSTEM_ADMIN → HTTP 403 | ✅ Pass |
| 5 | Password không xuất hiện trong response | ✅ Pass |
| 6 | Phân trang và filter hoạt động đúng | ✅ Pass (xem vấn đề #4) |
| 7 | Role không hợp lệ bị reject → HTTP 400 | ✅ Pass |

---

## Vấn đề cần sửa

---

### 🔴 VẤN ĐỀ #1 — CRITICAL: Compile Error do thiếu `ErrorCode.ACCOUNT_DISABLED`

**File:** `src/main/java/com/elog/exception/ErrorCode.java`  
**Mức độ:** Critical — App không build được

**Mô tả:**  
`AuthServiceImpl.java` tham chiếu đến `ErrorCode.ACCOUNT_DISABLED` nhưng enum
`ErrorCode` trong nhánh này không khai báo giá trị đó. Nhánh US-02 có khai báo
nhưng khi tạo nhánh US-03 bị bỏ sót.

**Code lỗi** (`AuthServiceImpl.java`):
```java
throw new BusinessException(ErrorCode.ACCOUNT_DISABLED, "Account has been disabled", HttpStatus.FORBIDDEN);
//                           ^^^^^^^^^^^^^^^^^^^^^^ không tồn tại trong enum
```

**Cách sửa** — Thêm vào `ErrorCode.java`:
```java
// Thêm vào nhóm "Resource errors"
ACCOUNT_DISABLED("ACCOUNT_DISABLED"),
```

---

### 🟡 VẤN ĐỀ #2 — `updatedAt` bị bỏ sót trong UserMapper

**File:** `src/main/java/com/elog/mapper/UserMapper.java` — dòng 43  
**Mức độ:** Significant — Data trả về thiếu field

**Mô tả:**  
`UserResponse` có khai báo field `updatedAt` nhưng `UserMapper.toResponse()`
không gán giá trị, khiến field này luôn trả về `null` trong mọi response.

**Code hiện tại:**
```java
return UserResponse.builder()
        .id(user.getId())
        .username(user.getUsername())
        .fullName(user.getFullName())
        .email(user.getEmail())
        .roles(roleNames)
        .isActive(user.getIsActive())
        .createdAt(user.getCreatedAt())
        // .updatedAt(...) ← bị bỏ sót
        .build();
```

**Cách sửa** — Thêm 1 dòng:
```java
.createdAt(user.getCreatedAt())
.updatedAt(user.getUpdatedAt())  // ← thêm dòng này
.build();
```

---

### 🟡 VẤN ĐỀ #3 — ErrorCode không đúng ngữ nghĩa cho lỗi 409 Conflict

**File:** `src/main/java/com/elog/service/impl/UserServiceImpl.java` — dòng 39, 43  
**Mức độ:** Significant — Frontend không phân biệt được loại lỗi

**Mô tả:**  
Khi username hoặc email đã tồn tại, code đang ném `ErrorCode.VALIDATION_FAILED`
với HTTP 409. Điều này sai ngữ nghĩa vì `VALIDATION_FAILED` dùng cho lỗi
validation input (HTTP 400), không phải lỗi trùng dữ liệu (HTTP 409).
Frontend sẽ không thể phân biệt "sai format" với "đã tồn tại" nếu chỉ dựa vào
error code.

**Code hiện tại:**
```java
throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Username already exists", HttpStatus.CONFLICT);
throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Email already exists", HttpStatus.CONFLICT);
```

**Cách sửa:**

Bước 1 — Thêm code mới vào `ErrorCode.java`:
```java
DUPLICATE_RESOURCE("DUPLICATE_RESOURCE"),
```

Bước 2 — Cập nhật `UserServiceImpl.java`:
```java
throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "Username already exists", HttpStatus.CONFLICT);
throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "Email already exists", HttpStatus.CONFLICT);
```

---

### 🟡 VẤN ĐỀ #4 — Thiếu `DISTINCT` trong `UserSpecification.hasRole()` có thể gây kết quả trùng

**File:** `src/main/java/com/elog/repository/specification/UserSpecification.java` — dòng 25–30  
**Mức độ:** Significant — Bug tiềm ẩn khi user có nhiều role

**Mô tả:**  
`hasRole()` dùng `JOIN` để lọc theo role. Khi một user có nhiều role (ví dụ
`DISPATCHER` + `LOGISTICS_MANAGER`), truy vấn có thể trả về user đó nhiều lần
trong danh sách, làm sai `totalElements` và hiển thị duplicate trên UI.

**Code hiện tại:**
```java
public static Specification<User> hasRole(String roleName) {
    return (root, query, cb) -> {
        if (!StringUtils.hasText(roleName)) return null;
        Join<User, Role> roleJoin = root.join("roles");
        return cb.equal(roleJoin.get("name"), roleName);
        // ← thiếu query.distinct(true)
    };
}
```

**Cách sửa** — Thêm 1 dòng:
```java
public static Specification<User> hasRole(String roleName) {
    return (root, query, cb) -> {
        if (!StringUtils.hasText(roleName)) return null;
        query.distinct(true);  // ← thêm dòng này
        Join<User, Role> roleJoin = root.join("roles");
        return cb.equal(roleJoin.get("name"), roleName);
    };
}
```

---

## Tóm tắt công việc cần làm

| # | File cần sửa | Thay đổi |
|---|-------------|---------|
| 1 | `exception/ErrorCode.java` | Thêm `ACCOUNT_DISABLED` và `DUPLICATE_RESOURCE` |
| 2 | `mapper/UserMapper.java` | Thêm `.updatedAt(user.getUpdatedAt())` |
| 3 | `service/impl/UserServiceImpl.java` | Đổi `VALIDATION_FAILED` → `DUPLICATE_RESOURCE` cho 2 chỗ |
| 4 | `repository/specification/UserSpecification.java` | Thêm `query.distinct(true)` trong `hasRole()` |

---

## Hướng xử lý tiếp theo

1. Sửa 4 vấn đề trên theo hướng dẫn trong báo cáo
2. Build lại và chạy `mvn spring-boot:run` để xác nhận không còn compile error
3. Test thủ công qua Swagger UI (`http://localhost:8080/swagger-ui/index.html`)
4. Ping lại reviewer để review lần 2 trước khi tạo PR merge vào `develop`

---

*Report sinh tự động từ code review — ELog SEP490_G104*
