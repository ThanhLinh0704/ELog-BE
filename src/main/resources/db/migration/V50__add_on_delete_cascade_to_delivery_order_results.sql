-- V50__add_on_delete_cascade_to_delivery_order_results.sql
-- Add ON DELETE CASCADE to foreign keys on delivery_order_results to prevent orphan rows

DELETE FROM delivery_order_results WHERE order_id NOT IN (SELECT id FROM orders);
DELETE FROM delivery_order_results WHERE stop_id NOT IN (SELECT id FROM trip_draft_stops);
DELETE FROM delivery_order_results WHERE trip_execution_id NOT IN (SELECT id FROM trip_executions);

ALTER TABLE delivery_order_results DROP FOREIGN KEY fk_dor_order;
ALTER TABLE delivery_order_results DROP FOREIGN KEY fk_dor_stop;

ALTER TABLE delivery_order_results
    ADD CONSTRAINT fk_dor_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_dor_stop FOREIGN KEY (stop_id) REFERENCES trip_draft_stops(id) ON DELETE CASCADE;
