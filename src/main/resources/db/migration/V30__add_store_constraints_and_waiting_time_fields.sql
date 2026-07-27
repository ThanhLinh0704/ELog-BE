ALTER TABLE stores
    ADD COLUMN time_window_start TIME NULL,
    ADD COLUMN time_window_end TIME NULL,
    ADD COLUMN restricted_vehicle_types VARCHAR(255) NULL;

ALTER TABLE trip_draft_stops
    ADD COLUMN planned_waiting_time_min INT NULL,
    ADD COLUMN violation_code VARCHAR(50) NULL;

ALTER TABLE trip_stops
    ADD COLUMN planned_waiting_time_min INT NULL,
    ADD COLUMN violation_code VARCHAR(50) NULL;
