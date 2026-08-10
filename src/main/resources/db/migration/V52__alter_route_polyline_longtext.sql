-- V52: Alter route_polyline column type from TEXT to LONGTEXT for trip_drafts and trips

ALTER TABLE trip_drafts MODIFY COLUMN route_polyline LONGTEXT;
ALTER TABLE trips MODIFY COLUMN route_polyline LONGTEXT;
