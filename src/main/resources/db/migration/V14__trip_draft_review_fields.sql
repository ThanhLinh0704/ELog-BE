-- V14__trip_draft_review_fields.sql
-- US-11 Trip Draft Review, Stop Filtering & ETA

-- ────────────────────────────────────────────────────────────
-- 1. Create system_config table (needed for warehouse GPS + avg speed)
-- ────────────────────────────────────────────────────────────
CREATE TABLE system_config (
    id           BIGINT        AUTO_INCREMENT PRIMARY KEY,
    config_key   VARCHAR(100)  NOT NULL,
    config_value VARCHAR(500)  NOT NULL,
    description  VARCHAR(255)  NULL,
    CONSTRAINT uq_config_key UNIQUE (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ────────────────────────────────────────────────────────────
-- 2. Add avg_service_time_min to route_stops (needed for ETA calc)
-- ────────────────────────────────────────────────────────────
ALTER TABLE route_stops
    ADD COLUMN avg_service_time_min INT NOT NULL DEFAULT 15;

-- ────────────────────────────────────────────────────────────
-- 3. Add review fields to trip_drafts
-- ────────────────────────────────────────────────────────────
ALTER TABLE trip_drafts
    ADD COLUMN planned_departure_time TIME         NULL,
    ADD COLUMN confirmed_at           DATETIME     NULL,
    ADD COLUMN confirmed_by           BIGINT       NULL,
    ADD CONSTRAINT fk_td_confirmed_by FOREIGN KEY (confirmed_by) REFERENCES users(id);

-- ────────────────────────────────────────────────────────────
-- 4. Add ETA + override fields to trip_draft_stops
-- ────────────────────────────────────────────────────────────
ALTER TABLE trip_draft_stops
    ADD COLUMN planned_eta    DATETIME     NULL,
    ADD COLUMN override_note  VARCHAR(255) NULL;

-- ────────────────────────────────────────────────────────────
-- 5. Seed system config
-- ────────────────────────────────────────────────────────────
INSERT INTO system_config (config_key, config_value, description) VALUES
    ('WAREHOUSE_LAT', '10.7769',  'Warehouse GPS latitude — origin point for ETA calculation'),
    ('WAREHOUSE_LNG', '106.7009', 'Warehouse GPS longitude'),
    ('AVG_SPEED_KMH', '40',      'Average delivery speed in km/h for ETA calculation');
