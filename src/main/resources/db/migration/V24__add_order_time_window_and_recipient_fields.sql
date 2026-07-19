-- ============================================================
-- ELog Delivery Management System
-- Flyway Migration: V24__add_order_time_window_and_recipient_fields.sql
-- Add delivery_time_window, recipient_name, recipient_phone, and notes to orders table
-- ============================================================

ALTER TABLE orders
    ADD COLUMN delivery_time_window VARCHAR(100) NULL,
    ADD COLUMN recipient_name VARCHAR(100) NULL,
    ADD COLUMN recipient_phone VARCHAR(20) NULL,
    ADD COLUMN notes TEXT NULL;
