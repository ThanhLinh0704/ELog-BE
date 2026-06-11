-- ============================================================
-- ELog Delivery Management System
-- Flyway Migration: V2__seed_reference_data.sql
-- Reference / seed data for Sprint 1 development
-- ============================================================
-- ⚠ IMPORTANT: password_hash values below are BCrypt hashes of
--   the plaintext passwords shown in the comment.
--   CHANGE before any non-local deployment.
-- ============================================================

-- ────────────────────────────────────────────────────────────
-- 1. Roles (5 system roles — fixed set, do not add more)
-- ────────────────────────────────────────────────────────────
INSERT INTO roles (id, name) VALUES
(1, 'ADMIN'),
(2, 'DISPATCHER'),
(3, 'WAREHOUSE_STAFF'),
(4, 'DRIVER'),
(5, 'LOGISTICS_MANAGER');

-- ────────────────────────────────────────────────────────────
-- 2. Default Admin User
-- username: admin | password: Admin@2025 (BCrypt, cost 12)
-- ────────────────────────────────────────────────────────────
INSERT INTO users (id, username, password_hash, full_name, role_id, is_active) VALUES
(1, 'admin',
 '$2a$12$4tQRRKOvZ1f5.2R5C8zTtuIXIHBSb4yjJj7r2VtQKEZEG6GCCLFqm',
 'System Administrator', 1, TRUE);

-- ────────────────────────────────────────────────────────────
-- 3. Dev Users (Sprint 1 testing — 1 per role)
-- password for all dev users: Dev@2025
-- ────────────────────────────────────────────────────────────
INSERT INTO users (id, username, password_hash, full_name, role_id, is_active) VALUES
(2, 'dispatcher01',
 '$2a$12$UGkFh4FVYnmX9FvjLkF9FO8yC.Tr8x2bS2D0V8q7kDQv6aFcvp./K',
 'Nguyen Van Dispatcher', 2, TRUE),

(3, 'warehouse01',
 '$2a$12$UGkFh4FVYnmX9FvjLkF9FO8yC.Tr8x2bS2D0V8q7kDQv6aFcvp./K',
 'Tran Thi Warehouse', 3, TRUE),

(4, 'driver01',
 '$2a$12$UGkFh4FVYnmX9FvjLkF9FO8yC.Tr8x2bS2D0V8q7kDQv6aFcvp./K',
 'Le Van Driver', 4, TRUE),

(5, 'manager01',
 '$2a$12$UGkFh4FVYnmX9FvjLkF9FO8yC.Tr8x2bS2D0V8q7kDQv6aFcvp./K',
 'Pham Thi Manager', 5, TRUE);

-- ────────────────────────────────────────────────────────────
-- 4. Sample Stores (3 stores for dev testing)
-- ────────────────────────────────────────────────────────────
INSERT INTO stores (id, code, name, address, latitude, longitude) VALUES
(1, 'ST-001', 'Cửa hàng Quận 1',
    '123 Nguyễn Huệ, Quận 1, TP.HCM',    10.7769, 106.7009),
(2, 'ST-002', 'Cửa hàng Quận 3',
    '45 Võ Văn Tần, Quận 3, TP.HCM',     10.7680, 106.6890),
(3, 'ST-003', 'Cửa hàng Bình Thạnh',
    '78 Phan Đăng Lưu, Bình Thạnh, TP.HCM', 10.7980, 106.6990);

-- ────────────────────────────────────────────────────────────
-- 5. Sample Routes (2 routes)
-- BR-05: routes are read-only reference data
-- ────────────────────────────────────────────────────────────
INSERT INTO routes (id, code, name, is_active) VALUES
(1, 'RT-001', 'Tuyến Trung Tâm',   TRUE),
(2, 'RT-002', 'Tuyến Bắc Thành',   TRUE);

-- ────────────────────────────────────────────────────────────
-- 6. Route Stops
-- sequence_no: 1 = delivered first (= loaded last = LIFO bottom)
-- RT-001: ST-001 (seq 1) → ST-002 (seq 2)
-- RT-002: ST-003 (seq 1)
-- ────────────────────────────────────────────────────────────
INSERT INTO route_stops (id, route_id, store_id, sequence_no) VALUES
(1, 1, 1, 1),   -- RT-001 stop 1: Quận 1 (first delivery → loaded last)
(2, 1, 2, 2),   -- RT-001 stop 2: Quận 3 (last delivery → loaded first)
(3, 2, 3, 1);   -- RT-002 stop 1: Bình Thạnh

-- ────────────────────────────────────────────────────────────
-- 7. Sample Vehicles (2 vehicles with different capacities)
-- ────────────────────────────────────────────────────────────
INSERT INTO vehicles (id, plate_no, capacity_m3, capacity_kg, is_active) VALUES
(1, '51F-12345', 10.000, 2000.000, TRUE),   -- Medium truck
(2, '51F-67890',  5.000,  800.000, TRUE);   -- Small van

-- ────────────────────────────────────────────────────────────
-- 8. Sample Products (3 electronics products)
-- volume_m3 = length * width * height (pre-computed)
-- ────────────────────────────────────────────────────────────
INSERT INTO products (id, code, name, length_m, width_m, height_m, volume_m3, weight_kg) VALUES
(1, 'PRD-001', 'Tivi LCD 55 inch',
    0.140, 1.270, 0.800, 0.142240, 25.000),
(2, 'PRD-002', 'Tủ lạnh 300L',
    0.700, 0.680, 1.700, 0.808360, 75.000),
(3, 'PRD-003', 'Máy giặt 8kg',
    0.600, 0.590, 0.850, 0.300600, 55.000);
