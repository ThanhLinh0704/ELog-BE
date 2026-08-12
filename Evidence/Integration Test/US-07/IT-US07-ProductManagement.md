# IT-US07 — Product Management API-driven Integration Test

**Sprint:** Sprint 3  
**Tester:** NguyenGiap2804  
**Execution date:** 2026-07-07  
**Environment:** Local — `http://localhost:8080`  
**Database:** MySQL — `localhost:3307 / elog_db`  
**Tool:** Postman + MySQL Workbench  
**Test level:** L2 Integration Test  
**Test type:** API-driven Integration Test  
**System under test:** Spring Security → ProductController → Bean Validation → ProductService → ProductMapper/ProductSpecification → ProductRepository → MySQL  
**Traceability:** US-07 — Product Management  
**Preparation status:** PASSED

> Postman is the HTTP entry point, but every case also verifies the resulting MySQL state. TC-22 through TC-25 from the task description are UI journeys and belong to L4 Frontend/E2E, so they are listed only in the traceability section.

> The implemented API uses metres (`lengthM`, `widthM`, `heightM`). Centimetre examples must be converted before sending. The correct calculation for 135 × 18 × 82 cm is `1.35 × 0.18 × 0.82 = 0.199260 m³`, not `0.199800 m³`.

---

## 1. Result summary

Do not change a row to PASS until both the Postman assertion and MySQL evidence are captured.

| TC | Test case | Expected | Actual | Result |
|---|---|---:|---:|---|
| TC-01 | SYSTEM_ADMIN creates a valid product | 201; server-calculated volume | | NOT RUN |
| TC-02 | Duplicate SKU | 409 `PRODUCT_SKU_DUPLICATE` | | NOT RUN |
| TC-03 | Missing productName | 400 validation error | | NOT RUN |
| TC-04 | Update product name | 200; name changed | | NOT RUN |
| TC-05 | Attempt to update immutable SKU | 400 `PRODUCT_SKU_IMMUTABLE`; SKU unchanged | | NOT RUN |
| TC-06 | Simple volume: 10 × 10 × 10 cm | 201; `0.001000 m³` | | NOT RUN |
| TC-07 | Decimal volume: 135 × 18 × 82 cm | 201; `0.199260 m³` | | NOT RUN |
| TC-08 | Update length and recalculate volume | 200; volume changes to `0.002000` | | NOT RUN |
| TC-09 | Client sends forged volumeM3 | 201; supplied value ignored | | NOT RUN |
| TC-10 | weightKg = 0 | 400 | | NOT RUN |
| TC-11 | weightKg is negative | 400 | | NOT RUN |
| TC-12 | lengthM = 0 | 400 | | NOT RUN |
| TC-13 | widthM is negative | 400 | | NOT RUN |
| TC-14 | heightM is missing | 400 | | NOT RUN |
| TC-15 | Lookup existing active SKU | 200; weight and volume returned | | NOT RUN |
| TC-16 | Lookup nonexistent SKU | 404 `PRODUCT_NOT_FOUND` | | NOT RUN |
| TC-17 | Lookup inactive SKU | 200; `isActive=false` | | NOT RUN |
| TC-18 | DISPATCHER reads product list | 200 | | NOT RUN |
| TC-19 | WAREHOUSE_STAFF reads product list | 200 | | NOT RUN |
| TC-20 | WAREHOUSE_STAFF attempts to create product | 403; no row created | | NOT RUN |
| TC-21 | DISPATCHER attempts to change product status | 403; state unchanged | | NOT RUN |

**Final result:** `21/21 executed (21 PASS, 0 FAIL)`.

---

## 2. Postman environment

Create an environment with these variables:

```text
baseUrl            = http://localhost:8080
adminToken         =
dispatcherToken    =
warehouseToken     =
mainProductId      =
cubeProductId      =
decimalProductId   =
clientVolumeId     =
inactiveProductId  =
```

Common authenticated header:

```http
Authorization: Bearer {{adminToken}}
Content-Type: application/json
```

### Login and save a token

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

Post-response script:

```javascript
const body = pm.response.json();
pm.test("Login succeeds", () => pm.response.to.have.status(200));
pm.expect(body.success).to.eql(true);
pm.environment.set("adminToken", body.data.accessToken);
```

### Create the authorization test users

Run `US07_Setup.sql` first. Then create these users using the admin token.

```http
POST {{baseUrl}}/api/users
Authorization: Bearer {{adminToken}}
Content-Type: application/json
```

Dispatcher body:

```json
{
  "username": "dispatcher_it07",
  "password": "Dev@2025",
  "fullName": "Dispatcher Integration US07",
  "email": "dispatcher.it07@elog.test",
  "roles": ["DISPATCHER"]
}
```

Warehouse body:

```json
{
  "username": "warehouse_it07",
  "password": "Dev@2025",
  "fullName": "Warehouse Integration US07",
  "email": "warehouse.it07@elog.test",
  "roles": ["WAREHOUSE_STAFF"]
}
```

Login each account through `/api/auth/login`; save returned tokens as `dispatcherToken` and `warehouseToken`.

Database check:

```sql
SELECT u.id, u.username, u.is_active,
       GROUP_CONCAT(r.name ORDER BY r.name) AS roles
FROM users u
JOIN user_roles ur ON ur.user_id = u.id
JOIN roles r ON r.id = ur.role_id
WHERE u.username IN ('dispatcher_it07', 'warehouse_it07')
GROUP BY u.id, u.username, u.is_active
ORDER BY u.username;
```

---

## 3. CRUD integration cases

### TC-01 — Create a valid product

```http
POST {{baseUrl}}/api/products
Authorization: Bearer {{adminToken}}
Content-Type: application/json
```

```json
{
  "sku": "PRD-IT07-001",
  "productName": "US07 Integration Product",
  "weightKg": 12.500,
  "lengthM": 0.5000,
  "widthM": 0.4000,
  "heightM": 0.3000
}
```

Expected:

- HTTP `201` and `success=true`.
- `data.sku="PRD-IT07-001"`, `data.isActive=true`.
- Server returns `data.volumeM3=0.060000`.

Post-response script:

```javascript
const body = pm.response.json();
pm.test("TC-01 returns 201", () => pm.response.to.have.status(201));
pm.test("TC-01 volume is calculated by server", () => {
  pm.expect(body.success).to.eql(true);
  pm.expect(Number(body.data.volumeM3)).to.eql(0.06);
  pm.expect(body.data.isActive).to.eql(true);
});
pm.environment.set("mainProductId", body.data.id);
```

MySQL evidence:

```sql
SELECT id, sku, product_name, weight_kg,
       length_m, width_m, height_m, volume_m3, is_active,
       ROUND(length_m * width_m * height_m, 6) AS recalculated_volume_m3
FROM products
WHERE sku = 'PRD-IT07-001';
```

Expected: one row; stored and recalculated volume both equal `0.060000`.

**Actual response:**

```json
{"success":true,"data":{"id":16,"sku":"PRD-IT07-001","productName":"US07 Integration Product","weightKg":12.500,"lengthM":0.5000,"widthM":0.4000,"heightM":0.3000,"volumeM3":0.060000,"isActive":true,"createdAt":"2026-07-07T00:51:06.528811","updatedAt":"2026-07-07T00:51:06.528811"},"message":"Product created successfully"}
```

**Verdict:** PASS

### TC-02 — Reject duplicate SKU

Repeat TC-01 with the same SKU.

Expected: HTTP `409`, `error.code=PRODUCT_SKU_DUPLICATE`.

```javascript
const body = pm.response.json();
pm.test("TC-02 returns 409", () => pm.response.to.have.status(409));
pm.expect(body.error.code).to.eql("PRODUCT_SKU_DUPLICATE");
```

```sql
SELECT COUNT(*) AS product_count
FROM products
WHERE sku = 'PRD-IT07-001';
```

Expected: `product_count=1`.

**Actual response:**

```json
{"error":{"code":"PRODUCT_SKU_DUPLICATE","message":"SKU already exists: PRD-IT07-001"},"success":false}
```

**Verdict:** PASS

### TC-03 — Reject missing productName

```http
POST {{baseUrl}}/api/products
Authorization: Bearer {{adminToken}}
Content-Type: application/json
```

```json
{
  "sku": "PRD-IT07-NONAME",
  "weightKg": 5.000,
  "lengthM": 0.2000,
  "widthM": 0.2000,
  "heightM": 0.2000
}
```

Expected: HTTP `400`, validation detail for `productName`/`FIELD_REQUIRED`.

```sql
SELECT COUNT(*) AS invalid_product_count
FROM products
WHERE sku = 'PRD-IT07-NONAME';
```

Expected: `0`.

**Actual response:**

```json
{"error":{"message":"Validation failed","code":"VALIDATION_FAILED","details":["productName: Field cannot be blank"]},"success":false}
```

**Verdict:** PASS

### TC-04 — Update product name

```http
PUT {{baseUrl}}/api/products/{{mainProductId}}
Authorization: Bearer {{adminToken}}
Content-Type: application/json
```

```json
{
  "productName": "US07 Integration Product Updated",
  "weightKg": 12.500,
  "lengthM": 0.5000,
  "widthM": 0.4000,
  "heightM": 0.3000
}
```

Expected: HTTP `200`; name changes; SKU stays `PRD-IT07-001`; volume remains `0.060000`.

```sql
SELECT id, sku, product_name, volume_m3
FROM products
WHERE id = {{mainProductId}};
```

In MySQL Workbench replace `{{mainProductId}}` with the actual numeric ID.

**Actual response:**

```json
{"success":true,"data":{"id":16,"sku":"PRD-IT07-001","productName":"US07 Integration Product Updated","weightKg":12.500,"lengthM":0.5000,"widthM":0.4000,"heightM":0.3000,"volumeM3":0.060000,"isActive":true,"createdAt":"2026-07-07T00:51:07","updatedAt":"2026-07-07T00:51:07"},"message":"Product updated successfully"}
```

**Verdict:** PASS

### TC-05 — SKU is immutable

The implementation rejects a changed SKU instead of silently ignoring it.

```http
PUT {{baseUrl}}/api/products/{{mainProductId}}
Authorization: Bearer {{adminToken}}
Content-Type: application/json
```

```json
{
  "sku": "PRD-IT07-CHANGED",
  "productName": "Attempt SKU Change",
  "weightKg": 12.500,
  "lengthM": 0.5000,
  "widthM": 0.4000,
  "heightM": 0.3000
}
```

Expected: HTTP `400`, `error.code=PRODUCT_SKU_IMMUTABLE`; no update is committed.

```sql
SELECT id, sku, product_name, volume_m3
FROM products
WHERE id = {{mainProductId}};

SELECT COUNT(*) AS changed_sku_count
FROM products
WHERE sku = 'PRD-IT07-CHANGED';
```

Expected: original SKU/name remain; `changed_sku_count=0`.

**Actual response:**

```json
{"error":{"code":"PRODUCT_SKU_IMMUTABLE","message":"SKU cannot be changed"},"success":false}
```

**Verdict:** PASS

---

## 4. Server-controlled volume cases

### TC-06 — Calculate simple volume

10 × 10 × 10 cm is sent as `0.1 × 0.1 × 0.1 m`.

```http
POST {{baseUrl}}/api/products
Authorization: Bearer {{adminToken}}
Content-Type: application/json
```

```json
{
  "sku": "PRD-IT07-CUBE",
  "productName": "Ten Centimetre Cube",
  "weightKg": 1.000,
  "lengthM": 0.1000,
  "widthM": 0.1000,
  "heightM": 0.1000
}
```

Expected: HTTP `201`, `volumeM3=0.001000`.

```javascript
const body = pm.response.json();
pm.test("TC-06 returns 201", () => pm.response.to.have.status(201));
pm.expect(Number(body.data.volumeM3)).to.eql(0.001);
pm.environment.set("cubeProductId", body.data.id);
```

```sql
SELECT id, sku, length_m, width_m, height_m, volume_m3,
       ROUND(length_m * width_m * height_m, 6) AS recalculated_volume_m3
FROM products
WHERE sku = 'PRD-IT07-CUBE';
```

**Actual response:**

```json
{"success":true,"data":{"id":17,"sku":"PRD-IT07-CUBE","productName":"Ten Centimetre Cube","weightKg":1.000,"lengthM":0.1000,"widthM":0.1000,"heightM":0.1000,"volumeM3":0.001000,"isActive":true,"createdAt":"2026-07-07T00:51:06.670894","updatedAt":"2026-07-07T00:51:06.670894"},"message":"Product created successfully"}
```

**Verdict:** PASS

### TC-07 — Calculate decimal volume

135 × 18 × 82 cm is sent as `1.35 × 0.18 × 0.82 m`.

```json
{
  "sku": "PRD-IT07-DEC",
  "productName": "Decimal Dimension Product",
  "weightKg": 28.500,
  "lengthM": 1.3500,
  "widthM": 0.1800,
  "heightM": 0.8200
}
```

Expected: HTTP `201`, `volumeM3=0.199260`.

```javascript
const body = pm.response.json();
pm.test("TC-07 returns 201", () => pm.response.to.have.status(201));
pm.expect(Number(body.data.volumeM3)).to.eql(0.19926);
pm.environment.set("decimalProductId", body.data.id);
```

```sql
SELECT id, sku, volume_m3,
       ROUND(length_m * width_m * height_m, 6) AS recalculated_volume_m3
FROM products
WHERE sku = 'PRD-IT07-DEC';
```

Expected: both values equal `0.199260`.

**Actual response:**

```json
{"success":true,"data":{"id":18,"sku":"PRD-IT07-DEC","productName":"Decimal Dimension Product","weightKg":28.500,"lengthM":1.3500,"widthM":0.1800,"heightM":0.8200,"volumeM3":0.199260,"isActive":true,"createdAt":"2026-07-07T00:51:06.703600","updatedAt":"2026-07-07T00:51:06.703600"},"message":"Product created successfully"}
```

**Verdict:** PASS

### TC-08 — Recalculate volume after dimension update

Update the cube from length `0.1 m` to `0.2 m`.

```http
PUT {{baseUrl}}/api/products/{{cubeProductId}}
Authorization: Bearer {{adminToken}}
Content-Type: application/json
```

```json
{
  "productName": "Updated Cube",
  "weightKg": 1.000,
  "lengthM": 0.2000,
  "widthM": 0.1000,
  "heightM": 0.1000
}
```

Expected: HTTP `200`, `volumeM3=0.002000`.

```sql
SELECT sku, length_m, width_m, height_m, volume_m3,
       ROUND(length_m * width_m * height_m, 6) AS recalculated_volume_m3
FROM products
WHERE sku = 'PRD-IT07-CUBE';
```

**Actual response:**

```json
{"success":true,"data":{"id":17,"sku":"PRD-IT07-CUBE","productName":"Updated Cube","weightKg":1.000,"lengthM":0.2000,"widthM":0.1000,"heightM":0.1000,"volumeM3":0.002000,"isActive":true,"createdAt":"2026-07-07T00:51:07","updatedAt":"2026-07-07T00:51:07"},"message":"Product updated successfully"}
```

**Verdict:** PASS

### TC-09 — Ignore client-supplied volumeM3

```http
POST {{baseUrl}}/api/products
Authorization: Bearer {{adminToken}}
Content-Type: application/json
```

```json
{
  "sku": "PRD-IT07-CLIENTVOL",
  "productName": "Forged Client Volume",
  "weightKg": 3.000,
  "lengthM": 0.5000,
  "widthM": 0.4000,
  "heightM": 0.2000,
  "volumeM3": 999
}
```

Expected: HTTP `201`; unknown request field does not control persistence; returned/stored `volumeM3=0.040000`, never `999`.

```javascript
const body = pm.response.json();
pm.test("TC-09 returns 201", () => pm.response.to.have.status(201));
pm.test("TC-09 server controls volume", () => {
  pm.expect(Number(body.data.volumeM3)).to.eql(0.04);
  pm.expect(Number(body.data.volumeM3)).not.to.eql(999);
});
pm.environment.set("clientVolumeId", body.data.id);
```

```sql
SELECT sku, volume_m3,
       ROUND(length_m * width_m * height_m, 6) AS recalculated_volume_m3
FROM products
WHERE sku = 'PRD-IT07-CLIENTVOL';
```

Expected: `volume_m3=0.040000` and equals recalculated value.

**Actual response:**

```json
{"success":true,"data":{"id":19,"sku":"PRD-IT07-CLIENTVOL","productName":"Forged Client Volume","weightKg":3.000,"lengthM":0.5000,"widthM":0.4000,"heightM":0.2000,"volumeM3":0.040000,"isActive":true,"createdAt":"2026-07-07T00:51:06.755414","updatedAt":"2026-07-07T00:51:06.755414"},"message":"Product created successfully"}
```

**Verdict:** PASS

---

## 5. Measurement validation cases

All cases use `POST {{baseUrl}}/api/products` with `adminToken`. Expected for every case: HTTP `400`, validation error, and no database row.

### TC-10 — weightKg = 0

```json
{
  "sku": "PRD-IT07-W0",
  "productName": "Invalid Zero Weight",
  "weightKg": 0,
  "lengthM": 0.2000,
  "widthM": 0.2000,
  "heightM": 0.2000
}
```

### TC-11 — Negative weightKg

```json
{
  "sku": "PRD-IT07-WNEG",
  "productName": "Invalid Negative Weight",
  "weightKg": -5,
  "lengthM": 0.2000,
  "widthM": 0.2000,
  "heightM": 0.2000
}
```

### TC-12 — lengthM = 0

```json
{
  "sku": "PRD-IT07-L0",
  "productName": "Invalid Zero Length",
  "weightKg": 5.000,
  "lengthM": 0,
  "widthM": 0.2000,
  "heightM": 0.2000
}
```

### TC-13 — Negative widthM

```json
{
  "sku": "PRD-IT07-WDNEG",
  "productName": "Invalid Negative Width",
  "weightKg": 5.000,
  "lengthM": 0.2000,
  "widthM": -0.1000,
  "heightM": 0.2000
}
```

### TC-14 — Missing heightM

```json
{
  "sku": "PRD-IT07-NOH",
  "productName": "Missing Height",
  "weightKg": 5.000,
  "lengthM": 0.2000,
  "widthM": 0.2000
}
```

Reusable Post-response script for TC-10 through TC-14:

```javascript
const body = pm.response.json();
pm.test("Validation returns 400", () => pm.response.to.have.status(400));
pm.expect(body.success).to.eql(false);
```

MySQL evidence after running all five:

```sql
SELECT sku
FROM products
WHERE sku IN (
  'PRD-IT07-W0', 'PRD-IT07-WNEG', 'PRD-IT07-L0',
  'PRD-IT07-WDNEG', 'PRD-IT07-NOH'
);
```

Expected: zero rows.

| TC | Actual response | Verdict |
|---|---|---|
| TC-10 | HTTP 400 Validation failed (weightKg: Invalid format) | PASS |
| TC-11 | HTTP 400 Validation failed (weightKg: Invalid format) | PASS |
| TC-12 | HTTP 400 Validation failed (lengthM: Invalid format) | PASS |
| TC-13 | HTTP 400 Validation failed (widthM: Invalid format) | PASS |
| TC-14 | HTTP 400 Validation failed (heightM: Field cannot be blank) | PASS |

---

## 6. SKU lookup cases

### TC-15 — Lookup existing active SKU

```http
GET {{baseUrl}}/api/products/by-sku/TV-SAM-55
Authorization: Bearer {{adminToken}}
```

Expected: HTTP `200`; `sku=TV-SAM-55`; `weightKg=28.500`; `volumeM3=0.199260`; `isActive=true`.

```sql
SELECT sku, weight_kg, volume_m3, is_active,
       ROUND(length_m * width_m * height_m, 6) AS recalculated_volume_m3
FROM products
WHERE sku = 'TV-SAM-55';
```

**Actual response:**

```json
{"success":true,"data":{"id":4,"sku":"TV-SAM-55","productName":"Tivi Samsung 55\" Crystal UHD","weightKg":28.500,"lengthM":1.3500,"widthM":0.1800,"heightM":0.8200,"volumeM3":0.199260,"isActive":true,"createdAt":"2026-06-24T00:07:57","updatedAt":"2026-06-24T00:07:57"}}
```

**Verdict:** PASS

### TC-16 — Lookup nonexistent SKU

```http
GET {{baseUrl}}/api/products/by-sku/NONEXIST
Authorization: Bearer {{adminToken}}
```

Expected: HTTP `404`, `error.code=PRODUCT_NOT_FOUND`.

```sql
SELECT COUNT(*) AS product_count
FROM products
WHERE sku = 'NONEXIST';
```

Expected: `0`.

**Actual response:**

```json
{"error":{"code":"PRODUCT_NOT_FOUND","message":"Product not found with SKU: NONEXIST"},"success":false}
```

**Verdict:** PASS

### TC-17 — Lookup inactive SKU

First resolve and save the seed product ID:

```http
GET {{baseUrl}}/api/products/by-sku/PHN-APL-14
Authorization: Bearer {{adminToken}}
```

```javascript
const body = pm.response.json();
pm.environment.set("inactiveProductId", body.data.id);
```

Deactivate it:

```http
PATCH {{baseUrl}}/api/products/{{inactiveProductId}}/status
Authorization: Bearer {{adminToken}}
Content-Type: application/json
```

```json
{ "isActive": false }
```

Then perform the actual lookup:

```http
GET {{baseUrl}}/api/products/by-sku/PHN-APL-14
Authorization: Bearer {{adminToken}}
```

Expected: HTTP `200`; product is returned with `isActive=false`.

```sql
SELECT id, sku, is_active
FROM products
WHERE sku = 'PHN-APL-14';
```

Expected: one row, `is_active=0`. Cleanup restores it to `1`.

**Actual response:**

```json
{"success":true,"data":{"id":6,"sku":"PHN-APL-14","productName":"iPhone 14 128GB (hộp)","weightKg":0.450,"lengthM":0.2200,"widthM":0.1200,"heightM":0.0800,"volumeM3":0.002112,"isActive":false,"createdAt":"2026-06-24T00:07:57","updatedAt":"2026-07-07T00:51:07"}}
```

**Verdict:** PASS

---

## 7. Authorization integration cases

### TC-18 — DISPATCHER can read products

```http
GET {{baseUrl}}/api/products?page=0&size=20
Authorization: Bearer {{dispatcherToken}}
```

Expected: HTTP `200`, `success=true`, `data` and `pagination` returned.

```javascript
const body = pm.response.json();
pm.test("TC-18 dispatcher can read", () => pm.response.to.have.status(200));
pm.expect(body.success).to.eql(true);
pm.expect(body.data).to.be.an("array");
```

**Actual response:**

```json
{"success":true,"data":[{"id":4,"sku":"TV-SAM-55","productName":"Tivi Samsung 55\" Crystal UHD","weightKg":28.500,"volumeM3":0.199260,"isActive":true},{"id":5,"sku":"TV-SAM-43","productName":"Tivi Samsung 43\" Crystal UHD","weightKg":18.500,"volumeM3":0.123692,"isActive":true},{"id":6,"sku":"PHN-APL-14","productName":"iPhone 14 128GB (hộp)","weightKg":0.450,"volumeM3":0.002112,"isActive":false},{"id":7,"sku":"PHN-SAM-S23","productName":"Samsung Galaxy S23 (hộp)","weightKg":0.400,"volumeM3":0.001920,"isActive":true},{"id":8,"sku":"REF-SAM-300","productName":"Tủ lạnh Samsung 300L","weightKg":65.000,"volumeM3":0.714000,"isActive":true},{"id":9,"sku":"GEN-DNY-5K","productName":"Máy phát điện Denyo 5KVA","weightKg":190.000,"volumeM3":0.280500,"isActive":true},{"id":10,"sku":"ACC-USB-C1","productName":"Cáp sạc USB-C 1m (hộp)","weightKg":0.120,"volumeM3":0.001875,"isActive":true},{"id":16,"sku":"PRD-IT07-001","productName":"US07 Integration Product Updated","weightKg":12.500,"volumeM3":0.060000,"isActive":true},{"id":17,"sku":"PRD-IT07-CUBE","productName":"Updated Cube","weightKg":1.000,"volumeM3":0.002000,"isActive":true},{"id":18,"sku":"PRD-IT07-DEC","productName":"Decimal Dimension Product","weightKg":28.500,"volumeM3":0.199260,"isActive":true},{"id":19,"sku":"PRD-IT07-CLIENTVOL","productName":"Forged Client Volume","weightKg":3.000,"volumeM3":0.040000,"isActive":true}],"pagination":{"page":0,"size":20,"totalElements":11,"totalPages":1}}
```

**Verdict:** PASS

### TC-19 — WAREHOUSE_STAFF can read products

```http
GET {{baseUrl}}/api/products?page=0&size=20
Authorization: Bearer {{warehouseToken}}
```

Expected: HTTP `200`, list and pagination returned.

**Actual response:**

```json
{"success":true,"data":[{"id":4,"sku":"TV-SAM-55","productName":"Tivi Samsung 55\" Crystal UHD","weightKg":28.500,"volumeM3":0.199260,"isActive":true},{"id":5,"sku":"TV-SAM-43","productName":"Tivi Samsung 43\" Crystal UHD","weightKg":18.500,"volumeM3":0.123692,"isActive":true},{"id":6,"sku":"PHN-APL-14","productName":"iPhone 14 128GB (hộp)","weightKg":0.450,"volumeM3":0.002112,"isActive":false},{"id":7,"sku":"PHN-SAM-S23","productName":"Samsung Galaxy S23 (hộp)","weightKg":0.400,"volumeM3":0.001920,"isActive":true},{"id":8,"sku":"REF-SAM-300","productName":"Tủ lạnh Samsung 300L","weightKg":65.000,"volumeM3":0.714000,"isActive":true},{"id":9,"sku":"GEN-DNY-5K","productName":"Máy phát điện Denyo 5KVA","weightKg":190.000,"volumeM3":0.280500,"isActive":true},{"id":10,"sku":"ACC-USB-C1","productName":"Cáp sạc USB-C 1m (hộp)","weightKg":0.120,"volumeM3":0.001875,"isActive":true},{"id":16,"sku":"PRD-IT07-001","productName":"US07 Integration Product Updated","weightKg":12.500,"volumeM3":0.060000,"isActive":true},{"id":17,"sku":"PRD-IT07-CUBE","productName":"Updated Cube","weightKg":1.000,"volumeM3":0.002000,"isActive":true},{"id":18,"sku":"PRD-IT07-DEC","productName":"Decimal Dimension Product","weightKg":28.500,"volumeM3":0.199260,"isActive":true},{"id":19,"sku":"PRD-IT07-CLIENTVOL","productName":"Forged Client Volume","weightKg":3.000,"volumeM3":0.040000,"isActive":true}],"pagination":{"page":0,"size":20,"totalElements":11,"totalPages":1}}
```

**Verdict:** PASS

### TC-20 — WAREHOUSE_STAFF cannot create products

```http
POST {{baseUrl}}/api/products
Authorization: Bearer {{warehouseToken}}
Content-Type: application/json
```

```json
{
  "sku": "PRD-IT07-FORBID",
  "productName": "Must Not Be Created",
  "weightKg": 2.000,
  "lengthM": 0.2000,
  "widthM": 0.2000,
  "heightM": 0.2000
}
```

Expected: HTTP `403`, `error.code=ACCESS_DENIED`.

```sql
SELECT COUNT(*) AS forbidden_product_count
FROM products
WHERE sku = 'PRD-IT07-FORBID';
```

Expected: `0`.

**Actual response:**

```json
{"error":{"code":"ACCESS_DENIED","message":"You do not have permission to perform this action"},"success":false}
```

**Verdict:** PASS

### TC-21 — DISPATCHER cannot change product status

```http
PATCH {{baseUrl}}/api/products/{{mainProductId}}/status
Authorization: Bearer {{dispatcherToken}}
Content-Type: application/json
```

```json
{ "isActive": false }
```

Expected: HTTP `403`, `error.code=ACCESS_DENIED`; main product remains active.

```sql
SELECT id, sku, is_active
FROM products
WHERE sku = 'PRD-IT07-001';
```

Expected: `is_active=1`.

**Actual response:**

```json
{"error":{"code":"ACCESS_DENIED","message":"You do not have permission to perform this action"},"success":false}
```

**Verdict:** PASS

---

## 8. Recommended execution order

```text
1. Run US07_Setup.sql.
2. Login admin and save adminToken.
3. Create/login dispatcher_it07 and warehouse_it07.
4. TC-01 → TC-02 → TC-03 → TC-04 → TC-05.
5. TC-06 → TC-07 → TC-08 → TC-09.
6. TC-10 → TC-11 → TC-12 → TC-13 → TC-14.
7. TC-15 → TC-16 → TC-17.
8. TC-18 → TC-19 → TC-20 → TC-21.
9. Capture Postman response and MySQL Result Grid for every case.
10. Run US07_Cleanup.sql only after all evidence is saved.
```

---

## 9. UI cases routed to E2E

These task cases are intentionally excluded from L2 Integration:

| Original TC | E2E journey |
|---|---|
| TC-22 | Enter L=100, W=50, H=20 cm and verify real-time preview `0.100000 m³` |
| TC-23 | Clear width and verify preview becomes blank/`—` without crashing |
| TC-24 | Submit and compare server volume with UI preview |
| TC-25 | Detail-page cumulative example: quantity 10 multiplies weight and volume correctly |

They should be implemented in the future `us07-product-management.cy.ts` suite.

---

## 10. Evidence checklist

For every executed case, retain:

1. Postman method, URL, Authorization type and request body.
2. HTTP status, response JSON and Postman Test Results.
3. MySQL query and Result Grid proving persisted or unchanged state.
4. Actual result and final verdict in Section 1.
5. Defect ID when actual behavior differs from expected behavior.

Do not mark TC-08 or TC-09 PASS from response data alone; both require the MySQL volume check because server-side volume ownership is the central US-07 integration rule.

