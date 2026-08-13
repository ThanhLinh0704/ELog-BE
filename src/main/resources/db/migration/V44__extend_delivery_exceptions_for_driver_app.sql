-- V44__extend_delivery_exceptions_for_driver_app.sql
-- ============================================================
-- Mở rộng delivery_exceptions để hỗ trợ ngoại lệ từ Driver App FT-09 (TripExecution + Order)
-- nới trip_stop_id thành NULL, bổ sung trip_execution_id và order_id.
-- ============================================================

ALTER TABLE delivery_exceptions
    MODIFY COLUMN trip_stop_id BIGINT NULL,
    ADD COLUMN trip_execution_id BIGINT NULL AFTER trip_stop_id,
    ADD COLUMN order_id BIGINT NULL AFTER trip_execution_id,
    ADD INDEX idx_de_trip_execution_id (trip_execution_id),
    ADD INDEX idx_de_order_id (order_id);
