-- V17__trip_tripstop_schema.sql
-- US-15 TASK-01: Trip & TripStop schema for Vehicle Assignment & Dispatch

-- 1. Drop existing foreign keys that reference the old trips/trip_stops tables (from V1 skeleton)
ALTER TABLE exceptions DROP FOREIGN KEY fk_exception_trip;
ALTER TABLE exceptions DROP FOREIGN KEY fk_exception_tripstop;
ALTER TABLE delivery_records DROP FOREIGN KEY fk_delivery_tripstop;
ALTER TABLE loading_manifest_items DROP FOREIGN KEY fk_manifest_trip;
ALTER TABLE loading_manifest_items DROP FOREIGN KEY fk_manifest_tripstop;

-- 2. Drop old tables
DROP TABLE IF EXISTS trip_stops;
DROP TABLE IF EXISTS trips;

-- 3. Re-create trips table with US-15/16 schema
CREATE TABLE trips (
    trip_id                BIGINT        AUTO_INCREMENT PRIMARY KEY,
    trip_draft_id          BIGINT        NOT NULL,
    route_id               BIGINT        NOT NULL,
    vehicle_id             BIGINT        NOT NULL,
    driver_id              BIGINT        NOT NULL,
    delivery_date          DATE          NOT NULL,
    status                 ENUM('VALIDATED','DISPATCHED','IN_PROGRESS','COMPLETED')
                                         NOT NULL DEFAULT 'VALIDATED',
    total_weight_kg        DECIMAL(10,3) NOT NULL,
    total_volume_m3        DECIMAL(10,6) NOT NULL,
    planned_departure_time TIME          NULL,
    actual_departure_time  DATETIME      NULL,
    locked_at              DATETIME      NULL,
    locked_by              BIGINT        NULL,
    completed_at           DATETIME      NULL,
    created_by             BIGINT        NOT NULL,
    created_at             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_trip_draft     FOREIGN KEY (trip_draft_id) REFERENCES trip_drafts(id),
    CONSTRAINT fk_trip_route     FOREIGN KEY (route_id)      REFERENCES routes(id),
    CONSTRAINT fk_trip_vehicle   FOREIGN KEY (vehicle_id)    REFERENCES vehicles(id),
    CONSTRAINT fk_trip_driver    FOREIGN KEY (driver_id)     REFERENCES users(id),
    CONSTRAINT fk_trip_locked_by FOREIGN KEY (locked_by)     REFERENCES users(id),
    CONSTRAINT fk_trip_created   FOREIGN KEY (created_by)    REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Re-create trip_stops table with US-15/16 schema
CREATE TABLE trip_stops (
    trip_stop_id       BIGINT        AUTO_INCREMENT PRIMARY KEY,
    trip_id            BIGINT        NOT NULL,
    route_stop_id      BIGINT        NOT NULL,
    trip_draft_stop_id BIGINT        NOT NULL,
    sequence_order     INT           NOT NULL,
    planned_eta        DATETIME      NULL,
    actual_arrival_time DATETIME     NULL,
    status             ENUM('PENDING','IN_PROGRESS','COMPLETED','EXCEPTION')
                                     NOT NULL DEFAULT 'PENDING',
    stop_weight_kg     DECIMAL(10,3) NOT NULL,
    stop_volume_m3     DECIMAL(10,6) NOT NULL,
    notes              TEXT          NULL,
    CONSTRAINT fk_ts_trip      FOREIGN KEY (trip_id)            REFERENCES trips(trip_id),
    CONSTRAINT fk_ts_routestop FOREIGN KEY (route_stop_id)      REFERENCES route_stops(id),
    CONSTRAINT fk_ts_draftstop FOREIGN KEY (trip_draft_stop_id) REFERENCES trip_draft_stops(id),
    CONSTRAINT uq_ts_trip_stop UNIQUE (trip_id, route_stop_id),
    CONSTRAINT uq_ts_trip_seq  UNIQUE (trip_id, sequence_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Re-create foreign keys from dependent tables referencing new primary keys
ALTER TABLE loading_manifest_items ADD CONSTRAINT fk_manifest_trip FOREIGN KEY (trip_id) REFERENCES trips(trip_id);
ALTER TABLE loading_manifest_items ADD CONSTRAINT fk_manifest_tripstop FOREIGN KEY (trip_stop_id) REFERENCES trip_stops(trip_stop_id);
ALTER TABLE delivery_records ADD CONSTRAINT fk_delivery_tripstop FOREIGN KEY (trip_stop_id) REFERENCES trip_stops(trip_stop_id);
ALTER TABLE exceptions ADD CONSTRAINT fk_exception_trip FOREIGN KEY (trip_id) REFERENCES trips(trip_id);
ALTER TABLE exceptions ADD CONSTRAINT fk_exception_tripstop FOREIGN KEY (trip_stop_id) REFERENCES trip_stops(trip_stop_id);

-- Indexes
CREATE INDEX idx_trips_date_status   ON trips (delivery_date, status);
CREATE INDEX idx_trips_driver_date   ON trips (driver_id, delivery_date);
CREATE INDEX idx_trips_draft         ON trips (trip_draft_id);
CREATE INDEX idx_tripstops_trip      ON trip_stops (trip_id, sequence_order);
CREATE INDEX idx_tripstops_status    ON trip_stops (trip_id, status);
