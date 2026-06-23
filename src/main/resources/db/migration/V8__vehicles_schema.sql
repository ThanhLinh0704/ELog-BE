-- V8__vehicles_schema.sql
-- US-06 Vehicle Management
-- Align the placeholder vehicles table from V1 to the US-06 master-data schema.
--
-- Important:
--   V1 already created vehicles with the old columns:
--     plate_no, capacity_m3, capacity_kg
--   Therefore this migration must ALTER the existing table, not CREATE it again.

-- Step 1: Rename legacy columns to US-06 names.
ALTER TABLE vehicles
    CHANGE COLUMN plate_no plate_number VARCHAR(20) NOT NULL;

ALTER TABLE vehicles
    ADD COLUMN vehicle_type VARCHAR(50) NULL AFTER plate_number;

ALTER TABLE vehicles
    CHANGE COLUMN capacity_kg max_weight_kg DECIMAL(10,2) NOT NULL AFTER vehicle_type;

ALTER TABLE vehicles
    CHANGE COLUMN capacity_m3 max_volume_m3 DECIMAL(8,3) NOT NULL AFTER max_weight_kg;

-- Step 2: Backfill vehicle_type for existing Sprint-1 seed data.
UPDATE vehicles
SET vehicle_type = CASE
    WHEN plate_number = '51F-12345' THEN 'Xe tai trung'
    WHEN plate_number = '51F-67890' THEN 'Xe tai nho'
    ELSE 'Xe tai'
END
WHERE vehicle_type IS NULL;

-- Step 3: Enforce US-06 required constraints.
ALTER TABLE vehicles
    MODIFY COLUMN vehicle_type VARCHAR(50) NOT NULL,
    MODIFY COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD CONSTRAINT chk_vehicles_max_weight_kg CHECK (max_weight_kg > 0),
    ADD CONSTRAINT chk_vehicles_max_volume_m3 CHECK (max_volume_m3 > 0);

-- Step 4: Align dev seed data with US-06 examples when the original Sprint-1
-- placeholder plates are still present. Avoid overwriting user-created vehicles.
UPDATE vehicles
SET plate_number = '51A-12345',
    vehicle_type = 'Xe tai nho',
    max_weight_kg = 1490.00,
    max_volume_m3 = 8.500,
    is_active = TRUE
WHERE plate_number = '51F-67890';

UPDATE vehicles
SET plate_number = '51B-67890',
    vehicle_type = 'Xe tai trung',
    max_weight_kg = 3490.00,
    max_volume_m3 = 16.000,
    is_active = TRUE
WHERE plate_number = '51F-12345';

INSERT IGNORE INTO vehicles (plate_number, vehicle_type, max_weight_kg, max_volume_m3, is_active) VALUES
('51C-11223', 'Xe tai lon', 4990.00, 25.000, TRUE);
