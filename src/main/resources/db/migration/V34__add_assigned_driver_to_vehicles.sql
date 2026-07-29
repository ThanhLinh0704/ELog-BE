-- Migration: V34__add_assigned_driver_to_vehicles.sql
ALTER TABLE vehicles ADD COLUMN assigned_driver_id BIGINT NULL;

ALTER TABLE vehicles ADD CONSTRAINT fk_vehicles_assigned_driver 
    FOREIGN KEY (assigned_driver_id) REFERENCES users(id);

CREATE INDEX idx_vehicles_assigned_driver ON vehicles(assigned_driver_id);
