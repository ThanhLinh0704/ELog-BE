-- V13__seed_us10_trip_draft_data.sql
-- Seed Trip Drafts for delivery date 2026-03-16 based on orders in V10
-- Update orders in V10 to ACCEPTED status to reflect a successful import validation stage (US-09)

-- ────────────────────────────────────────────────────────────
-- 1. Update Orders status of batch #1 (2026-03-16) to ACCEPTED
-- ────────────────────────────────────────────────────────────
UPDATE orders SET status = 'ACCEPTED' WHERE import_batch_id = 1;

-- ────────────────────────────────────────────────────────────
-- 2. Seed Trip Drafts
--    RT-001 (id=1): active_stop_count = 2 (ST-001 & ST-002), skipped_stop_count = 0
--                   total_volume = 1.62726 (Order 1) + 0.8415 (Order 2) = 2.46876 m3
--                   total_weight = 158.5 (Order 1) + 570.0 (Order 2) = 728.5 kg
--    RT-002 (id=2): active_stop_count = 1 (ST-003), skipped_stop_count = 0
--                   total_volume = 0.02112 m3
--                   total_weight = 4.5 kg
-- ────────────────────────────────────────────────────────────
INSERT INTO trip_drafts (id, route_id, delivery_date, total_volume_m3, total_weight_kg, active_stop_count, skipped_stop_count, status) VALUES
(1, 1, '2026-03-16', 2.468760, 728.500, 2, 0, 'DRAFT'),
(2, 2, '2026-03-16', 0.021120,   4.500, 1, 0, 'DRAFT');

-- ────────────────────────────────────────────────────────────
-- 3. Seed Trip Draft Stops
-- ────────────────────────────────────────────────────────────
INSERT INTO trip_draft_stops (id, trip_draft_id, route_stop_id, store_id, sequence_no, is_active, order_count) VALUES
-- RT-001 Stops (Stores 1 and 2, RouteStops 1 and 2)
(1, 1, 1, 1, 1, TRUE, 1),
(2, 1, 2, 2, 2, TRUE, 1),
-- RT-002 Stops (Store 3, RouteStop 3)
(3, 2, 3, 3, 1, TRUE, 1);

-- ────────────────────────────────────────────────────────────
-- 4. Associate orders to trip draft
-- ────────────────────────────────────────────────────────────
UPDATE orders SET trip_draft_id = 1 WHERE id IN (1, 2);
UPDATE orders SET trip_draft_id = 2 WHERE id = 3;
