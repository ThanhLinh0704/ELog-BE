-- V59__add_trip_cancellation.sql
-- Dispatcher "Huỷ chuyến" feature + auto stale-trip warning config.
-- See filemd/BE_FIX_CANCEL_TRIP_AND_STALE_WARNING_2026-08-23.md.

-- 1. Allow CANCELLED as a Trip status (DISPATCHED -> CANCELLED transition, before driver starts).
ALTER TABLE trips
    MODIFY COLUMN status ENUM('VALIDATED','DISPATCHED','IN_PROGRESS','COMPLETED','CANCELLED')
        NOT NULL DEFAULT 'VALIDATED';

-- 2. Cancellation audit columns, mirroring locked_at/locked_by.
ALTER TABLE trips
    ADD COLUMN cancelled_at   DATETIME     NULL,
    ADD COLUMN cancelled_by   BIGINT       NULL,
    ADD COLUMN cancel_reason  VARCHAR(255) NULL,
    ADD CONSTRAINT fk_trip_cancelled_by FOREIGN KEY (cancelled_by) REFERENCES users(id);

-- 3. Threshold (days) for the stale-trip auto-detection job.
INSERT INTO system_config (config_key, config_value, description) VALUES
    ('TRIP_STALE_THRESHOLD_DAYS', '3',
     'So ngay qua han deliveryDate ma chuyen DISPATCHED van chua bat dau thi bi flag can xu ly');
