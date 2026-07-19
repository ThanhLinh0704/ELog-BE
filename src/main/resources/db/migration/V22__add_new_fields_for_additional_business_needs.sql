-- ============================================================
-- ELog Delivery Management System
-- Flyway Migration: V22__add_new_fields_for_additional_business_needs.sql
-- Add business fields for stores, vehicles, products, trips, and trip_drafts
-- ============================================================

-- 1. Update stores table
ALTER TABLE stores
    ADD COLUMN allowed_delivery_hours VARCHAR(255) NOT NULL DEFAULT 'All',
    ADD COLUMN max_allowed_vehicle_weight DECIMAL(10,3) NULL,
    ADD COLUMN image_url VARCHAR(512) NULL;

-- 2. Update vehicles table
ALTER TABLE vehicles
    ADD COLUMN image_url VARCHAR(512) NULL,
    ADD COLUMN permit_info TEXT NULL,
    ADD COLUMN description TEXT NULL;

-- 3. Update products table
ALTER TABLE products
    ADD COLUMN shape VARCHAR(100) NULL,
    ADD COLUMN is_fragile BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN package_image_url VARCHAR(512) NULL,
    ADD COLUMN description TEXT NULL;


