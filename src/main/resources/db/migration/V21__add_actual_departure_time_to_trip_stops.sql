-- V21__add_actual_departure_time_to_trip_stops.sql
-- Add missing actual_departure_time column to trip_stops table to align with JPA entity

ALTER TABLE trip_stops ADD COLUMN actual_departure_time DATETIME NULL AFTER actual_arrival_time;
