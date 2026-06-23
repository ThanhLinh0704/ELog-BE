-- V6__add_route_description.sql
-- US-04 Route Management: add description column to routes table
ALTER TABLE routes ADD COLUMN description VARCHAR(255) NULL AFTER name;
-- US-04: add created_at column to route_stops table
ALTER TABLE route_stops ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
-- US-04: rename sequence_no to sequence_order in route_stops table
ALTER TABLE route_stops RENAME COLUMN sequence_no TO sequence_order;
