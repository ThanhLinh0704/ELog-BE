-- V32__driver_app_execution_and_outcomes_schema.sql
-- FT-09 Supporting Driver Trip Updates & Outcome History Tables

-- 1. Trip Executions table
CREATE TABLE IF NOT EXISTS trip_executions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_id BIGINT NOT NULL UNIQUE,
    driver_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ASSIGNED',
    assignment_version INT NOT NULL DEFAULT 1,
    started_at DATETIME NULL,
    completed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_te_trip FOREIGN KEY (trip_id) REFERENCES trips(trip_id) ON DELETE CASCADE,
    CONSTRAINT fk_te_driver FOREIGN KEY (driver_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Delivery Order Results table
CREATE TABLE IF NOT EXISTS delivery_order_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_execution_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    stop_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    updated_at DATETIME NULL,
    CONSTRAINT fk_dor_execution FOREIGN KEY (trip_execution_id) REFERENCES trip_executions(id) ON DELETE CASCADE,
    CONSTRAINT fk_dor_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_dor_stop FOREIGN KEY (stop_id) REFERENCES trip_draft_stops(id),
    CONSTRAINT uq_execution_order UNIQUE (trip_execution_id, order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Trip Outcomes table
CREATE TABLE IF NOT EXISTS trip_outcomes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_execution_id BIGINT NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED',
    total_orders INT NULL,
    delivered_count INT NULL,
    failed_count INT NULL,
    partial_count INT NULL,
    submitted_at DATETIME NULL,
    validated_at DATETIME NULL,
    validated_by VARCHAR(100) NULL,
    amendment_reason TEXT NULL,
    version INT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_to_execution FOREIGN KEY (trip_execution_id) REFERENCES trip_executions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
