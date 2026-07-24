-- V31__add_order_time_override_fields.sql
ALTER TABLE orders
    ADD COLUMN is_delivery_time_overridden BOOLEAN DEFAULT FALSE,
    ADD COLUMN time_override_reason VARCHAR(255) NULL,
    ADD COLUMN time_override_by BIGINT NULL,
    ADD COLUMN time_override_at TIMESTAMP NULL;
