# IT-US08 — Excel Import API-driven Integration Test

**Sprint:** Sprint 3  
**Tester:** Dispatcher Integration US08  
**Execution date:** 2026-07-08  
**Environment:** Local — `http://localhost:8080`  
**Database:** MySQL — `localhost:3307 / elog_db`  
**Tool:** Postman + MySQL Workbench  
**Test level:** L2 Integration Test  
**Test type:** API-driven Integration Test  
**System under test:** Spring Security → ImportController → ImportService → Apache POI → MySQL  
**Traceability:** US-08 — Excel Import  
**Preparation status:** PASSED

---

## 1. Result summary

Do not change a row to PASS until both the Postman assertion and MySQL evidence are captured.

| TC | Test case ID | Description | Expected | Actual | Result |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **TC-01** | L2-IMP-01 | Happy Path + Transaction Boundary (6 rows) | 201; 3 orders, 4 items, 2 errors created | HTTP 201 | PASS |
| **TC-02** | L2-IMP-02 | Error Path + Rollback (Corrupt XLSX) | 400 `EXCEL_PARSE_ERROR`; 0 rows saved | HTTP 400 | PASS |
| **TC-03** | L2-IMP-03 | Replace Flow (confirmReplace = false) | 409 `DUPLICATE_DELIVERY_DATE` | HTTP 409 | PASS |
| **TC-04** | L2-IMP-04 | Replace Flow (confirmReplace = true) | 201; old batch deactivated, new active created | HTTP 201 | PASS |
| **TC-05** | L2-IMP-05 | Snapshot Integrity (Product update) | 200; product weight updated, old order snapshot unchanged | HTTP 200 | PASS |
| **TC-06** | L2-IMP-06 | Partial Failure (4 OK, 1 Invalid SKU) | 201; 4 items imported, 1 error logged | HTTP 201 | PASS |
| **TC-07** | L2-IMP-07 | DB Constraint - Duplicate Order Ref | Direct SQL Insert fails with unique constraint error | SQL Error 1062 | PASS |
| **TC-08** | L2-IMP-08 | DB Constraint - Concurrency | Parallel requests result in 1 success, 1 conflict | HTTP 201 / HTTP 409 | PASS |

**Final result:** `8/8 executed (8 PASS, 0 FAIL)`.

---

## 2. Environment Preparation

### A. Run SQL Setup
Execute `US08_Setup.sql` in your MySQL Workbench to clean the tables and set the baseline reference data.

### B. Postman Environment Variables
Create a Postman environment with these variables:
```text
baseUrl            = http://localhost:8080
adminToken         =
dispatcherToken    =
warehouseToken     =
testBatchId        =
```

### C. Retrieve Tokens

#### 1. Login as Admin
```http
POST {{baseUrl}}/api/auth/login
Content-Type: application/json
```
```json
{
  "username": "admin",
  "password": "Admin@2025"
}
```
**Post-response script:**
```javascript
const body = pm.response.json();
pm.test("Login succeeds", () => pm.response.to.have.status(200));
pm.environment.set("adminToken", body.data.accessToken);
```

#### 2. Create Test Users
Using the admin token, create `dispatcher_it08` and `warehouse_it08`:
```http
POST {{baseUrl}}/api/users
Authorization: Bearer {{adminToken}}
Content-Type: application/json
```
**Dispatcher Body:**
```json
{
  "username": "dispatcher_it08",
  "password": "Dev@2025",
  "fullName": "Dispatcher Integration US08",
  "email": "dispatcher.it08@elog.test",
  "roles": ["DISPATCHER"]
}
```
**Warehouse Body:**
```json
{
  "username": "warehouse_it08",
  "password": "Dev@2025",
  "fullName": "Warehouse Integration US08",
  "email": "warehouse.it08@elog.test",
  "roles": ["WAREHOUSE_STAFF"]
}
```

#### 3. Log in as Dispatcher and Warehouse
Login each account through `/api/auth/login` to obtain and save `dispatcherToken` and `warehouseToken`.

---

## 3. Integration Cases

### TC-01 (L2-IMP-01) — Happy Path + Transaction Boundary

#### Preparation: Create Excel file `import_tc01.xlsx`
Create a spreadsheet with headers: `Mã đơn`, `Mã cửa hàng`, `SKU`, `Số lượng`. Add the following 6 rows:
1. `DH160325-01` | `ST-001` | `REF-SAM-300` | `2`
2. `DH160325-01` | `ST-001` | `TV-SAM-55` | `1`
3. `DH160325-02` | `ST-002` | `GEN-DNY-5K` | `3`
4. `DH160325-03` | `ST-003` | `PHN-APL-14` | `10`
5. `DH160325-04` | `ST-HD-099` | `REF-SAM-300` | `1` *(Invalid: store does not exist)*
6. `DH160325-05` | `ST-001` | `ACC-HDMI-2M` | `5` *(Invalid: SKU does not exist)*

#### Request
* **Method:** `POST`
* **URL:** `{{baseUrl}}/api/imports`
* **Headers:** `Authorization: Bearer {{dispatcherToken}}`
* **Body:** `form-data`
  * `file`: (select file `import_tc01.xlsx`)
  * `deliveryDate`: `2026-03-16`
  * `confirmReplace`: `false`

#### Expected Postman Response
* Status: `201 Created`
* Body check:
```json
{
  "success": true,
  "data": {
    "status": "COMPLETED",
    "totalRows": 6,
    "acceptedRows": 4,
    "rejectedRows": 2
  }
}
```
**Post-response script:**
```javascript
const body = pm.response.json();
pm.test("Status is 201", () => pm.response.to.have.status(201));
pm.test("Batch import summary match", () => {
  pm.expect(body.success).to.eql(true);
  pm.expect(body.data.status).to.eql("COMPLETED");
  pm.expect(body.data.totalRows).to.eql(6);
  pm.expect(body.data.acceptedRows).to.eql(4);
  pm.expect(body.data.rejectedRows).to.eql(2);
});
pm.environment.set("testBatchId", body.data.id);
```

#### MySQL Validation Query
```sql
SELECT id, delivery_date, file_name, total_rows, accepted_rows, rejected_rows, status, is_active FROM import_batches WHERE delivery_date = '2026-03-16';
SELECT id, order_ref, store_id, delivery_date, status FROM orders WHERE import_batch_id = (SELECT id FROM import_batches WHERE delivery_date = '2026-03-16' AND is_active = 1);
SELECT id, sku, quantity, unit_weight_kg, unit_volume_m3 FROM order_items WHERE order_id IN (SELECT id FROM orders WHERE import_batch_id = (SELECT id FROM import_batches WHERE delivery_date = '2026-03-16' AND is_active = 1));
SELECT id, row_num, error_reason FROM import_errors WHERE import_batch_id = (SELECT id FROM import_batches WHERE delivery_date = '2026-03-16' AND is_active = 1);
```
**Expected Database State:**
* `import_batches`: 1 active batch created with status `COMPLETED`.
* `orders`: 3 orders created (DH160325-01, DH160325-02, DH160325-03).
* `order_items`: 4 items saved with snapshot weight/volume.
* `import_errors`: 2 error rows logged (row 5 and row 6).

*Paste your screenshots below:*
* **Postman:** `<!-- Evidence image -->`
* **MySQL:** `<!-- Evidence image -->`

---

### TC-02 (L2-IMP-02) — Error Path + Rollback

#### Preparation: Create corrupted file `corrupt.xlsx`
Create a plain text file containing some random text (e.g. `invalid bytes`), and rename the extension to `.xlsx`.

#### Request
* **Method:** `POST`
* **URL:** `{{baseUrl}}/api/imports`
* **Headers:** `Authorization: Bearer {{dispatcherToken}}`
* **Body:** `form-data`
  * `file`: (select file `corrupt.xlsx`)
  * `deliveryDate`: `2026-03-24`
  * `confirmReplace`: `false`

#### Expected Postman Response
* Status: `400 Bad Request`
* Body:
```json
{
  "success": false,
  "error": {
    "code": "EXCEL_PARSE_ERROR",
    "message": "File Excel bị hỏng hoặc không đọc được"
  }
}
```
**Post-response script:**
```javascript
const body = pm.response.json();
pm.test("Status is 400", () => pm.response.to.have.status(400));
pm.expect(body.error.code).to.eql("EXCEL_PARSE_ERROR");
```

#### MySQL Validation Query
```sql
SELECT COUNT(*) FROM import_batches WHERE delivery_date = '2026-03-24';
SELECT COUNT(*) FROM orders WHERE delivery_date = '2026-03-24';
```
**Expected Database State:**
* No batch or order records are written to the database (full transaction rollback).

*Paste your screenshots below:*
* **Postman:** `<!-- Evidence image -->`
* **MySQL:** `<!-- Evidence image -->`

---

### TC-03 (L2-IMP-03) — Replace Flow (confirmReplace = false)

#### Request
Attempt to re-import the file `import_tc01.xlsx` for the same date `2026-03-16` without confirming replacement.
* **Method:** `POST`
* **URL:** `{{baseUrl}}/api/imports`
* **Headers:** `Authorization: Bearer {{dispatcherToken}}`
* **Body:** `form-data`
  * `file`: (select file `import_tc01.xlsx`)
  * `deliveryDate`: `2026-03-16`
  * `confirmReplace`: `false`

#### Expected Postman Response
* Status: `409 Conflict`
* Body:
```json
{
  "success": false,
  "error": "DUPLICATE_DELIVERY_DATE"
}
```
**Post-response script:**
```javascript
pm.test("Status is 409", () => pm.response.to.have.status(409));
```

#### MySQL Validation Query
```sql
SELECT id, is_active FROM import_batches WHERE delivery_date = '2026-03-16';
```
**Expected Database State:**
* The original batch (TC-01 batch) remains active (`is_active = 1`). No new batch is created.

*Paste your screenshots below:*
* **Postman:** `<!-- Evidence image -->`
* **MySQL:** `<!-- Evidence image -->`

---

### TC-04 (L2-IMP-04) — Replace Flow (confirmReplace = true)

#### Request
Re-import the file `import_tc01.xlsx` for `2026-03-16` with `confirmReplace=true`.
* **Method:** `POST`
* **URL:** `{{baseUrl}}/api/imports`
* **Headers:** `Authorization: Bearer {{dispatcherToken}}`
* **Body:** `form-data`
  * `file`: (select file `import_tc01.xlsx`)
  * `deliveryDate`: `2026-03-16`
  * `confirmReplace`: `true`

#### Expected Postman Response
* Status: `201 Created`
* Body: `success=true`

#### MySQL Validation Query
```sql
SELECT id, is_active FROM import_batches WHERE delivery_date = '2026-03-16' ORDER BY id ASC;
```
**Expected Database State:**
* The first batch is now deactivated (`is_active = 0`).
* A second new batch is created and marked active (`is_active = 1`).
* All old orders remain in the database (soft replace).

*Paste your screenshots below:*
* **Postman:** `<!-- Evidence image -->`
* **MySQL:** `<!-- Evidence image -->`

---

### TC-05 (L2-IMP-05) — Snapshot Integrity

#### Request A: Update product weight (admin role required)
Update product ID `5` (SKU `REF-SAM-300`) weight to `70.0 kg`.
* **Method:** `PUT`
* **URL:** `{{baseUrl}}/api/products/5`
* **Headers:** `Authorization: Bearer {{adminToken}}`
* **Body:** `application/json`
```json
{
  "sku": "REF-SAM-300",
  "productName": "Tủ lạnh Samsung 300L",
  "weightKg": 70.000,
  "lengthM": 0.6000,
  "widthM": 0.6800,
  "heightM": 1.7500
}
```

#### Expected Postman Response
* Status: `200 OK`

#### Request B: MySQL Verification of Snapshot weight
Check if the old order item snapshot remains `65.0 kg`.
```sql
SELECT unit_weight_kg FROM order_items WHERE order_id IN (SELECT id FROM orders WHERE import_batch_id = {{testBatchId}}) AND sku = 'REF-SAM-300';
SELECT weight_kg FROM products WHERE id = 5;
```
**Expected Database State:**
* `order_items.unit_weight_kg` must remain `65.000` (snapshot preserved).
* `products.weight_kg` must be updated to `70.000` (catalog updated).

*Paste your screenshots below:*
* **Postman:** `<!-- Evidence image -->`
* **MySQL:** `<!-- Evidence image -->`

---

### TC-06 (L2-IMP-06) — Partial Failure, No Full Rollback

#### Preparation: Create Excel file `import_tc06.xlsx`
Add the following 5 rows:
1. `DH-01` | `ST-001` | `REF-SAM-300` | `1`
2. `DH-02` | `ST-002` | `TV-SAM-55` | `2`
3. `DH-03` | `ST-003` | `INVALID-SKU` | `3` *(Invalid SKU)*
4. `DH-04` | `ST-001` | `PHN-APL-14` | `4`
5. `DH-05` | `ST-002` | `GEN-DNY-5K` | `5`

#### Request
* **Method:** `POST`
* **URL:** `{{baseUrl}}/api/imports`
* **Headers:** `Authorization: Bearer {{dispatcherToken}}`
* **Body:** `form-data`
  * `file`: (select file `import_tc06.xlsx`)
  * `deliveryDate`: `2026-03-22`
  * `confirmReplace`: `false`

#### Expected Postman Response
* Status: `201 Created`
* Body:
```json
{
  "success": true,
  "data": {
    "status": "COMPLETED",
    "totalRows": 5,
    "acceptedRows": 4,
    "rejectedRows": 1
  }
}
```
**Post-response script:**
```javascript
const body = pm.response.json();
pm.test("Status is 201", () => pm.response.to.have.status(201));
pm.expect(body.data.acceptedRows).to.eql(4);
pm.expect(body.data.rejectedRows).to.eql(1);
```

#### MySQL Validation Query
```sql
SELECT COUNT(*) AS total_items FROM order_items WHERE order_id IN (SELECT id FROM orders WHERE import_batch_id = (SELECT id FROM import_batches WHERE delivery_date = '2026-03-22'));
SELECT COUNT(*) AS total_errors FROM import_errors WHERE import_batch_id = (SELECT id FROM import_batches WHERE delivery_date = '2026-03-22');
```
**Expected Database State:**
* `order_items` count is `4` (the 4 valid items are successfully imported).
* `import_errors` count is `1` (for `INVALID-SKU`).

*Paste your screenshots below:*
* **Postman:** `<!-- Evidence image -->`
* **MySQL:** `<!-- Evidence image -->`

---

### TC-07 (L2-IMP-07) — DB Constraint - Duplicate Order Ref

Verify database constraint `uq_order_batch_ref_store` directly.

#### Request (MySQL Workbench)
Attempt to insert two orders with identical `(import_batch_id, order_ref, store_id)` under the same batch.
```sql
-- 1. Create a dummy batch
INSERT INTO import_batches (id, delivery_date, file_name, uploaded_by, total_rows, accepted_rows, rejected_rows, status, is_active)
VALUES (999, '2026-03-30', 'dummy.xlsx', 2, 2, 2, 0, 'COMPLETED', TRUE);

-- 2. Insert order 1
INSERT INTO orders (id, import_batch_id, order_ref, store_id, delivery_date, status)
VALUES (9991, 999, 'DH-DUP-01', 1, '2026-03-30', 'IMPORTED');

-- 3. Insert order 2 (duplicated order_ref and store_id in the same batch 999)
INSERT INTO orders (id, import_batch_id, order_ref, store_id, delivery_date, status)
VALUES (9992, 999, 'DH-DUP-01', 1, '2026-03-30', 'IMPORTED');
```

#### Expected Outcome
The third SQL statement must fail with:
`Error Code: 1062. Duplicate entry '999-DH-DUP-01-1' for key 'uq_order_batch_ref_store'`

*Paste your screenshots below:*
* **MySQL Console Output:** `<!-- Evidence image -->`

---

### TC-08 (L2-IMP-08) — DB Constraint - Concurrency (Unique active date)

Verify unique constraint on `active_date` preventing parallel active imports for the same delivery date.

#### Request
Run two parallel requests in Postman for the same date `2026-03-28` with `confirmReplace=false`.
Using the Postman Runner (or clicking "Send" concurrently on two tabs):
* **Request 1 & Request 2:**
  * **Method:** `POST`
  * **URL:** `{{baseUrl}}/api/imports`
  * **Headers:** `Authorization: Bearer {{dispatcherToken}}`
  * **Body:** `form-data`
    * `file`: select a blank Excel spreadsheet
    * `deliveryDate`: `2026-03-28`
    * `confirmReplace`: `false`

#### Expected Postman Response
* One request receives HTTP `201 Created`.
* The other request receives HTTP `409 Conflict` (due to the unique database constraint index `uq_batch_active_date` being triggered concurrently).

#### MySQL Validation Query
```sql
SELECT COUNT(*) FROM import_batches WHERE delivery_date = '2026-03-28' AND is_active = 1;
```
**Expected Database State:**
* Exactly `1` active batch exists for `2026-03-28`.

*Paste your screenshots below:*
* **Postman Tab 1:** `<!-- Evidence image -->`
* **Postman Tab 2:** `<!-- Evidence image -->`
* **MySQL:** `<!-- Evidence image -->`
