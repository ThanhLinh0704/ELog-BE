-- ============================================================
-- ELog Delivery Management System
-- Seed product permissions and assign to roles
-- ============================================================

-- 1. Seed product permissions
INSERT INTO permissions (id, name, description) VALUES
(17, 'product:read', 'View product catalog and details'),
(18, 'product:write', 'Create, update, or deactivate products');

-- 2. Seed role_permissions
-- SYSTEM_ADMIN (role_id = 1) -> product:read, product:write
INSERT INTO role_permissions (role_id, permission_id) VALUES
(1, 17), (1, 18);

-- DISPATCHER (role_id = 2) -> product:read
INSERT INTO role_permissions (role_id, permission_id) VALUES
(2, 17);

-- WAREHOUSE_STAFF (role_id = 3) -> product:read
INSERT INTO role_permissions (role_id, permission_id) VALUES
(3, 17);

-- LOGISTICS_MANAGER (role_id = 5) -> product:read
INSERT INTO role_permissions (role_id, permission_id) VALUES
(5, 17);
