-- US-08 Excel Import integration-test setup
-- Run before starting the Postman collection.

USE elog_db;

-- 1. Remove import-related tables data to start from a clean state.
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE order_items;
TRUNCATE TABLE orders;
TRUNCATE TABLE import_errors;
TRUNCATE TABLE import_batches;
SET FOREIGN_KEY_CHECKS = 1;

-- 2. Delete test users from previous runs
DELETE ur FROM user_roles ur JOIN users u ON u.id = ur.user_id WHERE u.username IN ('dispatcher_it08', 'warehouse_it08');
DELETE FROM users WHERE username IN ('dispatcher_it08', 'warehouse_it08');

-- 3. Reset product REF-SAM-300 weight to 65.000 in case it was modified
UPDATE products SET weight_kg = 65.000 WHERE sku = 'REF-SAM-300';

-- 4. Verify baseline state
SELECT COUNT(*) AS total_batches FROM import_batches;
SELECT COUNT(*) AS total_orders FROM orders;
SELECT COUNT(*) AS total_order_items FROM order_items;
SELECT weight_kg FROM products WHERE sku = 'REF-SAM-300';
