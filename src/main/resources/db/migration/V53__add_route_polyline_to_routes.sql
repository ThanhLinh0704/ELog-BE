-- Add route_polyline, total_distance_km, and total_duration_min columns to routes table
ALTER TABLE routes
    ADD COLUMN route_polyline LONGTEXT NULL AFTER description,
    ADD COLUMN total_distance_km DOUBLE NULL AFTER route_polyline,
    ADD COLUMN total_duration_min INT NULL AFTER total_distance_km;
