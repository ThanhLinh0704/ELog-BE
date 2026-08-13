-- V36: Add Goong route distance, duration, and polyline fields to trip_drafts, trip_draft_stops, trips, trip_stops

ALTER TABLE trip_drafts
    ADD COLUMN total_distance_km DECIMAL(10,2) NULL COMMENT 'Tong quang duong du kien (km) tu Goong API',
    ADD COLUMN route_polyline TEXT NULL COMMENT 'Encoded polyline cho toan bo tuyen duong';

ALTER TABLE trip_draft_stops
    ADD COLUMN distance_from_prev_km DECIMAL(10,2) NULL COMMENT 'Khoang cach tu diem dung truoc (km)',
    ADD COLUMN travel_time_from_prev_min INT NULL COMMENT 'Thoi gian di chuyen tu diem dung truoc (phut)';

ALTER TABLE trips
    ADD COLUMN total_distance_km DECIMAL(10,2) NULL COMMENT 'Tong quang duong du kien (km) tu Goong API',
    ADD COLUMN route_polyline TEXT NULL COMMENT 'Encoded polyline cho toan bo tuyen duong';

ALTER TABLE trip_stops
    ADD COLUMN distance_from_prev_km DECIMAL(10,2) NULL COMMENT 'Khoang cach tu diem dung truoc (km)',
    ADD COLUMN travel_time_from_prev_min INT NULL COMMENT 'Thoi gian di chuyen tu diem dung truoc (phut)';
