-- V9__us08_import_schema.sql — US-08 Excel Import
-- Migrate from V1 flat schema (1 order = 1 product) to US-08 schema (1 order = N items)
-- V1 had: excel_imports, orders (flat: 1 product/row)
-- US-08 needs: import_batches (replaces excel_imports), orders (grouped by order_ref+store),
--              order_items (1 order → N products with snapshot), import_errors

-- ──────────────────────────────────────────────────────────────
-- 1. CREATE import_batches (replaces excel_imports with new fields)
-- ──────────────────────────────────────────────────────────────
CREATE TABLE import_batches (
    id             BIGINT       AUTO_INCREMENT PRIMARY KEY,
    delivery_date  DATE         NOT NULL,
    file_name      VARCHAR(255) NOT NULL,
    uploaded_by    BIGINT       NOT NULL,
    total_rows     INT          NOT NULL DEFAULT 0,
    accepted_rows  INT          NOT NULL DEFAULT 0,
    rejected_rows  INT          NOT NULL DEFAULT 0,
    status         VARCHAR(20)  NOT NULL DEFAULT 'PROCESSING',
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Generated column + UNIQUE: chỉ tối đa 1 batch ACTIVE cho mỗi delivery_date
    active_date    DATE GENERATED ALWAYS AS (IF(is_active, delivery_date, NULL)) STORED,
    CONSTRAINT fk_batch_user FOREIGN KEY (uploaded_by) REFERENCES users(id),
    CONSTRAINT uq_batch_active_date UNIQUE (active_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ──────────────────────────────────────────────────────────────
-- 2. DROP old orders FK dependencies, then ALTER orders table
--    V1 schema: orders had product_id, quantity, volume_m3, weight_kg (flat)
--    US-08: orders grouped by order_ref + store, items in separate table
-- ──────────────────────────────────────────────────────────────

-- 2a. Delete old data (V1 orders are skeleton data, never used in production)
DELETE FROM loading_manifest_items;
DELETE FROM orders;

-- 2b. Drop FKs referencing old orders from other tables
ALTER TABLE loading_manifest_items DROP FOREIGN KEY fk_manifest_order;

-- 2c. Drop old FKs on orders itself
ALTER TABLE orders DROP FOREIGN KEY fk_orders_import;
ALTER TABLE orders DROP FOREIGN KEY fk_orders_product;

-- 2d. Drop old indexes
DROP INDEX idx_orders_importdate ON orders;

-- 2e. Drop columns that belong to order_items now
ALTER TABLE orders
    DROP COLUMN product_id,
    DROP COLUMN quantity,
    DROP COLUMN volume_m3,
    DROP COLUMN weight_kg,
    DROP COLUMN excel_import_id;

-- 2f. Rename import_date → delivery_date
ALTER TABLE orders CHANGE COLUMN import_date delivery_date DATE NOT NULL;

-- 2g. Change status from ENUM to VARCHAR (US-08 uses 'IMPORTED', later US-10/15 add more)
ALTER TABLE orders MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'IMPORTED';

-- 2h. Add new columns (table is empty after DELETE, so NOT NULL is safe)
ALTER TABLE orders
    ADD COLUMN import_batch_id BIGINT NOT NULL AFTER id,
    ADD COLUMN order_ref VARCHAR(50) NOT NULL DEFAULT '' AFTER import_batch_id;

-- 2i. Add new constraints
ALTER TABLE orders
    ADD CONSTRAINT fk_order_batch FOREIGN KEY (import_batch_id) REFERENCES import_batches(id),
    ADD CONSTRAINT uq_order_batch_ref_store UNIQUE (import_batch_id, order_ref, store_id);

-- ──────────────────────────────────────────────────────────────
-- 3. CREATE order_items (product details + snapshot per line)
-- ──────────────────────────────────────────────────────────────
CREATE TABLE order_items (
    id              BIGINT        AUTO_INCREMENT PRIMARY KEY,
    order_id        BIGINT        NOT NULL,
    product_id      BIGINT        NOT NULL,
    sku             VARCHAR(50)   NOT NULL,
    quantity        INT           NOT NULL CHECK (quantity > 0),
    unit_weight_kg  DECIMAL(8,3)  NOT NULL,
    unit_volume_m3  DECIMAL(10,6) NOT NULL,
    line_weight_kg  DECIMAL(10,3) NOT NULL,
    line_volume_m3  DECIMAL(12,6) NOT NULL,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_item_order   FOREIGN KEY (order_id)   REFERENCES orders(id),
    CONSTRAINT fk_item_product FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ──────────────────────────────────────────────────────────────
-- 4. CREATE import_errors
-- ──────────────────────────────────────────────────────────────
CREATE TABLE import_errors (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    import_batch_id BIGINT       NOT NULL,
    row_num         INT          NOT NULL,
    raw_data        TEXT         NULL,
    error_reason    VARCHAR(255) NOT NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_error_batch FOREIGN KEY (import_batch_id) REFERENCES import_batches(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ──────────────────────────────────────────────────────────────
-- 5. Re-add FK from loading_manifest_items → orders (still valid)
-- ──────────────────────────────────────────────────────────────
ALTER TABLE loading_manifest_items
    ADD CONSTRAINT fk_manifest_order FOREIGN KEY (order_id) REFERENCES orders(id);

-- ──────────────────────────────────────────────────────────────
-- 6. DROP legacy excel_imports table (replaced by import_batches)
-- ──────────────────────────────────────────────────────────────
DROP TABLE IF EXISTS excel_imports;
