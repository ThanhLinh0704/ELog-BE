-- US-07 Product Management integration-test setup
-- Run before starting the Postman collection.

USE elog_db;

-- Remove stale data from an interrupted previous US-07 run.
DELETE FROM products
WHERE sku LIKE 'PRD-IT07-%';

DELETE ur
FROM user_roles ur
JOIN users u ON u.id = ur.user_id
WHERE u.username IN ('dispatcher_it07', 'warehouse_it07');

DELETE FROM users
WHERE username IN ('dispatcher_it07', 'warehouse_it07');

-- Restore canonical lookup products to their initial active state.
UPDATE products
SET is_active = TRUE
WHERE sku IN ('TV-SAM-55', 'PHN-APL-14');

-- Confirm product seed data and manual volume calculations.
SELECT id, sku, product_name, weight_kg,
       length_m, width_m, height_m, volume_m3, is_active,
       ROUND(length_m * width_m * height_m, 6) AS recalculated_volume_m3
FROM products
WHERE sku IN ('TV-SAM-55', 'PHN-APL-14')
ORDER BY sku;

-- Required roles must exist before creating the two test users through API.
SELECT id, name
FROM roles
WHERE name IN ('SYSTEM_ADMIN', 'DISPATCHER', 'WAREHOUSE_STAFF')
ORDER BY name;

-- admin must exist; dispatcher_it07 and warehouse_it07 are created through
-- POST /api/users as documented in IT-US07-ProductManagement.md.
SELECT u.id, u.username, u.email, u.is_active,
       GROUP_CONCAT(r.name ORDER BY r.name) AS roles
FROM users u
LEFT JOIN user_roles ur ON ur.user_id = u.id
LEFT JOIN roles r ON r.id = ur.role_id
WHERE u.username IN ('admin', 'dispatcher_it07', 'warehouse_it07')
GROUP BY u.id, u.username, u.email, u.is_active
ORDER BY u.username;

-- Expected before Postman execution:
--   1. No product whose SKU starts with PRD-IT07-.
--   2. TV-SAM-55 and PHN-APL-14 are active.
--   3. SYSTEM_ADMIN, DISPATCHER and WAREHOUSE_STAFF roles exist.
--   4. admin exists; the two *_it07 users do not yet exist.

