-- ============================================================
-- ELog Delivery Management System
-- Flyway Migration: V1__init_schema.sql
-- INC-1 Baseline Schema (Sprint 1 — US-01 skeleton)
-- ============================================================
-- Naming conventions:
--   · All PKs: id BIGINT AUTO_INCREMENT PRIMARY KEY
--   · All FKs: {table_singular}_id
--   · Timestamps: created_at, updated_at (auto-managed)
--   · Soft-delete where noted: deleted_at DATETIME NULL
-- ============================================================

-- ────────────────────────────────────────────────────────────
-- 1. ROLES
-- Values: DISPATCHER | WAREHOUSE_STAFF | DRIVER | LOGISTICS_MANAGER | ADMIN
-- ────────────────────────────────────────────────────────────
CREATE TABLE roles (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    name       VARCHAR(50)   NOT NULL,
    CONSTRAINT pk_roles PRIMARY KEY (id),
    CONSTRAINT uq_roles_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ────────────────────────────────────────────────────────────
-- 2. USERS
-- ────────────────────────────────────────────────────────────
CREATE TABLE users (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    username      VARCHAR(50)   NOT NULL,
    password_hash VARCHAR(255)  NOT NULL,              -- BCrypt hash
    full_name     VARCHAR(100)  NOT NULL,
    role_id       BIGINT        NOT NULL,
    is_active     BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ────────────────────────────────────────────────────────────
-- 3. STORES
-- Must exist before route_stops (FK dependency)
-- ────────────────────────────────────────────────────────────
CREATE TABLE stores (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    code        VARCHAR(20)     NOT NULL,
    name        VARCHAR(100)    NOT NULL,
    address     VARCHAR(255)    NULL,
    latitude    DECIMAL(10, 7)  NULL,
    longitude   DECIMAL(10, 7)  NULL,
    is_active   BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_stores PRIMARY KEY (id),
    CONSTRAINT uq_stores_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ────────────────────────────────────────────────────────────
-- 4. ROUTES
-- BR-05: read-only in operational UI; managed by admin only
-- ────────────────────────────────────────────────────────────
CREATE TABLE routes (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    code        VARCHAR(20)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_routes PRIMARY KEY (id),
    CONSTRAINT uq_routes_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ────────────────────────────────────────────────────────────
-- 5. ROUTE_STOPS
-- sequence_no: 1 = first delivery stop (unloaded first = loaded last = LIFO)
-- BR-04: LIFO → last sequence_no loaded first into vehicle
-- ────────────────────────────────────────────────────────────
CREATE TABLE route_stops (
    id           BIGINT  NOT NULL AUTO_INCREMENT,
    route_id     BIGINT  NOT NULL,
    store_id     BIGINT  NOT NULL,
    sequence_no  INT     NOT NULL,    -- 1-based; lower = closer to first delivery
    CONSTRAINT pk_route_stops PRIMARY KEY (id),
    CONSTRAINT uq_route_stops_seq UNIQUE (route_id, sequence_no),
    CONSTRAINT fk_route_stops_route FOREIGN KEY (route_id) REFERENCES routes(id),
    CONSTRAINT fk_route_stops_store FOREIGN KEY (store_id) REFERENCES stores(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Index: lookup stops by route (used frequently during trip planning)
CREATE INDEX idx_route_stops_route ON route_stops(route_id);

-- ────────────────────────────────────────────────────────────
-- 6. VEHICLES
-- BR-03: both capacity_m3 AND capacity_kg must pass validation
-- ────────────────────────────────────────────────────────────
CREATE TABLE vehicles (
    id             BIGINT          NOT NULL AUTO_INCREMENT,
    plate_no       VARCHAR(20)     NOT NULL,
    capacity_m3    DECIMAL(8, 3)   NOT NULL,   -- cubic metres
    capacity_kg    DECIMAL(10, 3)  NOT NULL,   -- kilograms
    is_active      BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_vehicles PRIMARY KEY (id),
    CONSTRAINT uq_vehicles_plate UNIQUE (plate_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ────────────────────────────────────────────────────────────
-- 7. PRODUCTS
-- Dimensions stored for volume auto-calculation
-- volume_m3 = length_m * width_m * height_m (computed + stored)
-- ────────────────────────────────────────────────────────────
CREATE TABLE products (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    code        VARCHAR(50)     NOT NULL,
    name        VARCHAR(150)    NOT NULL,
    length_m    DECIMAL(8, 4)   NULL,          -- metres
    width_m     DECIMAL(8, 4)   NULL,
    height_m    DECIMAL(8, 4)   NULL,
    volume_m3   DECIMAL(10, 6)  NULL,          -- pre-computed; auto-filled by app
    weight_kg   DECIMAL(10, 3)  NULL,          -- per unit
    is_active   BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_products PRIMARY KEY (id),
    CONSTRAINT uq_products_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ────────────────────────────────────────────────────────────
-- 8. EXCEL_IMPORTS
-- Tracks each Excel upload batch (BR-01: orders via Excel only)
-- ────────────────────────────────────────────────────────────
CREATE TABLE excel_imports (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    original_filename VARCHAR(255) NOT NULL,
    import_date     DATE          NOT NULL,
    imported_by     BIGINT        NOT NULL,    -- FK users
    total_rows      INT           NOT NULL DEFAULT 0,
    valid_rows      INT           NOT NULL DEFAULT 0,
    invalid_rows    INT           NOT NULL DEFAULT 0,
    status          ENUM('PROCESSING','COMPLETED','FAILED') NOT NULL DEFAULT 'PROCESSING',
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_excel_imports PRIMARY KEY (id),
    CONSTRAINT fk_excel_imports_user FOREIGN KEY (imported_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ────────────────────────────────────────────────────────────
-- 9. ORDERS
-- BR-01: created only via Excel import (no manual creation)
-- BR-02: each order maps to exactly one store → one fixed route
-- ────────────────────────────────────────────────────────────
CREATE TABLE orders (
    id               BIGINT         NOT NULL AUTO_INCREMENT,
    excel_import_id  BIGINT         NOT NULL,
    store_id         BIGINT         NOT NULL,
    product_id       BIGINT         NOT NULL,
    quantity         INT            NOT NULL,
    volume_m3        DECIMAL(10, 6) NOT NULL,  -- quantity * product.volume_m3
    weight_kg        DECIMAL(10, 3) NOT NULL,  -- quantity * product.weight_kg
    import_date      DATE           NOT NULL,
    status           ENUM('PENDING','ASSIGNED','DISPATCHED') NOT NULL DEFAULT 'PENDING',
    created_at       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_orders PRIMARY KEY (id),
    CONSTRAINT fk_orders_import  FOREIGN KEY (excel_import_id) REFERENCES excel_imports(id),
    CONSTRAINT fk_orders_store   FOREIGN KEY (store_id)        REFERENCES stores(id),
    CONSTRAINT fk_orders_product FOREIGN KEY (product_id)      REFERENCES products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_orders_store      ON orders(store_id);
CREATE INDEX idx_orders_importdate ON orders(import_date);

-- ────────────────────────────────────────────────────────────
-- 10. TRIPS
-- Trip lifecycle: PLANNED → VALIDATED → DISPATCHED → IN_PROGRESS → COMPLETED
-- DC-01: only forward transitions allowed; no reopening Completed
-- ────────────────────────────────────────────────────────────
CREATE TABLE trips (
    id               BIGINT         NOT NULL AUTO_INCREMENT,
    route_id         BIGINT         NOT NULL,
    vehicle_id       BIGINT         NOT NULL,
    driver_id        BIGINT         NOT NULL,
    trip_date        DATE           NOT NULL,
    status           ENUM('PLANNED','VALIDATED','DISPATCHED','IN_PROGRESS','COMPLETED')
                                    NOT NULL DEFAULT 'PLANNED',
    total_volume_m3  DECIMAL(10, 3) NOT NULL DEFAULT 0.000,
    total_weight_kg  DECIMAL(10, 3) NOT NULL DEFAULT 0.000,
    created_by       BIGINT         NOT NULL,
    created_at       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_trips PRIMARY KEY (id),
    CONSTRAINT fk_trips_route    FOREIGN KEY (route_id)   REFERENCES routes(id),
    CONSTRAINT fk_trips_vehicle  FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),
    CONSTRAINT fk_trips_driver   FOREIGN KEY (driver_id)  REFERENCES users(id),
    CONSTRAINT fk_trips_creator  FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_trips_date   ON trips(trip_date);
CREATE INDEX idx_trips_status ON trips(status);

-- ────────────────────────────────────────────────────────────
-- 11. TRIP_STOPS
-- BR-06: only stops with orders for the trip_date are included
-- ────────────────────────────────────────────────────────────
CREATE TABLE trip_stops (
    id                  BIGINT    NOT NULL AUTO_INCREMENT,
    trip_id             BIGINT    NOT NULL,
    route_stop_id       BIGINT    NOT NULL,
    sequence_no         INT       NOT NULL,
    estimated_arrival   DATETIME  NULL,
    actual_arrival      DATETIME  NULL,
    status              ENUM('PENDING','ARRIVED','COMPLETED','SKIPPED') NOT NULL DEFAULT 'PENDING',
    CONSTRAINT pk_trip_stops PRIMARY KEY (id),
    CONSTRAINT fk_trip_stops_trip       FOREIGN KEY (trip_id)       REFERENCES trips(id),
    CONSTRAINT fk_trip_stops_routestop  FOREIGN KEY (route_stop_id) REFERENCES route_stops(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- NFR: Dashboard refresh ≤ 5s — index for live monitoring queries
CREATE INDEX idx_trip_stops_trip_status ON trip_stops(trip_id, status);

-- ────────────────────────────────────────────────────────────
-- 12. LOADING_MANIFEST_ITEMS
-- BR-04: load_order = LIFO sequence (1 = first loaded = last stop)
-- ────────────────────────────────────────────────────────────
CREATE TABLE loading_manifest_items (
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    trip_id         BIGINT         NOT NULL,
    trip_stop_id    BIGINT         NOT NULL,
    order_id        BIGINT         NOT NULL,
    load_order      INT            NOT NULL,    -- 1 = first INTO vehicle (last delivery stop)
    product_name    VARCHAR(150)   NOT NULL,    -- denormalized for manifest snapshot
    quantity        INT            NOT NULL,
    volume_m3       DECIMAL(10, 6) NOT NULL,
    weight_kg       DECIMAL(10, 3) NOT NULL,
    CONSTRAINT pk_manifest_items PRIMARY KEY (id),
    CONSTRAINT fk_manifest_trip      FOREIGN KEY (trip_id)      REFERENCES trips(id),
    CONSTRAINT fk_manifest_tripstop  FOREIGN KEY (trip_stop_id) REFERENCES trip_stops(id),
    CONSTRAINT fk_manifest_order     FOREIGN KEY (order_id)     REFERENCES orders(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_manifest_trip ON loading_manifest_items(trip_id, load_order);

-- ────────────────────────────────────────────────────────────
-- 13. DELIVERY_RECORDS
-- BR-10: REJECTED status triggers an exception record
-- ────────────────────────────────────────────────────────────
CREATE TABLE delivery_records (
    id            BIGINT     NOT NULL AUTO_INCREMENT,
    trip_stop_id  BIGINT     NOT NULL,
    driver_id     BIGINT     NOT NULL,
    status        ENUM('DELIVERED','REJECTED','PARTIAL') NOT NULL,
    notes         TEXT       NULL,
    recorded_at   DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_delivery_records PRIMARY KEY (id),
    CONSTRAINT uq_delivery_records_stop UNIQUE (trip_stop_id),    -- 1 record per stop
    CONSTRAINT fk_delivery_tripstop FOREIGN KEY (trip_stop_id) REFERENCES trip_stops(id),
    CONSTRAINT fk_delivery_driver   FOREIGN KEY (driver_id)    REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ────────────────────────────────────────────────────────────
-- 14. EPOD (Electronic Proof of Delivery)
-- BR-11: signature + ≥1 cargo image required to complete a stop
-- Deferred to INC-2 (Android app) but table scaffolded now
-- ────────────────────────────────────────────────────────────
CREATE TABLE epod (
    id                    BIGINT        NOT NULL AUTO_INCREMENT,
    delivery_record_id    BIGINT        NOT NULL,
    signature_image_url   VARCHAR(500)  NOT NULL,
    cargo_image_urls      JSON          NOT NULL,   -- array; validated: length ≥ 1
    captured_at           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_epod PRIMARY KEY (id),
    CONSTRAINT uq_epod_delivery UNIQUE (delivery_record_id),
    CONSTRAINT fk_epod_delivery FOREIGN KEY (delivery_record_id) REFERENCES delivery_records(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ────────────────────────────────────────────────────────────
-- 15. EXCEPTIONS
-- BR-09: TIME exception when ETA breached beyond threshold
-- BR-10: DELIVERY exception on rejection
-- ────────────────────────────────────────────────────────────
CREATE TABLE exceptions (
    id             BIGINT     NOT NULL AUTO_INCREMENT,
    trip_id        BIGINT     NOT NULL,
    trip_stop_id   BIGINT     NULL,                 -- NULL = trip-level exception
    type           ENUM('TIME','DELIVERY') NOT NULL,
    description    TEXT       NULL,
    flagged_at     DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at    DATETIME   NULL,
    CONSTRAINT pk_exceptions PRIMARY KEY (id),
    CONSTRAINT fk_exception_trip     FOREIGN KEY (trip_id)      REFERENCES trips(id),
    CONSTRAINT fk_exception_tripstop FOREIGN KEY (trip_stop_id) REFERENCES trip_stops(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- NFR: Dashboard refresh ≤ 5s
CREATE INDEX idx_exceptions_trip ON exceptions(trip_id);
