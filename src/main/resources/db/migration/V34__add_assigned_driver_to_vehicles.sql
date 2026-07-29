-- Migration: V34__add_assigned_driver_to_vehicles.sql
ALTER TABLE vehicles ADD COLUMN assigned_driver_id BIGINT NULL;

ALTER TABLE vehicles ADD CONSTRAINT fk_vehicles_assigned_driver 
    FOREIGN KEY (assigned_driver_id) REFERENCES users(id);

CREATE INDEX idx_vehicles_assigned_driver ON vehicles(assigned_driver_id);

-- Seed default 1-to-1 vehicle-driver assignments matching required license classes
UPDATE vehicles SET assigned_driver_id = 4 WHERE id = 1;  -- XE001 (1.25T, req B)  <- driver01 (B)
UPDATE vehicles SET assigned_driver_id = 8 WHERE id = 2;  -- XE002 (1.25T, req B)  <- driver04 (B)
UPDATE vehicles SET assigned_driver_id = 6 WHERE id = 11; -- XE011 (2.5T,  req C1) <- driver02 (C1)
UPDATE vehicles SET assigned_driver_id = 9 WHERE id = 12; -- XE012 (2.5T,  req C1) <- driver05 (C1)
UPDATE vehicles SET assigned_driver_id = 7 WHERE id = 25; -- XE025 (5T,    req C)  <- driver03 (C)
UPDATE vehicles SET assigned_driver_id = 10 WHERE id = 26;-- XE026 (5T,    req C)  <- driver06 (C)
