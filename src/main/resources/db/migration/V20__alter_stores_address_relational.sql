-- ============================================================
-- ELog Delivery Management System
-- Flyway Migration: V20__alter_stores_address_relational.sql
-- Relational address fields for stores
-- ============================================================

ALTER TABLE stores
    ADD COLUMN province_code VARCHAR(20) NULL,
    ADD COLUMN district_code VARCHAR(20) NULL,
    ADD COLUMN ward_code VARCHAR(20) NULL,
    ADD COLUMN address_detail VARCHAR(255) NULL;

-- Migrate / seed the existing stores data
UPDATE stores SET province_code = '79', district_code = '760', ward_code = '26740', address_detail = '123 Nguyễn Huệ' WHERE id = 1;
UPDATE stores SET province_code = '79', district_code = '770', ward_code = '27139', address_detail = '45 Võ Văn Tần' WHERE id = 2;
UPDATE stores SET province_code = '79', district_code = '765', ward_code = '26926', address_detail = '78 Phan Đăng Lưu' WHERE id = 3;

-- Drop old flat address column
ALTER TABLE stores DROP COLUMN address;

-- Add foreign key constraints
ALTER TABLE stores ADD CONSTRAINT fk_stores_province FOREIGN KEY (province_code) REFERENCES provinces(code);
ALTER TABLE stores ADD CONSTRAINT fk_stores_district FOREIGN KEY (district_code) REFERENCES districts(code);
ALTER TABLE stores ADD CONSTRAINT fk_stores_ward     FOREIGN KEY (ward_code)     REFERENCES wards(code);
