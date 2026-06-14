# ELog — Data Model Reference
> Load via `@docs/data-model.md` when designing schema or JPA entities.
> Key concepts defined here — do NOT re-explain in other files.

---

## Key Data Concepts (Canonical Definitions)

| Term | Definition |
|------|-----------|
| **Route** | Predefined ordered sequence of Stops. Read-only ref data. |
| **Stop** | One Store on one Route with a fixed sequence position. |
| **Trip** | One Vehicle dispatched on one Route for one date. Contains ordered stop list. |
| **Loading Manifest** | Flat LIFO-ordered list of packages generated from a Trip's stops. |
| **e-POD** | Electronic Proof of Delivery = digital signature + ≥1 cargo photo. |
| **Exception** | Time exception (ETA breach) or Delivery exception (rejection). |

---

## Entity Overview

```
User ──────────────────── Role (M:1)

Route ─────────────────── RouteStop (1:N)
                                │
                         Store (M:1 via stop)

Trip ──────────────────── Route (M:1)
 │                     ── Vehicle (M:1)
 │                     ── TripStop (1:N)
 │                            │
 │                     ── Stop (M:1)
 │
 ├── LoadingManifest (1:1)
 │       └── ManifestItem (1:N)
 │
 └── DeliveryRecord (1:N per stop)
         ├── ePOD (1:1)
         └── Exception (0..1)

Order ─────────────────── Store (M:1)
  │                    ── Route (M:1, derived via store)
  └── (groups into TripStop)

Vehicle ────────────────── (capacityM3, capacityKg)
```

---

## Core Tables — Field Notes

### `users`
```sql
id, username, password_hash, role_id, full_name,
is_active BOOLEAN DEFAULT TRUE,
created_at, updated_at
```

### `roles`
```sql
id, name  -- DISPATCHER | WAREHOUSE_STAFF | DRIVER | LOGISTICS_MANAGER | ADMIN
```

### `routes`
```sql
id, code VARCHAR(20) UNIQUE NOT NULL, name,
is_active BOOLEAN DEFAULT TRUE
-- BR-05: no edit via operational UI
```

### `route_stops`
```sql
id, route_id FK, store_id FK,
sequence_no INT NOT NULL,    -- 1 = first delivery, N = last
-- LIFO: last sequence_no is loaded first into vehicle
UNIQUE(route_id, sequence_no)
```

### `stores`
```sql
id, code VARCHAR(20) UNIQUE NOT NULL, name,
address, latitude DECIMAL(10,7), longitude DECIMAL(10,7)
```

### `vehicles`
```sql
id, plate_no VARCHAR(20) UNIQUE NOT NULL,
capacity_m3 DECIMAL(8,3) NOT NULL,
capacity_kg DECIMAL(10,3) NOT NULL,
is_active BOOLEAN DEFAULT TRUE
```

### `orders`
```sql
id, excel_import_id FK, store_id FK,
product_id FK, quantity INT,
volume_m3 DECIMAL(8,3), weight_kg DECIMAL(10,3),
import_date DATE, status ENUM('PENDING','ASSIGNED','DISPATCHED')
```

### `trips`
```sql
id, route_id FK, vehicle_id FK, driver_id FK,
trip_date DATE, status ENUM('PLANNED','VALIDATED','DISPATCHED','IN_PROGRESS','COMPLETED'),
total_volume_m3 DECIMAL(8,3), total_weight_kg DECIMAL(10,3),
created_by FK(users), created_at, updated_at
```

### `trip_stops`
```sql
id, trip_id FK, stop_id FK, sequence_no INT,
estimated_arrival DATETIME, actual_arrival DATETIME,
status ENUM('PENDING','ARRIVED','COMPLETED','SKIPPED')
```

### `loading_manifest_items`
```sql
id, trip_id FK, trip_stop_id FK,
load_order INT NOT NULL,    -- 1 = first INTO vehicle (last stop), N = last in
order_id FK,
product_name, quantity, volume_m3, weight_kg
```

### `delivery_records`
```sql
id, trip_stop_id FK, driver_id FK,
status ENUM('DELIVERED','REJECTED','PARTIAL'),
notes TEXT, recorded_at DATETIME
```

### `epod`
```sql
id, delivery_record_id FK UNIQUE,
signature_image_url VARCHAR(500) NOT NULL,
cargo_image_urls JSON NOT NULL,    -- array, min 1 element
captured_at DATETIME
```

### `exceptions`
```sql
id, trip_id FK, trip_stop_id FK NULL,
type ENUM('TIME','DELIVERY'),
description TEXT, flagged_at DATETIME, resolved_at DATETIME NULL
```

---

## Relationships Summary

```
User  N──1  Role
Route 1──N  RouteStop N──1 Store
Trip  N──1  Route
Trip  N──1  Vehicle
Trip  N──1  User (driver)
Trip  1──N  TripStop
TripStop N──1 RouteStop
Trip  1──N  LoadingManifestItem
TripStop 1──1 DeliveryRecord
DeliveryRecord 1──1 ePOD
DeliveryRecord 1──1 Exception (optional)
Order N──1  Store
```

---

## NFR Constraints Affecting Schema

- Dashboard refresh ≤ 5s → index `trip_stops(trip_id, status)`, `exceptions(trip_id)`
- Trip records retained ≥ 2 years → no hard delete on trips; use archive flag
- Driver sees own trips only → always filter `WHERE driver_id = :currentUserId`
