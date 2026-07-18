-- US-07 Product Management integration-test cleanup
-- Run only after Postman and MySQL evidence screenshots have been saved.

USE elog_db;

-- Test products created by TC-01, TC-06, TC-07 and TC-09.
DELETE FROM products
WHERE sku LIKE 'PRD-IT07-%';

-- TC-17 temporarily deactivates this canonical seed product.
UPDATE products
SET is_active = TRUE
WHERE sku = 'PHN-APL-14';

-- Remove authorization-only users created during environment preparation.
DELETE ur
FROM user_roles ur
JOIN users u ON u.id = ur.user_id
WHERE u.username IN ('dispatcher_it07', 'warehouse_it07');

DELETE FROM users
WHERE username IN ('dispatcher_it07', 'warehouse_it07');

-- Cleanup verification.
SELECT COUNT(*) AS remaining_test_products
FROM products
WHERE sku LIKE 'PRD-IT07-%';

SELECT sku, is_active
FROM products
WHERE sku = 'PHN-APL-14';

SELECT COUNT(*) AS remaining_test_users
FROM users
WHERE username IN ('dispatcher_it07', 'warehouse_it07');

-- Expected:
--   remaining_test_products = 0
--   PHN-APL-14 is_active = 1
--   remaining_test_users = 0

