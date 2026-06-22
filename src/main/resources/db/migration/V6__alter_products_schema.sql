-- V6__alter_products_schema.sql
-- US-07: Product Management
-- Aligns products table (created in V1 with placeholder schema) to US-07 spec:
--   · Rename: code→sku, name→product_name, length_m→length_cm, width_m→width_cm, height_m→height_cm
--   · Unit change: metres → centimetres for dimension columns
--   · Add NOT NULL + CHECK constraints on all measurement fields
--   · Replace V2 placeholder seed data with US-07 canonical seed

-- ── Step 1: Remove V2 placeholder seed (dimensions were in metres, wrong SKU format) ──
DELETE FROM products;

-- ── Step 2: Rename columns + fix types + add constraints ─────────────────────────────
ALTER TABLE products
    CHANGE COLUMN code         sku          VARCHAR(50)    NOT NULL,
    CHANGE COLUMN name         product_name VARCHAR(100)   NOT NULL,
    CHANGE COLUMN length_m     length_cm    DECIMAL(8,2)   NOT NULL,
    CHANGE COLUMN width_m      width_cm     DECIMAL(8,2)   NOT NULL,
    CHANGE COLUMN height_m     height_cm    DECIMAL(8,2)   NOT NULL,
    MODIFY COLUMN weight_kg    DECIMAL(8,3) NOT NULL,
    MODIFY COLUMN volume_m3    DECIMAL(10,6) NOT NULL,
    DROP INDEX uq_products_code,
    ADD CONSTRAINT uq_products_sku UNIQUE (sku),
    ADD CONSTRAINT chk_products_weight_kg  CHECK (weight_kg > 0),
    ADD CONSTRAINT chk_products_length_cm  CHECK (length_cm > 0),
    ADD CONSTRAINT chk_products_width_cm   CHECK (width_cm > 0),
    ADD CONSTRAINT chk_products_height_cm  CHECK (height_cm > 0),
    ADD CONSTRAINT chk_products_volume_m3  CHECK (volume_m3 > 0);

-- ── Step 3: Seed canonical product data for US-08 Excel Import testing ───────────────
-- volume_m3 = length_cm × width_cm × height_cm ÷ 1,000,000 (pre-computed)
INSERT INTO products (sku, product_name, weight_kg, length_cm, width_cm, height_cm, volume_m3) VALUES
('TV-SAM-55',   'Tivi Samsung 55" Crystal UHD',    28.500, 135.00, 18.00,  82.00, 0.199800),
('TV-SAM-43',   'Tivi Samsung 43" Crystal UHD',    18.500, 107.00, 17.00,  68.00, 0.123428),
('PHN-APL-14',  'iPhone 14 128GB (hộp)',             0.450,  22.00, 12.00,   8.00, 0.002112),
('PHN-SAM-S23', 'Samsung Galaxy S23 (hộp)',          0.400,  20.00, 12.00,   8.00, 0.001920),
('REF-SAM-300', 'Tủ lạnh Samsung 300L',            65.000,  60.00, 68.00, 175.00, 0.714000),
('GEN-DNY-5K',  'Máy phát điện Denyo 5KVA',       190.000,  75.00, 55.00,  68.00, 0.280500),
('ACC-USB-C1',  'Cáp sạc USB-C 1m (hộp)',           0.120,  25.00, 15.00,   5.00, 0.001875);

