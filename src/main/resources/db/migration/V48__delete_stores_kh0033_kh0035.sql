-- ============================================================
-- V48__delete_stores_kh0033_kh0035.sql
-- Delete stores KH0033 (id 33) and KH0035 (id 35) per request
-- ============================================================

DELETE FROM route_stops WHERE store_id IN (SELECT id FROM stores WHERE code IN ('KH0033', 'KH0035'));
DELETE FROM orders WHERE store_id IN (SELECT id FROM stores WHERE code IN ('KH0033', 'KH0035'));
DELETE FROM stores WHERE code IN ('KH0033', 'KH0035');
