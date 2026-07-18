# IT-US05 — Route Management API-driven Integration Test

**Sprint:** Sprint 2  
**Tester:** NguyenGiap2804  
**Execution date:** 2026-06-28  
**Environment:** Local — `http://localhost:8080`  
**Database:** MySQL 8 — `localhost:3307 / elog_db`  
**Tool:** Postman + MySQL Workbench  
**Test level:** L2 Integration Test  
**Test type:** API-driven Integration Test  
**System under test:** Spring Security → RouteController → validation → RouteService → RouteRepository/RouteStopRepository/StoreRepository → MySQL  
**Traceability:** US-05 / UC-03 — Manage Fixed Routes / Route Management

> Postman is the HTTP entry point. A case is treated as an integration test because it verifies the API result together with the resulting MySQL state. UI cases TC-23–TC-27 belong to Frontend/E2E and are not included here.

---

## 1. Final result

| TC | Test case | Expected | Actual | Result |
|---|---|---:|---:|---|
| TC-01 | SYSTEM_ADMIN creates a valid route | 201 | 201 | PASS — tester confirmed |
| TC-02 | Duplicate route code | 409 | 409 | PASS — tester confirmed |
| TC-03 | Create route without name | 400 | 400 | PASS — tester confirmed |
| TC-04 | Update route name and description | 200 | 200 | PASS — tester confirmed |
| TC-05 | Attempt to update immutable route code | 200 | 200; code unchanged | PASS — tester confirmed |
| TC-06 | Add active store with GPS | 201 | 201 | PASS — tester confirmed |
| TC-07 | Add duplicated store to route | 409 | 409 | PASS — tester confirmed |
| TC-08 | Add inactive store | 422 | 422 | PASS — tester confirmed |
| TC-09 | Add nonexistent store | 404 | 404 | PASS — tester confirmed |
| TC-10 | Add active store without GPS | 201 | 201; warning=true | PASS — tester confirmed |
| TC-11 | Reorder with all stop IDs | 200 | 200; order 5→4→7→6 | PASS |
| TC-12 | Reorder missing one stop ID | 400 | 400; ROUTE_STOP_REORDER_INVALID | PASS |
| TC-13 | Delete middle stop and renumber | 200 | 200; stopCount=3; sequences 1,2,3 | PASS |
| TC-14 | Activate route with at least two stops | 200 | 200; isActive=true | PASS |
| TC-15 | Activate route with one stop | 400 | 400; Current: 1 | PASS |
| TC-16 | Activate route with zero stops | 400 | 400; Current: 0 | PASS |
| TC-17 | Deactivate active route | 200 | 200; isActive=false | PASS |
| TC-18 | DISPATCHER attempts to create route | 403 | 403; ACCESS_DENIED | PASS |
| TC-19 | DISPATCHER attempts to delete stop | 403 | 403; ACCESS_DENIED | PASS |
| TC-20 | LOGISTICS_MANAGER reads route list | 200 | 200; totalElements=5 | PASS |
| TC-21 | Route detail returns complete store information | 200 | 200; four complete stops | PASS |
| TC-22 | coordinatesWarningCount is correct | 200 | 200; count=1 | PASS — assertion reused TC-21 response |

**Final result: 22/22 PASS. No functional defect was found in this execution.**

TC-01 through TC-10 were confirmed as PASS by the tester from the previous machine. Their original response payloads were not retained, so this report records the verified status and reproducible requests/SQL checks without inventing historical response values.

---

## 2. Postman environment

```text
baseUrl         = http://localhost:8080
adminToken      = access token of admin
dispatcherToken = access token of dispatcher01
managerToken    = access token of manager01
routeId         = main route ID
storeId1        = ST-IT05-001 ID
storeId2        = ST-IT05-002 ID
storeId3        = ST-IT05-003 ID
storeId4        = ST-IT05-004 ID
inactiveStoreId = ST-IT05-INACTIVE ID
stopIdA         = stop for ST-IT05-001
stopIdB         = stop for ST-IT05-002
stopIdC         = stop for ST-IT05-003
stopIdD         = stop for ST-IT05-004
```

### Login accounts

| Username | Password | Role |
|---|---|---|
| `admin` | `Admin@2025` | SYSTEM_ADMIN |
| `dispatcher01` | `Dev@2025` | DISPATCHER |
| `manager01` | `Dev@2025` | LOGISTICS_MANAGER |

Login request:

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

Use the returned `data.accessToken` as a Bearer token.

---

## 3. Reusable MySQL fixture

Run this section before a fresh execution. It prepares the five stores only. Route creation remains part of TC-01.

```sql
USE elog_db;

INSERT INTO stores (
    code, name, address, contact_name, contact_phone,
    latitude, longitude, is_active
)
VALUES
('ST-IT05-001', 'Route Test Store 01',
 '10 Le Lai, Quan 1, TP.HCM', 'Anh Route 01', '0901111001',
 10.7756587, 106.7004238, TRUE),
('ST-IT05-002', 'Route Test Store 02',
 '20 Nguyen Hue, Quan 1, TP.HCM', 'Anh Route 02', '0901111002',
 10.7765000, 106.7015000, TRUE),
('ST-IT05-003', 'Route Test Store 03 No GPS',
 '30 Tran Hung Dao, Quan 1, TP.HCM', 'Anh Route 03', '0901111003',
 NULL, NULL, TRUE),
('ST-IT05-004', 'Route Test Store 04',
 '40 Hai Ba Trung, Quan 1, TP.HCM', 'Anh Route 04', '0901111004',
 10.7785000, 106.7035000, TRUE),
('ST-IT05-INACTIVE', 'Inactive Route Test Store',
 '50 Le Thanh Ton, Quan 1, TP.HCM', 'Inactive Store', '0901111005',
 10.7795000, 106.7045000, FALSE)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    address = VALUES(address),
    contact_name = VALUES(contact_name),
    contact_phone = VALUES(contact_phone),
    latitude = VALUES(latitude),
    longitude = VALUES(longitude),
    is_active = VALUES(is_active);

SELECT id, code, is_active, latitude, longitude
FROM stores
WHERE code LIKE 'ST-IT05-%'
ORDER BY code;
```

Expected fixture rules:

```text
ST-IT05-001, 002 and 004: active and have GPS
ST-IT05-003: active and both coordinates are NULL
ST-IT05-INACTIVE: inactive
```

---

## 4. Route CRUD cases

### TC-01 — Create valid route

```http
POST {{baseUrl}}/api/routes
Authorization: Bearer {{adminToken}}
Content-Type: application/json
```

```json
{
  "code": "RT-IT05-001",
  "name": "Route Integration Test 01",
  "description": "Main route for US-05 integration testing"
}
```

Expected/actual: HTTP `201`, `isActive=false`, `stopCount=0`. Tester confirmed PASS.

MySQL evidence:

```sql
SELECT r.id, r.code, r.name, r.description, r.is_active,
       COUNT(rs.id) AS stop_count
FROM routes r
LEFT JOIN route_stops rs ON rs.route_id = r.id
WHERE r.code = 'RT-IT05-001'
GROUP BY r.id, r.code, r.name, r.description, r.is_active;
```

Expected: exactly one route, `is_active=0`, `stop_count=0`.

### TC-02 — Duplicate route code

Repeat TC-01 with the same code.

Expected/actual: HTTP `409`, `error.code=ROUTE_CODE_DUPLICATE`. Tester confirmed PASS.

```sql
SELECT COUNT(*) AS route_count
FROM routes
WHERE code = 'RT-IT05-001';
```

Expected: `route_count=1`; no duplicate row was committed.

### TC-03 — Missing route name

```http
POST {{baseUrl}}/api/routes
Authorization: Bearer {{adminToken}}
Content-Type: application/json
```

```json
{
  "code": "RT-IT05-NONAME",
  "description": "Name intentionally omitted"
}
```

Expected/actual: HTTP `400`, `error.code=VALIDATION_FAILED`. Tester confirmed PASS.

```sql
SELECT COUNT(*) AS invalid_route_count
FROM routes
WHERE code = 'RT-IT05-NONAME';
```

Expected: `invalid_route_count=0`.

### TC-04 — Update route name and description

```http
PUT {{baseUrl}}/api/routes/{{routeId}}
Authorization: Bearer {{adminToken}}
Content-Type: application/json
```

```json
{
  "name": "Route Integration Test 01 Updated",
  "description": "Updated by TC-04"
}
```

Expected/actual: HTTP `200`; name and description changed. Tester confirmed PASS.

```sql
SELECT id, code, name, description
FROM routes
WHERE code = 'RT-IT05-001';
```

### TC-05 — Route code is immutable

```http
PUT {{baseUrl}}/api/routes/{{routeId}}
Authorization: Bearer {{adminToken}}
Content-Type: application/json
```

```json
{
  "code": "RT-IT05-CHANGED",
  "name": "Route Integration Test 01 Updated",
  "description": "Code must remain immutable"
}
```

Expected/actual: HTTP `200`; persisted code remains `RT-IT05-001`. Tester confirmed PASS.

```sql
SELECT id, code, name, description
FROM routes
WHERE code = 'RT-IT05-001';
```

Expected: code remains `RT-IT05-001`; no row exists with `RT-IT05-CHANGED`.

---

## 5. Route-stop validation cases

### TC-06 — Add active store with GPS

```http
POST {{baseUrl}}/api/routes/{{routeId}}/stops
Authorization: Bearer {{adminToken}}
Content-Type: application/json
```

```json
{ "storeId": {{storeId1}} }
```

Expected/actual: HTTP `201`, sequence `1`, warning `false`. Tester confirmed PASS.

```sql
SELECT rs.id AS stop_id, s.code, rs.sequence_order,
       (s.latitude IS NOT NULL AND s.longitude IS NOT NULL) AS has_coordinates
FROM route_stops rs
JOIN stores s ON s.id = rs.store_id
WHERE rs.route_id = (
    SELECT id FROM routes WHERE code = 'RT-IT05-001'
)
ORDER BY rs.sequence_order;
```

### TC-07 — Duplicate store in the same route

Repeat TC-06 with the same store.

Expected/actual: HTTP `409`, `ROUTE_STOP_DUPLICATE`. Tester confirmed PASS.

```sql
SELECT COUNT(*) AS duplicate_guard_count
FROM route_stops rs
JOIN stores s ON s.id = rs.store_id
WHERE rs.route_id = (
    SELECT id FROM routes WHERE code = 'RT-IT05-001'
)
  AND s.code = 'ST-IT05-001';
```

Expected: `duplicate_guard_count=1`.

### TC-08 — Add inactive store

```json
{ "storeId": {{inactiveStoreId}} }
```

Expected/actual: HTTP `422`, `STORE_INACTIVE`. Tester confirmed PASS.

```sql
SELECT
  (SELECT is_active FROM stores WHERE code='ST-IT05-INACTIVE') AS store_active,
  (SELECT COUNT(*)
   FROM route_stops rs
   JOIN stores s ON s.id=rs.store_id
   WHERE rs.route_id=(
       SELECT id FROM routes WHERE code='RT-IT05-001'
   )
     AND s.code='ST-IT05-INACTIVE') AS route_stop_count;
```

Expected: `store_active=0`, `route_stop_count=0`.

### TC-09 — Add nonexistent store

```json
{ "storeId": 99999 }
```

Expected/actual: HTTP `404`, `STORE_NOT_FOUND`. Tester confirmed PASS.

```sql
SELECT COUNT(*) AS nonexistent_store
FROM stores
WHERE id = 99999;
```

Expected: `0`; route stop count remains unchanged.

### TC-10 — Add active store without GPS

```json
{ "storeId": {{storeId3}} }
```

Expected/actual: HTTP `201`, sequence `2`, `coordinatesWarning=true`. Tester confirmed PASS.

```sql
SELECT rs.id AS stop_id, s.code, rs.sequence_order,
       s.latitude, s.longitude
FROM route_stops rs
JOIN stores s ON s.id=rs.store_id
WHERE rs.route_id=(
    SELECT id FROM routes WHERE code='RT-IT05-001'
)
ORDER BY rs.sequence_order;
```

Expected: `ST-IT05-003` is sequence `2`; both coordinates are `NULL`.

---

## 6. Reorder and detail cases

Before TC-11, add `ST-IT05-002` and `ST-IT05-004` through `POST /api/routes/{{routeId}}/stops`. The route must contain four stops.

### TC-11 — Reorder all stops

```http
PUT {{baseUrl}}/api/routes/{{routeId}}/stops/reorder
Authorization: Bearer {{adminToken}}
Content-Type: application/json
```

```json
{
  "orderedStopIds": [
    {{stopIdC}},
    {{stopIdA}},
    {{stopIdD}},
    {{stopIdB}}
  ]
}
```

Actual execution on 2026-06-28:

```text
HTTP 200
Route ID: 3
Order: stop 5 → stop 4 → stop 7 → stop 6
Sequences: 1 → 2 → 3 → 4
Message: Stops reordered successfully
```

```sql
SELECT rs.id AS stop_id, s.code AS store_code, rs.sequence_order
FROM route_stops rs
JOIN stores s ON s.id=rs.store_id
WHERE rs.route_id=3
ORDER BY rs.sequence_order;
```

Expected evidence: `5/ST-IT05-003/1`, `4/ST-IT05-001/2`, `7/ST-IT05-004/3`, `6/ST-IT05-002/4`.

**Verdict: PASS.**

### TC-12 — Reorder with one missing stop ID

```json
{
  "orderedStopIds": [5, 4, 7]
}
```

Actual:

```text
HTTP 400
error.code: ROUTE_STOP_REORDER_INVALID
error.message: orderedStopIds must contain exactly all stop IDs of this route
```

Run the same SQL as TC-11. Expected order remains `5 → 4 → 7 → 6` with sequences `1 → 4`.

**Verdict: PASS.**

### TC-21 — Route detail contains complete store information

```http
GET {{baseUrl}}/api/routes/3
Authorization: Bearer {{adminToken}}
```

Actual:

```text
HTTP 200
stopCount=4
Every stop contains id, routeId, sequenceOrder, store.id, storeCode,
storeName, address and hasCoordinates.
Order is 5 → 4 → 7 → 6.
```

```sql
SELECT r.id AS route_id, r.code AS route_code,
       rs.id AS stop_id, rs.sequence_order,
       s.id AS store_id, s.code AS store_code,
       s.name AS store_name, s.address,
       (s.latitude IS NOT NULL AND s.longitude IS NOT NULL) AS has_coordinates
FROM routes r
JOIN route_stops rs ON rs.route_id=r.id
JOIN stores s ON s.id=rs.store_id
WHERE r.id=3
ORDER BY rs.sequence_order;
```

**Verdict: PASS.**

### TC-22 — coordinatesWarningCount

TC-22 uses the same GET execution as TC-21 but applies a separate assertion.

Actual:

```text
HTTP 200
coordinatesWarningCount=1
ST-IT05-003 has hasCoordinates=false and coordinatesWarning=true.
```

```sql
SELECT
    COUNT(*) AS total_stops,
    SUM(CASE WHEN s.latitude IS NULL OR s.longitude IS NULL THEN 1 ELSE 0 END)
      AS missing_coordinate_count
FROM route_stops rs
JOIN stores s ON s.id=rs.store_id
WHERE rs.route_id=3;
```

Expected at execution time: `total_stops=4`, `missing_coordinate_count=1`.

**Verdict: PASS.**

---

## 7. Delete and route lifecycle cases

### TC-13 — Delete a middle stop and renumber

```http
DELETE {{baseUrl}}/api/routes/3/stops/4
Authorization: Bearer {{adminToken}}
```

Actual:

```text
HTTP 200
message: Stop removed successfully
GET route detail after deletion: stopCount=3
Remaining order: stop 5/sequence 1, stop 7/sequence 2, stop 6/sequence 3
```

```sql
SELECT rs.id AS stop_id, s.code AS store_code, rs.sequence_order
FROM route_stops rs
JOIN stores s ON s.id=rs.store_id
WHERE rs.route_id=3
ORDER BY rs.sequence_order;
```

Expected: three rows with continuous sequences `1,2,3`; stop ID `4` is absent.

**Verdict: PASS.**

### TC-14 — Activate route with at least two stops

```http
PATCH {{baseUrl}}/api/routes/3/status
Authorization: Bearer {{adminToken}}
Content-Type: application/json
```

```json
{ "isActive": true }
```

Actual: HTTP `200`, `isActive=true`, `stopCount=3`, message `Route status updated`.

```sql
SELECT r.id, r.code, r.is_active, COUNT(rs.id) AS stop_count,
       GROUP_CONCAT(rs.sequence_order ORDER BY rs.sequence_order) AS sequences
FROM routes r
LEFT JOIN route_stops rs ON rs.route_id=r.id
WHERE r.id=3
GROUP BY r.id, r.code, r.is_active;
```

Expected: `is_active=1`, `stop_count=3`, `sequences=1,2,3`.

**Verdict: PASS.**

### TC-15 — Reject activation of one-stop route

Setup:

```sql
INSERT INTO routes(code,name,description,is_active)
VALUES('RT-IT05-ONE','Route One Stop','For insufficient stop validation',FALSE)
ON DUPLICATE KEY UPDATE name=VALUES(name), description=VALUES(description), is_active=FALSE;

SET @one_route_id=(SELECT id FROM routes WHERE code='RT-IT05-ONE');
SET @store_id_1=(SELECT id FROM stores WHERE code='ST-IT05-001');
DELETE FROM route_stops WHERE route_id=@one_route_id;
INSERT INTO route_stops(route_id,store_id,sequence_order)
VALUES(@one_route_id,@store_id_1,1);
SELECT @one_route_id AS oneStopRouteId;
```

Request: `PATCH /api/routes/4/status` with `{"isActive":true}`.

Actual: HTTP `400`, `ROUTE_INSUFFICIENT_STOPS`, message contains `Current: 1`.

```sql
SELECT r.id, r.code, r.is_active, COUNT(rs.id) AS stop_count
FROM routes r
LEFT JOIN route_stops rs ON rs.route_id=r.id
WHERE r.id=4
GROUP BY r.id,r.code,r.is_active;
```

Expected: `is_active=0`, `stop_count=1`.

**Verdict: PASS.**

### TC-16 — Reject activation of empty route

Setup:

```sql
INSERT INTO routes(code,name,description,is_active)
VALUES('RT-IT05-EMPTY','Empty Route','Route with zero stops for validation test',FALSE)
ON DUPLICATE KEY UPDATE name=VALUES(name), description=VALUES(description), is_active=FALSE;

SET @empty_route_id=(SELECT id FROM routes WHERE code='RT-IT05-EMPTY');
DELETE FROM route_stops WHERE route_id=@empty_route_id;
SELECT @empty_route_id AS emptyRouteId;
```

Request: `PATCH /api/routes/5/status` with `{"isActive":true}`.

Actual: HTTP `400`, `ROUTE_INSUFFICIENT_STOPS`, message contains `Current: 0`.

```sql
SELECT r.id, r.code, r.is_active, COUNT(rs.id) AS stop_count
FROM routes r
LEFT JOIN route_stops rs ON rs.route_id=r.id
WHERE r.id=5
GROUP BY r.id,r.code,r.is_active;
```

Expected: `is_active=0`, `stop_count=0`.

**Verdict: PASS.**

### TC-17 — Deactivate active route

Request: `PATCH /api/routes/3/status` with `{"isActive":false}`.

Actual: HTTP `200`, `isActive=false`, `stopCount=3`, message `Route status updated`.

```sql
SELECT r.id, r.code, r.is_active, COUNT(rs.id) AS stop_count
FROM routes r
LEFT JOIN route_stops rs ON rs.route_id=r.id
WHERE r.id=3
GROUP BY r.id,r.code,r.is_active;
```

Expected: `is_active=0`; all three stops remain.

**Verdict: PASS.**

---

## 8. Authorization and read-access cases

### TC-18 — DISPATCHER cannot create route

Login as `dispatcher01`, then send:

```http
POST {{baseUrl}}/api/routes
Authorization: Bearer {{dispatcherToken}}
Content-Type: application/json
```

```json
{
  "code": "RT-FORBIDDEN",
  "name": "Should Not Create"
}
```

Actual: HTTP `403`, `ACCESS_DENIED`, message `You do not have permission to perform this action`.

```sql
SELECT COUNT(*) AS forbidden_route_count
FROM routes
WHERE code='RT-FORBIDDEN';
```

Expected: `0`.

**Verdict: PASS.**

### TC-19 — DISPATCHER cannot delete route stop

```http
DELETE {{baseUrl}}/api/routes/3/stops/5
Authorization: Bearer {{dispatcherToken}}
```

Actual: HTTP `403`, `ACCESS_DENIED`.

```sql
SELECT rs.id AS stop_id, s.code AS store_code, rs.sequence_order
FROM route_stops rs
JOIN stores s ON s.id=rs.store_id
WHERE rs.route_id=3
ORDER BY rs.sequence_order;
```

Expected: stops `5,7,6` remain with sequences `1,2,3`.

**Verdict: PASS.**

### TC-20 — LOGISTICS_MANAGER can read routes

Login as `manager01`, then send:

```http
GET {{baseUrl}}/api/routes?page=0&size=20&sort=createdAt,desc
Authorization: Bearer {{managerToken}}
```

Actual:

```text
HTTP 200
pagination.totalElements=5
RT-IT05-EMPTY: 0 stops
RT-IT05-ONE: 1 stop
RT-IT05-001: 3 stops
RT-001: 2 stops
RT-002: 1 stop
```

```sql
SELECT r.id, r.code, r.is_active, COUNT(rs.id) AS stop_count
FROM routes r
LEFT JOIN route_stops rs ON rs.route_id=r.id
GROUP BY r.id,r.code,r.is_active
ORDER BY r.created_at DESC;

SELECT COUNT(*) AS total_routes FROM routes;
```

Expected: `total_routes=5`, matching API pagination.

**Verdict: PASS.**

---

## 9. Recommended execution order

```text
Prepare five stores
TC-01 → TC-02 → TC-03 → TC-04 → TC-05
TC-06 → TC-07 → TC-08 → TC-09 → TC-10
Add stores 002 and 004 to main route
TC-11 → TC-12 → TC-21 → TC-22 → TC-13
TC-14 → TC-17
TC-15 and TC-16 use independent routes
TC-18 → TC-19 → TC-20
Capture API response and MySQL Result Grid for every case
Run cleanup
```

---

## 10. Cleanup

Run in this order after screenshots/evidence have been saved:

```sql
USE elog_db;

DELETE rs
FROM route_stops rs
JOIN routes r ON r.id=rs.route_id
WHERE r.code IN ('RT-IT05-001','RT-IT05-ONE','RT-IT05-EMPTY');

DELETE FROM routes
WHERE code IN (
    'RT-IT05-001',
    'RT-IT05-ONE',
    'RT-IT05-EMPTY',
    'RT-IT05-NONAME',
    'RT-IT05-CHANGED',
    'RT-FORBIDDEN'
);

DELETE FROM stores
WHERE code IN (
    'ST-IT05-001',
    'ST-IT05-002',
    'ST-IT05-003',
    'ST-IT05-004',
    'ST-IT05-INACTIVE'
);
```

Cleanup verification:

```sql
SELECT COUNT(*) AS remaining_test_routes
FROM routes
WHERE code LIKE 'RT-IT05-%' OR code='RT-FORBIDDEN';

SELECT COUNT(*) AS remaining_test_stores
FROM stores
WHERE code LIKE 'ST-IT05-%';
```

Expected: both counts are `0`.

---

## 11. Evidence checklist

For each case, keep:

1. Postman request URL, method, Authorization and body.
2. Postman HTTP status and response JSON.
3. MySQL query and Result Grid proving the persisted state or unchanged state.
4. Final verdict (`PASS`/`FAIL`) and defect ID if failed.

**Execution conclusion:** all 22 backend API-driven integration cases passed. TC-22 reused the TC-21 GET response with a separate warning assertion; this is explicitly documented rather than represented as a second network call.
