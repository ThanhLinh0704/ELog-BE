-- V25__alter_vehicles_and_users_for_license_matching.sql

-- 1. Alter vehicles table: rename max_weight_kg to payload_kg
-- First drop the check constraint associated with max_weight_kg if it exists.
-- H2 and MySQL support DROP CONSTRAINT.
ALTER TABLE vehicles DROP CONSTRAINT chk_vehicles_max_weight_kg;

ALTER TABLE vehicles
    CHANGE COLUMN max_weight_kg payload_kg DECIMAL(10,2) NOT NULL;

ALTER TABLE vehicles
    ADD CONSTRAINT chk_vehicles_payload_kg CHECK (payload_kg > 0);

-- 2. Add new columns to vehicles table
ALTER TABLE vehicles
    ADD COLUMN vehicle_code VARCHAR(50) NULL UNIQUE,
    ADD COLUMN vehicle_class VARCHAR(20) NULL,
    ADD COLUMN gross_vehicle_weight_kg DECIMAL(10,2) NULL,
    ADD COLUMN required_license VARCHAR(10) NULL,
    ADD COLUMN cargo_length_mm INT NULL,
    ADD COLUMN cargo_width_mm INT NULL,
    ADD COLUMN cargo_height_mm INT NULL,
    ADD COLUMN average_speed_kmh DECIMAL(5,2) NULL,
    ADD COLUMN cost_per_km DECIMAL(10,2) NULL,
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE';

-- Backfill vehicle_code for existing data so we can enforce NOT NULL and UNIQUE later
UPDATE vehicles SET vehicle_code = CONCAT('XE', LPAD(id, 3, '0')) WHERE vehicle_code IS NULL;

-- Backfill other columns with sensible defaults for existing data
UPDATE vehicles SET
    vehicle_class = CASE
        WHEN payload_kg <= 1500 THEN '1.25T'
        WHEN payload_kg <= 3000 THEN '2.5T'
        WHEN payload_kg <= 4000 THEN '3.5T'
        WHEN payload_kg <= 6000 THEN '5T'
        ELSE '8T'
    END,
    gross_vehicle_weight_kg = payload_kg * 1.5,
    required_license = CASE
        WHEN payload_kg <= 3500 THEN 'B'
        WHEN payload_kg <= 4500 THEN 'C1'
        ELSE 'C'
    END,
    cargo_length_mm = 3100,
    cargo_width_mm = 1600,
    cargo_height_mm = 1700,
    average_speed_kmh = 50.00,
    cost_per_km = 5000.00
WHERE vehicle_class IS NULL;

-- Enforce NOT NULL on vehicle_code
ALTER TABLE vehicles MODIFY COLUMN vehicle_code VARCHAR(50) NOT NULL;

-- 3. Alter users table: add license_class
ALTER TABLE users
    ADD COLUMN license_class VARCHAR(10) NULL;

-- Backfill driver license class to C for existing drivers (assuming username starting with 'driver' or general drivers)
-- But simply letting it be null is fine, or we can set it to 'C' for existing drivers.
-- Let's update any user with driver in role/username/fullname just in case
UPDATE users SET license_class = 'C' WHERE id IN (
    SELECT user_id FROM user_roles WHERE role_id = (SELECT id FROM roles WHERE name = 'DRIVER')
);
