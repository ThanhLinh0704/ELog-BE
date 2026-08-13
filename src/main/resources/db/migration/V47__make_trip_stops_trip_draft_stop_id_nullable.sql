-- V47__make_trip_stops_trip_draft_stop_id_nullable.sql
-- Allow trip_draft_stop_id in trip_stops to be nullable when a RouteStop is removed from route configuration

ALTER TABLE trip_stops MODIFY COLUMN trip_draft_stop_id BIGINT NULL;
