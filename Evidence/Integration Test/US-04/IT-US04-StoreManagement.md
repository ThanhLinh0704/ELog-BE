# IT-US04 — Store Management Integration Test

**Sprint:** Sprint 2
**Tester:** NguyenGiap2804
**Date:**
**Environment:** Local — `http://localhost:8080`
**Tool:** Postman
**Test Level:** L2 Integration Test
**Test Type:** API-driven Integration Test
**System Under Test:** StoreController + StoreService + StoreRepository + RouteStop integration checks + Spring Security + MySQL

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
| `manager01` | `Dev@2025` | `LOGISTICS_MANAGER` | true |

**Dữ liệu seed sẵn có:**

| ID | Code | Tên | Trong tuyến |
|----|------|-----|-------------|
| 1 | `ST-001` | Cửa hàng Quận 1 | RT-001 (active) |
| 2 | `ST-002` | Cửa hàng Quận 3 | RT-001 (active) |
| 3 | `ST-003` | Cửa hàng Bình Thạnh | RT-002 (active) |

**ID tạo ra trong quá trình test (điền sau khi chạy TC-02, TC-03):**

| Code | ID |
|------|----|
| `ST-IT04-001` | ? |
| `ST-IT04-002` | ? |

---

## Kết quả tổng hợp

| # | Test Case | Expected | Actual | Kết quả |
|---|-----------|----------|--------|---------|
| TC-01 | SYSTEM_ADMIN GET /api/stores | 200 | | |
| TC-02 | Tạo store có toạ độ | 201 | | |
| TC-03 | Tạo store không có toạ độ | 201 | | |
| TC-04 | GET /api/stores/{id} | 200 | | |
| TC-05 | PUT cập nhật address, storeCode không đổi | 200 | | |
| TC-06 | PATCH deactivate (không trong tuyến active) | 200 | | |
| TC-07 | PATCH reactivate | 200 | | |
| TC-08 | DISPATCHER GET /api/stores | 200 | | |
| TC-09 | LOGISTICS_MANAGER GET /api/stores/{id} | 200 | | |
| TC-10 | DISPATCHER POST /api/stores | 403 | | |
| TC-11 | Không có token GET /api/stores | 401 | | |
| TC-12 | Tạo store thiếu storeName | 400 | | |
| TC-13 | Tạo store latitude=91 (out of range) | 400 | | |
| TC-14 | Tạo store longitude=181 (out of range) | 400 | | |
| TC-15 | Tạo store chỉ có latitude, thiếu longitude | 400 | | |
| TC-16 | Tạo store chỉ có longitude, thiếu latitude | 400 | | |
| TC-17 | Tạo store storeCode trùng | 409 | | |
| TC-18 | PATCH deactivate store đang trong tuyến active | 409 | | |
| TC-19 | GET /api/stores/{id} — ID không tồn tại | 404 | | |
| TC-20 | GET ?isActive=false | 200 | | |
| TC-21 | GET ?hasRoute=false | 200 | | |
| TC-22 | GET ?keyword=IT04 | 200 | | |

**Tổng: /22 PASS**

---

## Chi tiết từng Test Case

---

### TC-01 — SYSTEM_ADMIN gọi GET /api/stores

**Mục tiêu:** Role SYSTEM_ADMIN có quyền xem danh sách stores → HTTP 200 kèm pagination

**Dữ liệu test:**
```
-- Bước 1: Lấy token admin
Method: POST
URL: http://localhost:8080/api/auth/login
Headers: Content-Type: application/json
Body: { "username": "admin", "password": "Admin@2025" }

-- Bước 2: Gọi API
Method: GET
URL: http://localhost:8080/api/stores
Headers: Authorization: Bearer <accessToken từ bước 1>
```

**Các bước thực hiện:**
1. Login `admin` / `Admin@2025` → copy `data.accessToken`
2. Tạo request `GET http://localhost:8080/api/stores`
3. Tab Authorization → Bearer Token → paste token
4. Send

**Kết quả mong muốn:**
- HTTP `200`
- `success: true`
- `data` là mảng, có ít nhất 3 stores (ST-001, ST-002, ST-003)
- `pagination.totalElements >= 3`

**Kết quả thực tế:**
```json

```

**Verdict:**

---

### TC-02 — Tạo store mới có toạ độ GPS

**Mục tiêu:** SYSTEM_ADMIN tạo store hợp lệ với đầy đủ lat/lng → HTTP 201, isActive=true

**Dữ liệu test:**
```
Method: POST
URL: http://localhost:8080/api/stores
Headers:
  Content-Type: application/json
  Authorization: Bearer <token admin>
Body:
{
  "storeCode": "ST-IT04-001",
  "storeName": "Store Integration Test 01",
  "address": "10 Le Lai, Quan 1, TP.HCM",
  "contactName": "Nguyen Van Test",
  "contactPhone": "0901234567",
  "latitude": 10.7756587,
  "longitude": 106.7004238
}
```

**Các bước thực hiện:**
1. Login `admin` / `Admin@2025` → copy `data.accessToken`
2. Tạo request `POST http://localhost:8080/api/stores`
3. Điền header + body như trên
4. Send
5. **Ghi lại `data.id`** → điền vào bảng ID (STORE_ID_1) để dùng cho TC-04 đến TC-09

**Kết quả mong muốn:**
- HTTP `201`
- `data.storeCode: "ST-IT04-001"`
- `data.isActive: true`
- `data.assignedRoute: null`

**Cleanup sau test (chạy sau TC-22 khi hoàn tất toàn bộ):**
```sql
DELETE FROM stores WHERE code = 'ST-IT04-001';
```

**Kết quả thực tế:**
```json

```

**Verdict:**

---

### TC-03 — Tạo store mới không có toạ độ GPS

**Mục tiêu:** Store không bắt buộc GPS → HTTP 201 khi latitude và longitude đều null

**Dữ liệu test:**
```
Method: POST
URL: http://localhost:8080/api/stores
Headers:
  Content-Type: application/json
  Authorization: Bearer <token admin>
Body:
{
  "storeCode": "ST-IT04-002",
  "storeName": "Store Integration Test 02",
  "address": "20 Nguyen Hue, Quan 1, TP.HCM"
}
```

**Các bước thực hiện:**
1. Login `admin` / `Admin@2025` → copy `data.accessToken`
2. Tạo request `POST http://localhost:8080/api/stores`
3. Điền header + body như trên (không có latitude/longitude)
4. Send
5. **Ghi lại `data.id`** → điền vào bảng ID (STORE_ID_2)

**Kết quả mong muốn:**
- HTTP `201`
- `data.storeCode: "ST-IT04-002"`
- `data.isActive: true`
- `data.latitude: null`, `data.longitude: null`
- `data.hasCoordinates: false` *(field trong list response)*

**Cleanup sau test (chạy sau TC-22 khi hoàn tất toàn bộ):**
```sql
DELETE FROM stores WHERE code = 'ST-IT04-002';
```

**Kết quả thực tế:**
```json

```

**Verdict:**

---

### TC-04 — GET chi tiết store theo ID

**Mục tiêu:** SYSTEM_ADMIN lấy detail store vừa tạo → HTTP 200 đầy đủ thông tin

**Yêu cầu:** Đã chạy TC-02, có STORE_ID_1

**Dữ liệu test:**
```
Method: GET
URL: http://localhost:8080/api/stores/{STORE_ID_1}
Headers: Authorization: Bearer <token admin>
```

**Các bước thực hiện:**
1. Login `admin` / `Admin@2025` → copy `data.accessToken`
2. Tạo request `GET http://localhost:8080/api/stores/{STORE_ID_1}` (thay ID thật)
3. Send

**Kết quả mong muốn:**
- HTTP `200`
- `data.storeCode: "ST-IT04-001"`
- `data.latitude: 10.7756587`
- `data.assignedRoute: null`
- `data.createdAt` không null

**Kết quả thực tế:**
```json

```

**Verdict:**

---

### TC-05 — PUT cập nhật thông tin store

**Mục tiêu:** Cập nhật address → HTTP 200, address mới được lưu, storeCode không thay đổi

**Yêu cầu:** Đã chạy TC-02, có STORE_ID_1

**Dữ liệu test:**
```
Method: PUT
URL: http://localhost:8080/api/stores/{STORE_ID_1}
Headers:
  Content-Type: application/json
  Authorization: Bearer <token admin>
Body:
{
  "storeName": "Store Integration Test 01 (Updated)",
  "address": "99 Le Lai, Quan 1, TP.HCM",
  "contactName": "Tran Thi Updated",
  "contactPhone": "0909999999",
  "latitude": 10.7756587,
  "longitude": 106.7004238
}
```

**Các bước thực hiện:**
1. Login `admin` / `Admin@2025` → copy `data.accessToken`
2. Tạo request `PUT http://localhost:8080/api/stores/{STORE_ID_1}`
3. Điền header + body như trên
4. Send

**Kết quả mong muốn:**
- HTTP `200`
- `data.address: "99 Le Lai, Quan 1, TP.HCM"`
- `data.storeName: "Store Integration Test 01 (Updated)"`
- `data.storeCode: "ST-IT04-001"` *(không thay đổi)*

**Kết quả thực tế:**
```json

```

**Verdict:**

---

### TC-06 — PATCH deactivate store không thuộc tuyến active

**Mục tiêu:** Deactivate ST-IT04-001 (không có route_stop) → HTTP 200, isActive=false

**Yêu cầu:** Đã chạy TC-02, có STORE_ID_1. ST-IT04-001 không có trong bất kỳ tuyến nào.

**Dữ liệu test:**
```
Method: PATCH
URL: http://localhost:8080/api/stores/{STORE_ID_1}/status
Headers:
  Content-Type: application/json
  Authorization: Bearer <token admin>
Body: { "isActive": false }
```

**Các bước thực hiện:**
1. Login `admin` / `Admin@2025` → copy `data.accessToken`
2. Tạo request `PATCH http://localhost:8080/api/stores/{STORE_ID_1}/status`
3. Điền header + body như trên
4. Send

**Kết quả mong muốn:**
- HTTP `200`
- `data.isActive: false`

**Ghi chú:** Giữ nguyên trạng thái deactivated để chạy TC-20 (`?isActive=false`), restore sau TC-20.

**Kết quả thực tế:**
```json

```

**Verdict:**

---

### TC-07 — PATCH reactivate store

**Mục tiêu:** Kích hoạt lại store đang inactive → HTTP 200, isActive=true, không cần kiểm tra route

**Yêu cầu:** Đã chạy TC-06 (STORE_ID_1 đang isActive=false) VÀ TC-20 đã chạy xong

**Dữ liệu test:**
```
Method: PATCH
URL: http://localhost:8080/api/stores/{STORE_ID_1}/status
Headers:
  Content-Type: application/json
  Authorization: Bearer <token admin>
Body: { "isActive": true }
```

**Các bước thực hiện:**
1. Login `admin` / `Admin@2025` → copy `data.accessToken`
2. Tạo request `PATCH http://localhost:8080/api/stores/{STORE_ID_1}/status`
3. Điền body `{ "isActive": true }`
4. Send

**Kết quả mong muốn:**
- HTTP `200`
- `data.isActive: true`

**Kết quả thực tế:**
```json

```

**Verdict:**

---

### TC-08 — DISPATCHER gọi GET /api/stores

**Mục tiêu:** Role DISPATCHER có quyền đọc danh sách stores → HTTP 200

**Dữ liệu test:**
```
Method: POST  URL: http://localhost:8080/api/auth/login
Body: { "username": "dispatcher01", "password": "Dev@2025" }

Method: GET  URL: http://localhost:8080/api/stores
Headers: Authorization: Bearer <token dispatcher01>
```

**Các bước thực hiện:**
1. Login `dispatcher01` / `Dev@2025` → copy `data.accessToken`
2. Tạo request `GET http://localhost:8080/api/stores` với token dispatcher01
3. Send

**Kết quả mong muốn:**
- HTTP `200`
- `success: true`

**Kết quả thực tế:**
```json

```

**Verdict:**

---

### TC-09 — LOGISTICS_MANAGER gọi GET /api/stores/{id}

**Mục tiêu:** Role LOGISTICS_MANAGER có quyền đọc chi tiết store → HTTP 200

**Yêu cầu:** Đã chạy TC-02, có STORE_ID_1

**Dữ liệu test:**
```
Method: POST  URL: http://localhost:8080/api/auth/login
Body: { "username": "manager01", "password": "Dev@2025" }

Method: GET  URL: http://localhost:8080/api/stores/{STORE_ID_1}
Headers: Authorization: Bearer <token manager01>
```

**Các bước thực hiện:**
1. Login `manager01` / `Dev@2025` → copy `data.accessToken`
2. Tạo request `GET http://localhost:8080/api/stores/{STORE_ID_1}` với token manager01
3. Send

**Kết quả mong muốn:**
- HTTP `200`
- `success: true`

**Kết quả thực tế:**
```json

```

**Verdict:**

---

### TC-10 — DISPATCHER gọi POST /api/stores

**Mục tiêu:** Role DISPATCHER không có quyền tạo store → HTTP 403

**Dữ liệu test:**
```
Method: POST  URL: http://localhost:8080/api/auth/login
Body: { "username": "dispatcher01", "password": "Dev@2025" }

Method: POST  URL: http://localhost:8080/api/stores
Headers:
  Content-Type: application/json
  Authorization: Bearer <token dispatcher01>
Body:
{
  "storeCode": "ST-FORBIDDEN",
  "storeName": "Should Not Create",
  "address": "Should Not Create 123"
}
```

**Các bước thực hiện:**
1. Login `dispatcher01` / `Dev@2025` → copy `data.accessToken`
2. Tạo request `POST http://localhost:8080/api/stores` với token dispatcher01
3. Send

**Kết quả mong muốn:**
- HTTP `403`
- `error.code: "ACCESS_DENIED"`

**Kết quả thực tế:**
```json

```

**Verdict:**

---

### TC-11 — Không có token gọi GET /api/stores

**Mục tiêu:** Request không có Authorization header → HTTP 401

**Dữ liệu test:**
```
Method: GET
URL: http://localhost:8080/api/stores
Headers: (không có Authorization)
```

**Các bước thực hiện:**
1. Tạo request `GET http://localhost:8080/api/stores`
2. Đảm bảo KHÔNG có Authorization header
3. Send

**Kết quả mong muốn:**
- HTTP `401`
- `error.code: "AUTHENTICATION_FAILED"`

**Kết quả thực tế:**
```json

```

**Verdict:**

---

### TC-12 — Tạo store thiếu storeName (bắt buộc)

**Mục tiêu:** `storeName` là field bắt buộc, để trống → HTTP 400 VALIDATION_FAILED

**Dữ liệu test:**
```
Method: POST
URL: http://localhost:8080/api/stores
Headers:
  Content-Type: application/json
  Authorization: Bearer <token admin>
Body:
{
  "storeCode": "ST-NO-NAME",
  "address": "10 Le Lai, Quan 1, TP.HCM"
}
```

**Các bước thực hiện:**
1. Login `admin` / `Admin@2025` → copy token
2. Gửi POST với body trên (không có storeName)
3. Send

**Kết quả mong muốn:**
- HTTP `400`
- `error.code: "VALIDATION_FAILED"`
- `error.details` chứa lỗi field `storeName`

**Kết quả thực tế:**
```json

```

**Verdict:**

---

### TC-13 — Tạo store với latitude vượt giới hạn (> 90)

**Mục tiêu:** latitude=91 vi phạm `@DecimalMax(90.0)` → HTTP 400 VALIDATION_FAILED

**Dữ liệu test:**
```
Method: POST
URL: http://localhost:8080/api/stores
Headers:
  Content-Type: application/json
  Authorization: Bearer <token admin>
Body:
{
  "storeCode": "ST-BAD-LAT",
  "storeName": "Bad Latitude Store",
  "address": "10 Le Lai, Quan 1, TP.HCM",
  "latitude": 91.0,
  "longitude": 106.7004238
}
```

**Các bước thực hiện:**
1. Login `admin` / `Admin@2025` → copy token
2. Gửi POST với latitude=91.0
3. Send

**Kết quả mong muốn:**
- HTTP `400`
- `error.code: "VALIDATION_FAILED"`
- `error.details` chứa lỗi field `latitude`

**Kết quả thực tế:**
```json

```

**Verdict:**

---

### TC-14 — Tạo store với longitude vượt giới hạn (> 180)

**Mục tiêu:** longitude=181 vi phạm `@DecimalMax(180.0)` → HTTP 400 VALIDATION_FAILED

**Dữ liệu test:**
```
Method: POST
URL: http://localhost:8080/api/stores
Headers:
  Content-Type: application/json
  Authorization: Bearer <token admin>
Body:
{
  "storeCode": "ST-BAD-LNG",
  "storeName": "Bad Longitude Store",
  "address": "10 Le Lai, Quan 1, TP.HCM",
  "latitude": 10.7756587,
  "longitude": 181.0
}
```

**Các bước thực hiện:**
1. Login `admin` / `Admin@2025` → copy token
2. Gửi POST với longitude=181.0
3. Send

**Kết quả mong muốn:**
- HTTP `400`
- `error.code: "VALIDATION_FAILED"`
- `error.details` chứa lỗi field `longitude`

**Kết quả thực tế:**
```json

```

**Verdict:**

---

### TC-15 — Tạo store chỉ có latitude, không có longitude

**Mục tiêu:** Chỉ truyền latitude mà thiếu longitude → HTTP 400 INVALID_COORDINATES (service-level check)

**Dữ liệu test:**
```
Method: POST
URL: http://localhost:8080/api/stores
Headers:
  Content-Type: application/json
  Authorization: Bearer <token admin>
Body:
{
  "storeCode": "ST-LAT-ONLY",
  "storeName": "Lat Only Store",
  "address": "10 Le Lai, Quan 1, TP.HCM",
  "latitude": 10.7756587
}
```

**Các bước thực hiện:**
1. Login `admin` / `Admin@2025` → copy token
2. Gửi POST với chỉ latitude (không có longitude)
3. Send

**Kết quả mong muốn:**
- HTTP `400`
- `error.code: "INVALID_COORDINATES"`

**Kết quả thực tế:**
```json

```

**Verdict:**

---

### TC-16 — Tạo store chỉ có longitude, không có latitude

**Mục tiêu:** Chỉ truyền longitude mà thiếu latitude → HTTP 400 INVALID_COORDINATES

**Dữ liệu test:**
```
Method: POST
URL: http://localhost:8080/api/stores
Headers:
  Content-Type: application/json
  Authorization: Bearer <token admin>
Body:
{
  "storeCode": "ST-LNG-ONLY",
  "storeName": "Lng Only Store",
  "address": "10 Le Lai, Quan 1, TP.HCM",
  "longitude": 106.7004238
}
```

**Các bước thực hiện:**
1. Login `admin` / `Admin@2025` → copy token
2. Gửi POST với chỉ longitude (không có latitude)
3. Send

**Kết quả mong muốn:**
- HTTP `400`
- `error.code: "INVALID_COORDINATES"`

**Kết quả thực tế:**
```json

```

**Verdict:**

---

### TC-17 — Tạo store với storeCode đã tồn tại

**Mục tiêu:** `ST-001` đã tồn tại trong DB → HTTP 409 STORE_CODE_DUPLICATE

**Dữ liệu test:**
```
Method: POST
URL: http://localhost:8080/api/stores
Headers:
  Content-Type: application/json
  Authorization: Bearer <token admin>
Body:
{
  "storeCode": "ST-001",
  "storeName": "Duplicate Store",
  "address": "10 Le Lai, Quan 1, TP.HCM"
}
```

**Các bước thực hiện:**
1. Login `admin` / `Admin@2025` → copy token
2. Gửi POST với storeCode="ST-001" (đã có trong seed)
3. Send

**Kết quả mong muốn:**
- HTTP `409`
- `error.code: "STORE_CODE_DUPLICATE"`
- `error.message` chứa `"ST-001"`

**Kết quả thực tế:**
```json

```

**Verdict:**

---

### TC-18 — PATCH deactivate store đang thuộc tuyến active

**Mục tiêu:** ST-001 đang là điểm dừng của RT-001 (is_active=true) → HTTP 409 STORE_ACTIVE_ROUTE

**Dữ liệu test:**
```
Method: PATCH
URL: http://localhost:8080/api/stores/1/status
Headers:
  Content-Type: application/json
  Authorization: Bearer <token admin>
Body: { "isActive": false }
```

**Các bước thực hiện:**
1. Login `admin` / `Admin@2025` → copy token
2. Tạo request `PATCH http://localhost:8080/api/stores/1/status`
3. Điền body `{ "isActive": false }`
4. Send

**Kết quả mong muốn:**
- HTTP `409`
- `error.code: "STORE_ACTIVE_ROUTE"`
- `error.message` chứa mã tuyến `"RT-001"`

**Kết quả thực tế:**
```json

```

**Verdict:**

---

### TC-19 — GET store theo ID không tồn tại

**Mục tiêu:** ID=9999 không có trong DB → HTTP 404 STORE_NOT_FOUND

**Dữ liệu test:**
```
Method: GET
URL: http://localhost:8080/api/stores/9999
Headers: Authorization: Bearer <token admin>
```

**Các bước thực hiện:**
1. Login `admin` / `Admin@2025` → copy token
2. Tạo request `GET http://localhost:8080/api/stores/9999`
3. Send

**Kết quả mong muốn:**
- HTTP `404`
- `error.code: "STORE_NOT_FOUND"`

**Kết quả thực tế:**
```json

```

**Verdict:**

---

### TC-20 — GET danh sách stores filter isActive=false

**Mục tiêu:** Filter `?isActive=false` → chỉ trả các store đang inactive

**Yêu cầu:** Chạy sau TC-06 (STORE_ID_1 đang isActive=false)

**Dữ liệu test:**
```
Method: GET
URL: http://localhost:8080/api/stores?isActive=false
Headers: Authorization: Bearer <token admin>
```

**Các bước thực hiện:**
1. Login `admin` / `Admin@2025` → copy token
2. Tạo request `GET http://localhost:8080/api/stores?isActive=false`
3. Send

**Kết quả mong muốn:**
- HTTP `200`
- `data` chứa ST-IT04-001 (vừa deactivate ở TC-06)
- Tất cả phần tử trong `data` có `isActive: false`

**Restore:** Sau TC này chạy TC-07 để reactivate ST-IT04-001.

**Kết quả thực tế:**
```json

```

**Verdict:**

---

### TC-21 — GET danh sách stores filter hasRoute=false

**Mục tiêu:** Filter `?hasRoute=false` → chỉ trả store chưa gắn tuyến nào

**Yêu cầu:** Đã chạy TC-02, ST-IT04-001 chưa có route_stop

**Dữ liệu test:**
```
Method: GET
URL: http://localhost:8080/api/stores?hasRoute=false
Headers: Authorization: Bearer <token admin>
```

**Các bước thực hiện:**
1. Login `admin` / `Admin@2025` → copy token
2. Tạo request `GET http://localhost:8080/api/stores?hasRoute=false`
3. Send

**Kết quả mong muốn:**
- HTTP `200`
- `data` chứa ST-IT04-001 và ST-IT04-002
- Tất cả phần tử trong `data` có `assignedRoute: null`
- ST-001, ST-002, ST-003 **không xuất hiện** (đều đã có tuyến)

**Kết quả thực tế:**
```json

```

**Verdict:**

---

### TC-22 — GET danh sách stores filter keyword

**Mục tiêu:** Filter `?keyword=IT04` → partial match theo tên/mã → trả đúng store

**Yêu cầu:** Đã chạy TC-02 và TC-03

**Dữ liệu test:**
```
Method: GET
URL: http://localhost:8080/api/stores?keyword=IT04
Headers: Authorization: Bearer <token admin>
```

**Các bước thực hiện:**
1. Login `admin` / `Admin@2025` → copy token
2. Tạo request `GET http://localhost:8080/api/stores?keyword=IT04`
3. Send

**Kết quả mong muốn:**
- HTTP `200`
- `data` chứa ST-IT04-001 và ST-IT04-002
- Không chứa ST-001, ST-002, ST-003

**Cleanup sau test:**
```sql
DELETE FROM stores WHERE code IN ('ST-IT04-001', 'ST-IT04-002');
```

**Kết quả thực tế:**
```json

```

**Verdict:**

---

## Thứ tự chạy TC (dependency)

```
TC-01 → TC-02 → TC-03 → TC-04 → TC-05 → TC-06
                                           ↓
                                         TC-20 → TC-07
TC-08, TC-09, TC-10, TC-11 (độc lập, chạy bất kỳ lúc nào)
TC-12 → TC-17 (độc lập nhau, không tạo dữ liệu thật)
TC-18 (dùng store id=1 từ seed, không cần setup)
TC-19 (độc lập)
TC-21, TC-22 (cần TC-02, TC-03 chạy trước)
Cleanup: Chạy SQL xóa sau TC-22
```

---

## Bug Report

*(Điền sau khi hoàn tất test)*
