-- V19__add_actual_departure_time_to_trip_stops.sql
-- Fix: TripStop entity declares actualDepartureTime but column was missing from trip_stops table (V17)

ALTER TABLE trip_stops
    ADD COLUMN actual_departure_time DATETIME NULL
        AFTER actual_arrival_time;
