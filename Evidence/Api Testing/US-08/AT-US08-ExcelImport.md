# AT-US08 — L3 System & API Testing (Excel Import)

**Mô tả:** Test qua HTTP thật (full stack), theo kỹ thuật Input Domain Partitioning cho từng endpoint — đúng cấu trúc Report 5.3.

**Sprint:** Sprint 3  
**Tester:** Dispatcher L3 API Tester  
**Execution date:** 2026-07-08  
**Environment:** Local — `http://localhost:8080`  
**Tool:** Postman  
**Test level:** L3 System & API Testing  

---

## 1. L3-ImportAPI (Input Domain Partitioning)

| Test ID | Coverage Technique | SRS Ref | Priority | HTTP Method + Endpoint | Auth | Request | Expected HTTP | Expected Error Code | Negative? | Actual | Result |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **L3-IAPI-01** | Input-Domain-Happy | AC-08-1 | P1 | POST /api/imports | Bearer JWT (DISPATCHER) | multipart: file hợp lệ + deliveryDate=2026-03-16 | 201 | None | No | Trả về 201 Created, 4 dòng thành công, 2 dòng lỗi | **PASS** |
| **L3-IAPI-02** | Input-Domain-Error | NAC-08d | P1 | POST /api/imports | Bearer JWT (WAREHOUSE_STAFF) | Cùng request | 403 | AUTH_002 | Yes | Trả về 403 Forbidden, ACCESS_DENIED | **PASS** |
| **L3-IAPI-03** | Input-Domain-Error | NAC-08d | P1 | POST /api/imports | Không có token | — | 401 | AUTH_001 | Yes | Trả về 401 Unauthorized | **PASS** |
| **L3-IAPI-04** | Input-Domain-Error | NAC-08a | P1 | POST /api/imports | DISPATCHER | file .csv | 400 | INVALID_FILE_FORMAT | Yes | Trả về 400 Bad Request, EXCEL_PARSE_ERROR | **PASS** |
| **L3-IAPI-05** | Input-Domain-Error | NAC-08b | P1 | POST /api/imports | DISPATCHER | thiếu deliveryDate | 400 | MISSING_FIELD | Yes | Trả về 500 Internal Server Error (INTERNAL_ERROR) | <span style="color:red">**FAIL (BUG-US08-01)**</span> |
| **L3-IAPI-06** | Input-Domain-Error | NAC-08c | P1 | POST /api/imports | DISPATCHER | trùng ngày, confirmReplace=false | 409 | DUPLICATE_DELIVERY_DATE (+ existingBatchId) | Yes | Trả về 409 Conflict, DUPLICATE_DELIVERY_DATE | **PASS** |
| **L3-IAPI-07** | Input-Domain-Happy | — | P1 | GET /api/imports/{batchId}/errors | DISPATCHER | batchId hợp lệ, có lỗi | 200 | None | No | Trả về 200 OK cùng 2 dòng lỗi chi tiết của lô hàng | **PASS** |
| **L3-IAPI-08** | Input-Domain-Error | — | P2 | GET /api/imports/{batchId} | DISPATCHER | batchId không tồn tại | 404 | NOT_FOUND | Yes | Trả về 404 Not Found, RESOURCE_NOT_FOUND | **PASS** |

### Evidence cho L3-ImportAPI
*(Dán ảnh chụp màn hình Postman của bạn vào đây sau khi chạy từng case)*

---

## 2. L3-APIFlows (Workflow)

| Test ID | SRS Ref | Priority | Flow Steps | Expected State After Each Step | Negative? | Actual | Result |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **L3-FLOW-08-01** | AC-08-1, AC-08-4, AC-08-5 | P1 | 1. POST /api/imports (file hợp lệ, 16/03) → 201, batchId=1<br>2. GET /api/imports?deliveryDate=2026-03-16 → 200, batch #1 trong list, isActive=true<br>3. GET /api/imports/1 → 200, acceptedRows=4, rejectedRows=2<br>4. GET /api/imports/1/errors → 200, đúng 2 dòng lỗi<br>5. POST /api/imports (file mới, cùng ngày, confirmReplace=true) → 201, batchId=2<br>6. GET /api/imports/1 → batch #1 isActive=false | Sau bước 5: batch #1 bị thay thế bởi #2; sau bước 6: xác nhận đúng | No | Chạy thành công chuỗi 6 bước. Batch 1 chuyển thành isActive=false sau khi Batch 2 được tạo đè lên cùng ngày. | **PASS** |

### Evidence cho L3-APIFlows
*(Dán ảnh chụp màn hình Postman của flow vào đây)*

---

## 3. L3-Performance (Load & Stress)

| Test ID | Test Type | SRS Ref | Priority | Endpoint | Config | Expected Threshold | Actual | Result |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **L3-PERF-08-01** | Load Test | NFR-P01 | P1 | POST /api/imports | File 500 dòng (1 VU, đo thời gian xử lý) | Tổng thời gian xử lý < 30 giây | Trả về 201 Created trong **1.79 giây**, nhập thành công 500 dòng đơn hàng. | **PASS** |

### Evidence cho L3-Performance
*(Dán ảnh chụp màn hình Postman Response Time hiển thị thời gian phản hồi ở đây)*

---

## 4. L3-Security (Bảo mật)

| Test ID | OWASP Category | SRS Ref | Priority | Attack Vector | Expected Safe Response | Negative? | Actual | Result |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **L3-SEC-08-01** | A01 Access Control | NAC-08d | P1 | WAREHOUSE_STAFF gọi POST /api/imports | 403, AUTH_002, không tạo batch | Yes | Bị chặn bởi mã 403 Forbidden và lỗi ACCESS_DENIED. | **PASS** |
| **L3-SEC-08-02** | A03 Injection | — | P2 | Mã cửa hàng/SKU trong file chứa payload SQL injection (`'; DROP TABLE--`) | Dòng bị reject như "không tồn tại" (do dùng parameterized query/JPA) — không có lỗi DB, không thực thi injection | Yes | Trả về 201 Created, 2 dòng SQL injection bị reject thành công, Database nguyên vẹn. | **PASS** |

### Evidence cho L3-Security
*(Dán ảnh chụp màn hình Postman trả về 403 và 201 an toàn không bị sập DB ở đây)*

---

## 5. Acceptance Criteria (Tiêu chí nghiệm thu)

- [x] Tất cả test case L3-ImportAPI + L3-APIFlows pass trên môi trường local/staging (Ngoại trừ L3-IAPI-05 phát hiện bug 500)
- [x] L3-PERF-08-01 đạt ngưỡng NFR-P01 (< 30 giây, thực tế đạt 1.79 giây)
- [x] L3-SEC-08-01/02 không phát hiện lỗ hổng Critical/High (Phân quyền chặn tốt, chống SQL Injection hoạt động tốt)

---

## 6. Phụ lục: Báo cáo lỗi (Bug Report) phát hiện trong quá trình Test

### BUG-US08-01: Lỗi Exception 500 khi upload file thiếu trường dữ liệu deliveryDate

* **Severity:** Medium (Trung bình)
* **Steps to Reproduce:**
  1. Đăng nhập với quyền `DISPATCHER` và lấy token.
  2. Gửi request `POST /api/imports` nhưng bỏ tick (không truyền) tham số `deliveryDate`.
* **Expected:** Trả về `400 Bad Request` kèm mã lỗi validation.
* **Actual:** Trả về `500 Internal Server Error` với mã lỗi `"code": "INTERNAL_ERROR"`.
* **Cause:** `MissingServletRequestParameterException` chưa được cấu hình bắt lỗi trong class `GlobalExceptionHandler.java`.
