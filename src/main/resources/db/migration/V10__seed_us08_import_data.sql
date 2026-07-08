-- ============================================================
-- V10__seed_us08_import_data.sql
-- US-08: Seed data cho import_batches, orders, order_items, import_errors
-- Sử dụng stores (ST-001, ST-002, ST-003) và products (SKU từ V7)
-- User: dispatcher01 (id=2)
-- ============================================================

-- ────────────────────────────────────────────────────────────
-- 1. Import Batch — Batch #1: ngày 16/03/2026, 6 dòng (4 OK, 2 lỗi)
-- ────────────────────────────────────────────────────────────
INSERT INTO import_batches (id, delivery_date, file_name, uploaded_by, total_rows, accepted_rows, rejected_rows, status, is_active) VALUES
(1, '2026-03-16', 'donhang_16-03.xlsx', 2, 6, 4, 2, 'COMPLETED', TRUE);

-- ────────────────────────────────────────────────────────────
-- 2. Orders — 3 đơn hàng từ batch #1
--    DH160325-01: ST-001 (Quận 1) — 2 sản phẩm (gộp đơn)
--    DH160325-02: ST-002 (Quận 3) — 1 sản phẩm
--    DH160325-03: ST-003 (Bình Thạnh) — 1 sản phẩm
-- ────────────────────────────────────────────────────────────
INSERT INTO orders (id, import_batch_id, order_ref, store_id, delivery_date, status) VALUES
(1, 1, 'DH160325-01', 1, '2026-03-16', 'IMPORTED'),
(2, 1, 'DH160325-02', 2, '2026-03-16', 'IMPORTED'),
(3, 1, 'DH160325-03', 3, '2026-03-16', 'IMPORTED');

-- ────────────────────────────────────────────────────────────
-- 3. Order Items — 4 dòng hợp lệ, snapshot weight/volume
--    Dùng products: REF-SAM-300 (65kg/0.714m³), TV-SAM-55 (28.5kg/0.19926m³),
--                   PHN-APL-14 (0.45kg/0.002112m³), GEN-DNY-5K (190kg/0.2805m³)
-- ────────────────────────────────────────────────────────────
INSERT INTO order_items (id, order_id, product_id, sku, quantity, unit_weight_kg, unit_volume_m3, line_weight_kg, line_volume_m3) VALUES
-- Order DH160325-01 (ST-001): 2 items → gộp đơn
(1, 1, 5, 'REF-SAM-300', 2, 65.000, 0.714000, 130.000, 1.428000),   -- Tủ lạnh Samsung 300L × 2
(2, 1, 1, 'TV-SAM-55',   1, 28.500, 0.199260,  28.500, 0.199260),   -- Tivi Samsung 55" × 1
-- Order DH160325-02 (ST-002): 1 item
(3, 2, 6, 'GEN-DNY-5K',  3, 190.000, 0.280500, 570.000, 0.841500),  -- Máy phát điện × 3
-- Order DH160325-03 (ST-003): 1 item
(4, 3, 3, 'PHN-APL-14', 10,   0.450, 0.002112,   4.500, 0.021120);  -- iPhone 14 × 10

-- ────────────────────────────────────────────────────────────
-- 4. Import Errors — 2 dòng bị reject
-- ────────────────────────────────────────────────────────────
INSERT INTO import_errors (id, import_batch_id, row_num, raw_data, error_reason) VALUES
(1, 1, 5, 'DH160325-04,ST-HD-099,REF-SAM-300,1',  'Mã cửa hàng ''ST-HD-099'' không tồn tại trong hệ thống'),
(2, 1, 6, 'DH160325-05,ST-001,ACC-HDMI-2M,5',     'SKU ''ACC-HDMI-2M'' chưa có trong danh mục sản phẩm');

-- ────────────────────────────────────────────────────────────
-- 5. Import Batch #2 — batch cũ đã bị thay thế (is_active=false)
--    Minh hoạ flow re-import: batch #2 cho ngày 17/03, sau đó batch #3 thay thế
-- ────────────────────────────────────────────────────────────
INSERT INTO import_batches (id, delivery_date, file_name, uploaded_by, total_rows, accepted_rows, rejected_rows, status, is_active) VALUES
(2, '2026-03-17', 'donhang_17-03_v1.xlsx', 2, 3, 3, 0, 'COMPLETED', FALSE),
(3, '2026-03-17', 'donhang_17-03_v2.xlsx', 2, 4, 4, 0, 'COMPLETED', TRUE);

-- Orders cho batch #3 (batch hiện hành ngày 17/03)
INSERT INTO orders (id, import_batch_id, order_ref, store_id, delivery_date, status) VALUES
(4, 3, 'DH170325-01', 1, '2026-03-17', 'IMPORTED'),
(5, 3, 'DH170325-02', 2, '2026-03-17', 'IMPORTED');

INSERT INTO order_items (id, order_id, product_id, sku, quantity, unit_weight_kg, unit_volume_m3, line_weight_kg, line_volume_m3) VALUES
(5, 4, 5, 'REF-SAM-300', 1, 65.000, 0.714000,  65.000, 0.714000),  -- Tủ lạnh × 1
(6, 4, 2, 'TV-SAM-43',   2, 18.500, 0.123692,  37.000, 0.247384),  -- Tivi 43" × 2
(7, 5, 4, 'PHN-SAM-S23', 5,  0.400, 0.001920,   2.000, 0.009600),  -- Samsung S23 × 5
(8, 5, 7, 'ACC-USB-C1', 20,  0.120, 0.001875,   2.400, 0.037500);  -- Cáp sạc × 20

-- Orders cho batch #2 (đã bị thay thế — giữ lại để audit)
INSERT INTO orders (id, import_batch_id, order_ref, store_id, delivery_date, status) VALUES
(6, 2, 'DH170325-01', 1, '2026-03-17', 'IMPORTED');

INSERT INTO order_items (id, order_id, product_id, sku, quantity, unit_weight_kg, unit_volume_m3, line_weight_kg, line_volume_m3) VALUES
(9, 6, 5, 'REF-SAM-300', 3, 65.000, 0.714000, 195.000, 2.142000);  -- File cũ ghi 3 cái, file mới sửa lại 1 cái
