-- V51: Add return_distance_km to trip_drafts and trips

ALTER TABLE trip_drafts
    ADD COLUMN return_distance_km DECIMAL(10,2) NULL COMMENT 'Quang duong di chuyen ve kho (km)';

ALTER TABLE trips
    ADD COLUMN return_distance_km DECIMAL(10,2) NULL COMMENT 'Quang duong di chuyen ve kho (km)';
