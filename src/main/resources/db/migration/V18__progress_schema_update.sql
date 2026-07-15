-- V18__progress_schema_update.sql
-- US-17 Dashboard Monitoring — TASK-01 Schema Update

INSERT IGNORE INTO system_config (config_key, config_value, description)
VALUES
    ('ETA_THRESHOLD_MINUTES',       '15', 'Minutes past planned ETA before flagging TIME_EXCEPTION (BR-09)'),
    ('GPS_STALENESS_WINDOW_MINUTES', '5',  'Minutes without GPS update before marking position stale (BV-11)');

-- 3. Index hỗ trợ dashboard query: "all active trips today"
CREATE INDEX idx_trips_status_date ON trips (status, delivery_date);

-- 4. Index hỗ trợ scheduled job: tìm PENDING stops đã quá ETA
CREATE INDEX idx_tripstops_pending_eta ON trip_stops (status, planned_eta);

-- 5. Tạo bảng delivery_exceptions
CREATE TABLE delivery_exceptions (
    exception_id     BIGINT   NOT NULL AUTO_INCREMENT,
    trip_stop_id     BIGINT   NOT NULL,
    exception_type   ENUM('TIME_EXCEPTION', 'DELIVERY_REJECTION') NOT NULL,
    reported_by      BIGINT   NOT NULL,
    description      TEXT     NULL,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at      DATETIME NULL,
    resolved_by      BIGINT   NULL,
    resolution_notes TEXT     NULL,
    CONSTRAINT pk_delivery_exceptions     PRIMARY KEY (exception_id),
    CONSTRAINT fk_de_tripstop            FOREIGN KEY (trip_stop_id) REFERENCES trip_stops(trip_stop_id),
    CONSTRAINT fk_de_reporter            FOREIGN KEY (reported_by)  REFERENCES users(id),
    CONSTRAINT fk_de_resolver            FOREIGN KEY (resolved_by)  REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Index hỗ trợ các query 
CREATE INDEX idx_de_tripstop      ON delivery_exceptions (trip_stop_id);
CREATE INDEX idx_de_type_resolved ON delivery_exceptions (exception_type, resolved_at);
CREATE INDEX idx_de_created       ON delivery_exceptions (created_at DESC);


