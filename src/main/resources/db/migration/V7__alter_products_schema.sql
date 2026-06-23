-- V6__alter_products_schema.sql
-- US-07: Product Management
-- Aligns products table (created in V1 with placeholder schema) to US-07 spec:
--   · Rename: code→sku, name→product_name
--   · Columns length_m, width_m, height_m already exist from V1 (metres)
--   · Add NOT NULL + CHECK constraints on all measurement fields
--   · Replace placeholder seed data with US-07 canonical seed

-- ── Step 1: Remove placeholder seed ──────────────────────────────────────────
DELETE FROM products;

-- ── Step 2: Rename columns + fix types + add constraints ─────────────────────
-- Note: length_m, width_m, height_m already exist in V1 with correct names
ALTER TABLE products
    CHANGE COLUMN code         sku          VARCHAR(50)    NOT NULL,
    CHANGE COLUMN name         product_name VARCHAR(100)   NOT NULL,
    MODIFY COLUMN length_m                  DECIMAL(8,4)   NOT NULL,
    MODIFY COLUMN width_m                   DECIMAL(8,4)   NOT NULL,
    MODIFY COLUMN height_m                  DECIMAL(8,4)   NOT NULL,
    MODIFY COLUMN weight_kg                 DECIMAL(8,3)   NOT NULL,
    MODIFY COLUMN volume_m3                 DECIMAL(10,6)  NOT NULL,
    DROP INDEX uq_products_code,
    ADD CONSTRAINT uq_products_sku        UNIQUE (sku),
    ADD CONSTRAINT chk_products_weight_kg CHECK (weight_kg > 0),
    ADD CONSTRAINT chk_products_length_m  CHECK (length_m > 0),
    ADD CONSTRAINT chk_products_width_m   CHECK (width_m > 0),
    ADD CONSTRAINT chk_products_height_m  CHECK (height_m > 0),
    ADD CONSTRAINT chk_products_volume_m3 CHECK (volume_m3 > 0);

-- ── Step 3: Seed canonical product data ──────────────────────────────────────
-- volume_m3 = length_m × width_m × height_m (pre-computed)
INSERT INTO products (sku, product_name, weight_kg, length_m, width_m, height_m, volume_m3) VALUES
('TV-SAM-55',   'Tivi Samsung 55" Crystal UHD',    28.500, 1.3500, 0.1800, 0.8200, 0.199260),
('TV-SAM-43',   'Tivi Samsung 43" Crystal UHD',    18.500, 1.0700, 0.1700, 0.6800, 0.123692),
('PHN-APL-14',  'iPhone 14 128GB (hộp)',             0.450, 0.2200, 0.1200, 0.0800, 0.002112),
('PHN-SAM-S23', 'Samsung Galaxy S23 (hộp)',          0.400, 0.2000, 0.1200, 0.0800, 0.001920),
('REF-SAM-300', 'Tủ lạnh Samsung 300L',            65.000, 0.6000, 0.6800, 1.7500, 0.714000),
('GEN-DNY-5K',  'Máy phát điện Denyo 5KVA',       190.000, 0.7500, 0.5500, 0.6800, 0.280500),
('ACC-USB-C1',  'Cáp sạc USB-C 1m (hộp)',           0.120, 0.2500, 0.1500, 0.0500, 0.001875);
