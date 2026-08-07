-- V46__make_trip_stops_route_stop_id_nullable.sql
-- Allow route_stop_id in trip_stops to be nullable when a RouteStop is removed from route configuration

ALTER TABLE trip_stops MODIFY COLUMN route_stop_id BIGINT NULL;
