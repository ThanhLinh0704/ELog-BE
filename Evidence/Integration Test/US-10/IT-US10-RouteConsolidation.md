# IT-US10 — L2 Integration Testing (Route Consolidation)

**Mô tả:** Kiểm thử tích hợp (L2) luồng gom tuyến đơn hàng dựa trên khớp dữ liệu giữa các bảng `orders`, `trip_drafts`, và `trip_draft_stops` trong cơ sở dữ liệu MySQL.

**Sprint:** Sprint 3  
**Tester:** [Tester Name]  
**Execution date:** [Execution Date]  
**Environment:** Local — `http://localhost:8080`  
**Database:** MySQL — `localhost:3307 / elog_db`  
**Tool:** Postman + MySQL Workbench  
**Test level:** L2 Integration Test  

---

## 1. Tóm tắt kết quả kiểm thử (Summary)

| Test ID | UC Ref | Title | Expected Result | Actual Result | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **L2-CON-01** | UC-05, UC-06 | Gom tuyến thành công (Happy Path) | Tạo 2 Trip Drafts cho 2 tuyến RT-001 và RT-002, tính đúng tổng cân nặng/thể tích | | NOT RUN |
| **L2-CON-02** | UC-06 | Gom tuyến trùng lặp ngày (Idempotency) | Cập nhật lại đè lên Draft cũ có trạng thái `DRAFT` thành công, không tạo trùng record | | NOT RUN |
| **L2-CON-03** | UC-06, BR-07 | Chặn gom tuyến khi Draft đã Khóa (Locked Guard) | Trả về lỗi `409 Conflict` (TRIP_DRAFT_LOCKED) khi gom tuyến cho ngày đã được xác nhận | | NOT RUN |

---

## 2. Chi tiết từng bước thực thi và kiểm tra Database

### ⚙️ Bước 0: Chuẩn bị Dữ liệu Sạch (Chạy trên MySQL Workbench)
Trước khi test, hãy đưa Database về trạng thái có sẵn đơn hàng cho ngày `2026-03-24` nhưng **chưa gom tuyến**:
```sql
USE elog_db;

-- 1. Reset các lô hàng và đơn hàng cũ của ngày 24/03 nếu có
DELETE FROM trip_draft_stops WHERE trip_draft_id IN (SELECT id FROM trip_drafts WHERE delivery_date = '2026-03-24');
UPDATE orders SET trip_draft_id = NULL WHERE delivery_date = '2026-03-24';
DELETE FROM trip_drafts WHERE delivery_date = '2026-03-24';
DELETE FROM orders WHERE delivery_date = '2026-03-24';
DELETE FROM import_batches WHERE delivery_date = '2026-03-24';

-- 2. Tạo một Lô hàng ảo đã import thành công cho ngày 2026-03-24
INSERT INTO import_batches (id, delivery_date, file_name, uploaded_by, total_rows, accepted_rows, rejected_rows, status, is_active)
VALUES (999, '2026-03-24', 'dummy_l2_us10.xlsx', 2, 3, 3, 0, 'COMPLETED', TRUE);

-- 3. Tạo 3 đơn hàng trạng thái ACCEPTED (đã qua bước validate US-09)
-- Đơn 1: Cửa hàng Q1 (ST-001) - thuộc Tuyến RT-001 (id=1)
INSERT INTO orders (id, import_batch_id, order_ref, store_id, delivery_date, status)
VALUES (9991, 999, 'DH-CON-01', 1, '2026-03-24', 'ACCEPTED');
INSERT INTO order_items (order_id, sku, quantity, unit_weight_kg, unit_volume_m3, line_weight_kg, line_volume_m3)
VALUES (9991, 'PRD-001', 2, 25.000, 0.142240, 50.000, 0.284480);

-- Đơn 2: Cửa hàng Q3 (ST-002) - thuộc Tuyến RT-001 (id=1)
INSERT INTO orders (id, import_batch_id, order_ref, store_id, delivery_date, status)
VALUES (9992, 999, 'DH-CON-02', 2, '2026-03-24', 'ACCEPTED');
INSERT INTO order_items (order_id, sku, quantity, unit_weight_kg, unit_volume_m3, line_weight_kg, line_volume_m3)
VALUES (9992, 'PRD-002', 1, 75.000, 0.808360, 75.000, 0.808360);

-- Đơn 3: Cửa hàng Bình Thạnh (ST-003) - thuộc Tuyến RT-002 (id=2)
INSERT INTO orders (id, import_batch_id, order_ref, store_id, delivery_date, status)
VALUES (9993, 999, 'DH-CON-03', 3, '2026-03-24', 'ACCEPTED');
INSERT INTO order_items (order_id, sku, quantity, unit_weight_kg, unit_volume_m3, line_weight_kg, line_volume_m3)
VALUES (9993, 'PRD-003', 1, 55.000, 0.300600, 55.000, 0.300600);
```

---

### 🧪 L2-CON-01: Gom tuyến thành công (Happy Path)

**Các bước thực hiện:**
1. Mở Postman, gửi request POST Gom tuyến cho ngày `2026-03-24`:
   * **Method**: `POST`
   * **URL**: `http://localhost:8080/api/trip-drafts/consolidate`
   * **Headers**: `Authorization: Bearer <Dispatcher_Token>`
   * **Body (JSON)**:
     ```json
     {
         "deliveryDate": "2026-03-24"
     }
     ```
2. Mở MySQL Workbench và chạy câu lệnh kiểm thử:
   ```sql
   USE elog_db;
   -- Kiểm tra xem có đúng 2 Trip Drafts được tạo ra cho ngày 2026-03-24 không
   SELECT id, route_id, total_volume_m3, total_weight_kg, active_stop_count, status 
   FROM trip_drafts 
   WHERE delivery_date = '2026-03-24';
   ```

**Kết quả mong đợi:**
* Postman trả về: `200 OK` với thông báo `"Consolidation hoàn tất"`.
* Kết quả SQL trả về đúng **2 dòng**:
  * Tuyến **RT-001** (route_id = 1): `total_volume_m3` = 1.092840 (0.284480 + 0.808360), `total_weight_kg` = 125.000, `active_stop_count` = 2, `status` = 'DRAFT'.
  * Tuyến **RT-002** (route_id = 2): `total_volume_m3` = 0.300600, `total_weight_kg` = 55.000, `active_stop_count` = 1, `status` = 'DRAFT'.
* Chạy câu lệnh kiểm tra liên kết đơn hàng:
  ```sql
  -- Xác nhận các đơn hàng đã được cập nhật trường trip_draft_id
  SELECT id, order_ref, trip_draft_id FROM orders WHERE delivery_date = '2026-03-24';
  ```
  *(Các đơn hàng 9991, 9992 phải trỏ về trip_draft_id của tuyến RT-001, đơn 9993 trỏ về trip_draft_id của tuyến RT-002)*.

---

### 🧪 L2-CON-02: Gom tuyến trùng lặp ngày (Idempotency)

**Các bước thực hiện:**
1. Không dọn dẹp DB, giữ nguyên dữ liệu vừa gom ở case trước.
2. Gửi lại request POST Consolidation y hệt ở case L2-CON-01 trên Postman một lần nữa.
3. Chạy truy vấn SQL để kiểm tra số lượng bản ghi:
   ```sql
   USE elog_db;
   SELECT COUNT(*) AS total_drafts FROM trip_drafts WHERE delivery_date = '2026-03-24';
   ```

**Kết quả mong đợi:**
* Postman trả về: `200 OK` thành công.
* Kết quả SQL `total_drafts` vẫn là **`2`** (Hệ thống tự động ghi đè/cập nhật thông tin chứ không tạo bản ghi mới trùng lặp, đảm bảo tính idempotent).

---

### 🧪 L2-CON-03: Chặn gom tuyến khi Draft đã Xác nhận (Locked Guard)

**Các bước thực hiện:**
1. Giả lập việc Dispatcher đã xác nhận (confirm) kế hoạch gom tuyến bằng cách đổi trạng thái của lô hàng sang `PLANNED` trong MySQL Workbench:
   ```sql
   USE elog_db;
   UPDATE trip_drafts SET status = 'PLANNED' WHERE delivery_date = '2026-03-24';
   ```
2. Gửi lại request POST Gom tuyến ngày `2026-03-24` trên Postman.
3. Chạy truy vấn SQL kiểm tra xem dữ liệu có bị ghi đè trái phép không:
   ```sql
   USE elog_db;
   SELECT status, total_weight_kg FROM trip_drafts WHERE delivery_date = '2026-03-24';
   ```

**Kết quả mong đợi:**
* Postman trả về: **`409 Conflict`**.
* JSON trả về chứa mã lỗi: `"code": "TRIP_DRAFT_LOCKED"` kèm thông điệp báo rằng bản nháp đã được xác nhận, không được sửa đổi.
* Kết quả SQL: Cột `status` vẫn giữ nguyên là `PLANNED`, không bị hạ về `DRAFT` hay bị ghi đè thông số cân nặng.

---

## 3. Dọn dẹp dữ liệu kiểm thử (Cleanup)
Chạy script này để dọn dẹp các bản ghi nháp vừa tạo:
```sql
USE elog_db;
DELETE FROM trip_draft_stops WHERE trip_draft_id IN (SELECT id FROM trip_drafts WHERE delivery_date = '2026-03-24');
UPDATE orders SET trip_draft_id = NULL WHERE delivery_date = '2026-03-24';
DELETE FROM trip_drafts WHERE delivery_date = '2026-03-24';
DELETE FROM orders WHERE delivery_date = '2026-03-24';
DELETE FROM import_batches WHERE id = 999;
```
